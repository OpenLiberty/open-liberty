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
public class OKDServiceLogin_BasicTests extends OKDServiceLogin_GenericTests {

    /**
     * pass a blank service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    public void OKDServiceLogin_BasicTests_badUserValidationApi() throws Exception {

        String statusCode = "403";
        if (stubbedTests) {
            statusCode = "404";
        }

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badUserValidationApi");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED, SocialMessageConstants.CWWKS5383E_USER_API_UNEXPECTED_RESPONSE_CODE, statusCode);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a blank service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.net.MalformedURLException" }) // using allowed instead of expected as only the first exception will result in an ffdc (other tests throw this exception)
    public void OKDServiceLogin_BasicTests_blankUserValidationApi() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_blankUserValidationApi");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a blank service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "java.net.MalformedURLException" }) // using allowed instead of expected as only the first exception will result in an ffdc (other tests throw this exception)
    public void OKDServiceLogin_BasicTests_emptyUserValidationApi() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_emptyUserValidationApi");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a blank service account token - expect a bad response from the user validation api server
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.net.UnknownHostException" })
    public void OKDServiceLogin_BasicTests_omitUserValidationApi() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_omitUserValidationApi");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5382E_USER_API_RESPONSE_CANNOT_BE_PROCESSED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - expect a good response from the user validation api server
     * use the same service account token again - expect a good result (in the case where we use a stubbed
     * server, we will see that we hit the userValidationApi the first time we use the token, but, not the second
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_reuseServiceAccountTokenWithinCacheLifetime() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_defaultCacheLifetime");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations1 = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations1 = vData.addExpectation(expectations1, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);
        if (stubbedTests) {
            // when we're using a stubbed OpenShift service, we will be able to see a message in the server side log that tells us that
            // the user validation api is called on the first request, but not on the second - when using he real OpenShift server,
            // we'll just be able to see that we got to the app that we were trying to get to.
            expectations1 = validationTools.addMessageExpectation(genericTestServer, expectations1, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Did NOT find the msg that indicates that the user validation api was called.", "Got into the stub");
        }
        List<validationData> expectations2 = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations2 = vData.addExpectation(expectations2, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);
        if (stubbedTests) {
            // when we're using a stubbed OpenShift service, we will be able to see a message in the server side log that tells us that
            // the user validation api is called on the first request, but not on the second - when using he real OpenShift server,
            // we'll just be able to see that we to to the app that we were trying to get to.
            expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found the msg that indicates that the user validation api was called - we should NOT have.", "Got into the stub");
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations1);
        genericTestServer.setMarkToEndOfLogs();
        addToAllowableTimeoutCount(1);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations2);
        genericTestServer.setMarkToEndOfLogs();
        addToAllowableTimeoutCount(1);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations2);

    }

    /**
     * pass a good service account token - expect a good response from the user validation api server
     * use the same service account token again after waiting for it to expire - expect a good result
     * (in the case where we use a stubbed server, we will see that we hit the userValidationApi the
     * on both requests - without digging into the trace, we won't be able to tell when we use a real OpenShift)
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_shortCacheLifetime() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_shortCacheLifetime");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);
        if (stubbedTests) {
            // when we're using a stubbed OpenShift service, we will be able to see a message in the server side log that tells us that
            // the user validation api is called on both requests - when using he real OpenShift server,
            // we'll just be able to see that we to to the app that we were trying to get to.
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Did NOT find the msg that indicates that the user validation api was called.", "Got into the stub");
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
        helpers.testSleep(20);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void OKDServiceLogin_BasicTests_specifyRealmName() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_specifyRealmName");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));
        updatedSocialTestSettings.setRealm("SomeOtherRealmName");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
        helpers.testSleep(20);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OKDServiceLogin_BasicTests_realmNameWithSpaces() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_realmNameWithSpaces");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));
        updatedSocialTestSettings.setRealm("Realm Name With Spaces");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
        helpers.testSleep(20);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OKDServiceLogin_BasicTests_blankRealmName() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_blankRealmName");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
        helpers.testSleep(20);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a group that is in the service account token
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_goodGroupMember() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldGoodGroup/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a group that is not in the service account token
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_badGroupMember() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldBadGroup/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setUnauthorizedErrorPageForSocialLogin(SocialMessageConstants.CWWKS9104A_AUTHORIZATION_FAILED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a user that is in the service account token
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_goodUser() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldGoodUser/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a user that is not in the service account token
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_badUser() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldBadUser/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setUnauthorizedErrorPageForSocialLogin(SocialMessageConstants.CWWKS9104A_AUTHORIZATION_FAILED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OKDServiceLogin_BasicTests_roleMap_goodGroupMember_specifyRealm() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldGoodGroupRealm/rest/helloworld_specifyRealmName");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));
        updatedSocialTestSettings.setRealm("SomeOtherRealmName");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a group that is not in the service account token
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_badGroupMember_dueToRealmMisMatch() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldGoodGroupRealm/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setUnauthorizedErrorPageForSocialLogin(SocialMessageConstants.CWWKS9104A_AUTHORIZATION_FAILED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a user that is in the service account token
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_goodUser_specifyRealm() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldGoodUserRealm/rest/helloworld_specifyRealmName");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));
        updatedSocialTestSettings.setRealm("SomeOtherRealmName");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, serviceAccountToken);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * pass a good service account token - have the application specify a user that is not in the service account token
     *
     * @throws Exception
     */
    @Test
    public void OKDServiceLogin_BasicTests_roleMap_badUser_dueToRealmMismatch() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworldGoodUserRealm/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(serviceAccountToken));

        List<validationData> expectations = setUnauthorizedErrorPageForSocialLogin(SocialMessageConstants.CWWKS9104A_AUTHORIZATION_FAILED);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }
}
