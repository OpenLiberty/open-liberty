/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
public class OKDServiceLogin_SSLTests extends OKDServiceLoginCommonTest {

    /**
     * pass a good service account token - expect a successful outcome
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_SSLTests_goodSSLRef() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodSSLRef");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "javax.net.ssl.SSLHandshakeException" })
    @AllowedFFDC({ "com.ibm.security.cert.IBMCertPathBuilderException", "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OKDServiceLogin_SSLTests_badSSLRef() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badSSLRef");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED, SocialMessageConstants.CWPKI0823E_HANDSHAKE_EXCEPTION);
        // Could get a CWWKO0801E message on some platforms because of the faulty SSL connection
        testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "javax.net.ssl.SSLHandshakeException" })
    @AllowedFFDC({ "com.ibm.security.cert.IBMCertPathBuilderException", "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OKDServiceLogin_SSLTests_omitSSLRef_globalSSLCfgIsNotSufficient() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED, SocialMessageConstants.CWPKI0823E_HANDSHAKE_EXCEPTION);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "javax.net.ssl.SSLHandshakeException" })
    @AllowedFFDC({ "com.ibm.security.cert.IBMCertPathBuilderException", "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OKDServiceLogin_SSLTests_missingSSLRef() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_missingSSLRef");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED, SocialMessageConstants.CWPKI0823E_HANDSHAKE_EXCEPTION);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }
}
