/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.okdServiceLogin.commonTests;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 **/
//@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OKDServiceLogin_GenericTests extends OKDServiceLoginCommonTest {

    /**
     * pass a good service account token - expect a successful outcome
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OKDServiceLogin_GenericTests_goodServiceAccountToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a bad service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    public void OKDServiceLogin_GenericTests_badServiceAccountToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("badServiceAccountToken"));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED, SocialMessageConstants.CWWKS5383E_USER_API_UNEXPECTED_RESPONSE_CODE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a blank service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    //@Test
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    public void OKDServiceLogin_GenericTests_blankServiceAccountToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(" "));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass an empty service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    public void OKDServiceLogin_GenericTests_emptyServiceAccountToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(""));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED); 

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass an empty Authorization header - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_GenericTests_emptyAuthorizationHeader() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue("");

       List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass an empty service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Test
    //    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    public void OKDServiceLogin_GenericTests_omitServiceAccountToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

}
