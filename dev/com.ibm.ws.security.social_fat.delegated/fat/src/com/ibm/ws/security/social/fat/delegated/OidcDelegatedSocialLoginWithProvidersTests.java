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
package com.ibm.ws.security.social.fat.delegated;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.delegated.common.CommonDelegatedTestClass;
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
public class OidcDelegatedSocialLoginWithProvidersTests extends CommonDelegatedTestClass {

    public static Class<?> thisClass = OidcDelegatedSocialLoginWithProvidersTests.class;

    @BeforeClass
    public static void setUp() throws Exception {
        List<String> rpStartMsgs = new ArrayList<String>();
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.HELLOWORLD_SERVLET);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_CLIENT_DEFAULT_CONTEXT_ROOT);

        List<String> rpApps = new ArrayList<String>();
        rpApps.add(SocialConstants.HELLOWORLD_SERVLET);

        List<String> opStartMsgs = new ArrayList<String>();
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.JWT_DEFAULT_CONTEXT_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_DEFAULT_CONTEXT_ROOT);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        testRPServer = commonSetUp(SocialConstants.SERVER_NAME + ".delegated.rp", "rp_server_orig.xml", SocialConstants.GENERIC_SERVER, rpApps, SocialConstants.DO_NOT_USE_DERBY, rpStartMsgs);
        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".delegated.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP);

        // Each test will have to reconfigure, so don't bother restoring in between tests
        testOPServer.setRestoreServerBetweenTests(false);

        setGenericVSSpeicificProviderFlags(ProviderConfig);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void OidcDelegatedSocialLoginTests_facebook() throws Exception {

        testOPServer.reconfigServer("server_provider_facebook.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.FACEBOOK_PROVIDER, SocialConstants.OAUTH_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void OidcDelegatedSocialLoginTests_linkedin() throws Exception {

        // TODO - add https://localhost:8947/ibm/api/social-login/redirect/linkedinLogin as valid redirect URI in linkedin application
        testOPServer.reconfigServer("server_provider_linkedin.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.LINKEDIN_PROVIDER, SocialConstants.OAUTH_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void OidcDelegatedSocialLoginTests_twitter() throws Exception {

        testOPServer.reconfigServer("server_provider_twitter.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.TWITTER_PROVIDER, SocialConstants.OAUTH_OP, DOES_NOT_USE_SELECTION_PAGE);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

}
