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
import com.ibm.ws.security.social.fat.multiProvider.commonTests.CommonMultiProviderTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 *
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class MultiProvider_usingSocialConfig_withQueryParams extends CommonMultiProviderTests {

    public static Class<?> thisClass = MultiProvider_usingSocialConfig_withQueryParams.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @BeforeClass
    public static void setUp() throws Exception {
        classOverrideValidationEndpointValue = SocialConstants.USERINFO_ENDPOINT;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);
        startMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.JWT_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        List<String> opStartMsgs = new ArrayList<String>();
        opStartMsgs.add(SocialMessageConstants.CWWKS1631I_OIDC_ENDPOINT_SERVICE_ACTIVATED);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_DEFAULT_CONTEXT_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH2_DEFAULT_CONTEXT_ROOT);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(SocialConstants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.social", "server_multiProvider_usingSocialConfig.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setGenericVSSpeicificProviderFlags(GenericConfig);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the test application with query parameters included in the request
     * <LI>Select the Facebook provider on the selection page
     * <LI>Verify that when we get to the protected resource, those parameters are preserved
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then provider login page. After entering a valid id/pw, we should
     * receive access to the helloworld app and the query parameters included.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void MultiProvider_withQueryParams_selectFacebook() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateParameterTestSettings(updatedSocialTestSettings, SocialConstants.FACEBOOK_PROVIDER, SocialConstants.FACEBOOK_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = getGoodExpectationsForParameterTest(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the test application with query parameters included in the request
     * <LI>Select the GitHub provider on the selection page
     * <LI>Verify that when we get to the protected resource, those parameters are preserved
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then provider login page. After entering a valid id/pw, we should
     * receive access to the helloworld app and the query parameters included.
     * </OL>
     */
    // TODO - can enable once we find a better way of automating authentication through GitHub
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    //@Test
    public void MultiProvider_withQueryParams_selectGitHub() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateParameterTestSettings(updatedSocialTestSettings, SocialConstants.GITHUB_PROVIDER, SocialConstants.GITHUB_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = getGoodExpectationsForParameterTest(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the test application with query parameters included in the request
     * <LI>Select the Linkedin provider on the selection page
     * <LI>Verify that when we get to the protected resource, those parameters are preserved
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then provider login page. After entering a valid id/pw, we should
     * receive access to the helloworld app and the query parameters included.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void MultiProvider_withQueryParams_selectLinkedin() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateParameterTestSettings(updatedSocialTestSettings, SocialConstants.LINKEDIN_PROVIDER, SocialConstants.LINKEDIN_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = getGoodExpectationsForParameterTest(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the test application with query parameters included in the request
     * <LI>Select the Twitter provider on the selection page
     * <LI>Verify that when we get to the protected resource, those parameters are preserved
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then provider login page. After entering a valid id/pw, we should
     * receive access to the helloworld app and the query parameters included.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRumORGenericConfig.class)
    @Test
    public void MultiProvider_withQueryParams_selectTwitter() throws Exception {

        // Note: Won't run with generic config since Twitter has no generic config equivalent

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateParameterTestSettings(updatedSocialTestSettings, SocialConstants.TWITTER_PROVIDER, SocialConstants.TWITTER_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = getGoodExpectationsForParameterTest(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the test application with query parameters included in the request
     * <LI>Select the Liberty OAuth provider on the selection page
     * <LI>Verify that when we get to the protected resource, those parameters are preserved
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then provider login page. After entering a valid id/pw, we should
     * receive access to the helloworld app and the query parameters included.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_withQueryParams_selectLibertyOP_oauth() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateParameterTestSettings(updatedSocialTestSettings, SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_2_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = getGoodExpectationsForParameterTest(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the test application with query parameters included in the request
     * <LI>Select the Liberty OIDC provider on the selection page
     * <LI>Verify that when we get to the protected resource, those parameters are preserved
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then provider login page. After entering a valid id/pw, we should
     * receive access to the helloworld app and the query parameters included.
     * </OL>
     */
    @Test
    public void MultiProvider_withQueryParams_selectLibertyOP_oidc() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateParameterTestSettings(updatedSocialTestSettings, SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_LOGIN, SocialConstants.OIDC_OP);

        List<validationData> expectations = getGoodExpectationsForParameterTest(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /************************************************* Helper methods *************************************************/

    SocialTestSettings updateParameterTestSettings(SocialTestSettings socialTestSettings, String requestedProvider, String configId, String testStyle) throws Exception {
        setActionsForProvider(requestedProvider, testStyle, true);

        socialTestSettings = updateSettingsForProvider(socialTestSettings, requestedProvider, configId);

        if (!SocialConstants.TWITTER_PROVIDER.equals(requestedProvider)) {
            socialTestSettings.setProviderButton(getProviderButtonValue(configId));
        }
        socialTestSettings.setProviderButtonDisplay(socialTestSettings.getProviderButtonDisplay());

        socialTestSettings = addParametersToProtectedResourceUrl(socialTestSettings, getRequestParameters());

        return socialTestSettings;
    }

    SocialTestSettings updateSettingsForProvider(SocialTestSettings socialTestSettings, String requestedProvider, String configId) throws Exception {
        if (SocialConstants.LIBERTYOP_PROVIDER.equals(requestedProvider)) {
            socialTestSettings = updateLibertyOPSettings(socialTestSettings);

        } else if (SocialConstants.FACEBOOK_PROVIDER.equals(requestedProvider)) {
            socialTestSettings = updateFacebookSettings(socialTestSettings);

        } else if (SocialConstants.TWITTER_PROVIDER.equals(requestedProvider)) {
            socialTestSettings = updateTwitterSettings(socialTestSettings);

        } else if (SocialConstants.LINKEDIN_PROVIDER.equals(requestedProvider)) {
            socialTestSettings = updateLinkedinSettings(socialTestSettings);

        } else if (SocialConstants.GITHUB_PROVIDER.equals(requestedProvider)) {
            socialTestSettings = updateGitHubSettings(socialTestSettings);

        }
        socialTestSettings.setProvider(configId);
        return socialTestSettings;
    }

    String getProviderButtonValue(String configId) {
        return configId + "1";
    }

    List<validationData> getGoodExpectationsForParameterTest(SocialTestSettings socialTestSettings) throws Exception {
        List<validationData> expectations = setGoodSocialExpectations(socialTestSettings, doNotAddJWTTokenValidation);

        String firstAction = invoke_social_login_actions[0];
        String lastAction = invoke_social_login_actions[invoke_social_login_actions.length - 1];

        // Add expectations for the default request parameters appearing in both the initial and final request URLs
        expectations = addQueryParameterExpectations(expectations, getRequestParameters(), firstAction);
        expectations = addQueryParameterExpectations(expectations, getRequestParameters(), lastAction);

        // Login hint should be passed in a cookie by default, not in a request parameter
        String hintParamAndValue = SocialConstants.SOCIAL_LOGIN_HINT + "=";
        expectations = vData.addExpectation(expectations, lastAction, SocialConstants.RESPONSE_URL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found unexpected parameter [" + SocialConstants.SOCIAL_LOGIN_HINT + "] in protected resource request URL.", null, hintParamAndValue);

        return expectations;
    }

    String getObscuredConfigId(String configId) {
        // Should reflect the same calculation made in SocialLoginTAI.java
        return new Integer(configId.hashCode()).toString();
    }
}
