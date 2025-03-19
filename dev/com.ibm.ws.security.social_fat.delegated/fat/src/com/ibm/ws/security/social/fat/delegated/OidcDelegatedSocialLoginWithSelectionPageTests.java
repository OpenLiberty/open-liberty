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
public class OidcDelegatedSocialLoginWithSelectionPageTests extends CommonDelegatedTestClass {

    public static Class<?> thisClass = OidcDelegatedSocialLoginWithSelectionPageTests.class;

    public static final String HIDDEN_INPUT_HTML_START = "<input type=\"hidden\" name=\"";

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
        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".delegated.op", "server_provider_all.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP);

        setGenericVSSpeicificProviderFlags(ProviderConfig);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void OidcDelegatedSocialLoginWithSelectionPageTests_facebook() throws Exception {

        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.FACEBOOK_PROVIDER, SocialConstants.OAUTH_OP, USES_SELECTION_PAGE);

        List<validationData> expectations = getGoodExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void OidcDelegatedSocialLoginWithSelectionPageTests_linkedin() throws Exception {

        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.LINKEDIN_PROVIDER, SocialConstants.OAUTH_OP, USES_SELECTION_PAGE);

        List<validationData> expectations = getGoodExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @Test
    public void OidcDelegatedSocialLoginWithSelectionPageTests_twitter() throws Exception {

        SocialTestSettings updatedSocialTestSettings = getUpdatedTestSettings(SocialConstants.TWITTER_PROVIDER, SocialConstants.OAUTH_OP, USES_SELECTION_PAGE);

        List<validationData> expectations = getGoodExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /********************************************** Helper methods **********************************************/

    List<validationData> getGoodExpectations(SocialTestSettings settings) throws Exception {
        List<validationData> expectations = setGoodSocialExpectations(settings, doNotAddJWTTokenValidation);

        // Ensure that all required OAuth/OIDC parameters are included as hidden inputs in the selection page
        for (String oidcParam : getRequiredOidcHiddenParamNames()) {
            expectations = vData.addResponseExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, "Did not find OAuth parameter " + oidcParam + " in the selection page form.", HIDDEN_INPUT_HTML_START + oidcParam);
        }

        return expectations;
    }

    /**
     * Returns a list of OAuth/OIDC request parameters that must be included in the selection page in order for the delegated OIDC
     * flow to work.
     */
    List<String> getRequiredOidcHiddenParamNames() {
        List<String> requiredNames = new ArrayList<String>();
        requiredNames.add("redirect_uri");
        requiredNames.add("scope");
        requiredNames.add("state");
        requiredNames.add("response_type");
        requiredNames.add("client_id");
        return requiredNames;
    }
}
