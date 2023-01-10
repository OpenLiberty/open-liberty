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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.OpenShift;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
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
 *
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OpenShift_StubbedTests_usingSocialConfig extends SocialCommonTest {

    public static Class<?> thisClass = OpenShift_StubbedTests_usingSocialConfig.class;

    String action = SocialConstants.INVOKE_SOCIAL_RESOURCE;

    @BeforeClass
    public static void setUp() throws Exception {
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);
        extraApps.add(SocialConstants.STUBBED_OPENSHIFT_SERVLET);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".OpenShift.social", "server_OpenShift_StubbedTests_usingSocialConfig.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setActionsForProvider(SocialConstants.OPENSHIFT_PROVIDER, SocialConstants.OAUTH_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, null);

        socialSettings = updateStubbedOpenShiftSettings(socialSettings);

    }

    /**
     * accessTokenRequired is set to true - test will pass a good token
     *
     * @throws Exception
     */
    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_goodTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_statusMissingInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setBadStubbedTestsExpectations(updatedSocialTestSettings, "status");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_authenticatedMissingInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    //@Test
    public void OpenShift_StubbedTests_usingSocialConfig_authenticatedFalseInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":false,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_userNameMissingInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove the UserName
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setBadStubbedTestsExpectations(updatedSocialTestSettings, "username");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" })
    @AllowedFFDC({ "java.security.PrivilegedActionException" })
    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_userNameUnknownInTokenReviewResponse_mapToUserRegistryTrue() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"unknown\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = set403ErrorPageForSocialLogin(SocialMessageConstants.CWWKS1106A_AUTHENTICATION_FAILED);
        genericTestServer.addIgnoredServerException("CWWKS5447E");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_userNameUnknownInTokenReviewResponse_mapToUserRegistryFalse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"unknown\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));
        updatedSocialTestSettings.setUserName("unknown");

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingUIDInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove the UID
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_groupsMissingInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove the UserName
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    //    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_groupsUnknownInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove the UserName
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_badUserApiToken() throws Exception {

        // send a flag to the stubbed an OpenShift tokenReviews response
        String access_token = "badServiceAccountToken";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_bad_userApiToken");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setErrorPageExpectations(action, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*403");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_blankUserApiToken() throws Exception {

        // send a flag to the stubbed an OpenShift tokenReviews response
        String access_token = "badServiceAccountToken";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_blank_userApiToken");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setErrorPageExpectations(action, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*403");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_emptyUserApiToken() throws Exception {

        // send a flag to the stubbed an OpenShift tokenReviews response
        String access_token = "badServiceAccountToken";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_empty_userApiToken");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setErrorPageExpectations(action, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*403");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will pass a bad token
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_badAccessTokenPassed() throws Exception {

        // send a flag to the stubbed an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"somebadvalueForAnAccessToken\"},\"status\":{\"user\":{},\"error\":\"[invalid bearer token, token lookup failed]\"}}";
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    //TODO add a test that returns a string instead of json (with a 200 status code)
    // groups null

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingKindInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove kind
        String access_token = "{\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingApiVersionInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove apiVersion
        String access_token = "{\"kind\":\"TokenReview\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingMetadataInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingCreationTimestampInMetadataInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":null,\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_futureCreationTimestampInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":4736099269},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        Log.info(thisClass, _testName, "currentTime: " + System.currentTimeMillis());
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingSpecInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        // remove spec
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingTokenValueInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":null},\"status\":{\"authenticated\":true,\"user\":{\"username\":\"admin\",\"uid\":\"52a9f1a4-e469-11e9-9be8-0016ac102aea\",\"groups\":[\"system:authenticated:oauth\",\"system:authenticated\"],\"extra\":{\"scopes.authorization.openshift.io\":[\"user:full\"]}}}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodStubbedTestsExpectations(updatedSocialTestSettings, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void OpenShift_StubbedTests_usingSocialConfig_missingStatusInTokenReviewResponse() throws Exception {

        // Use a "captured" response from an OpenShift tokenReviews response
        String access_token = "{\"kind\":\"TokenReview\",\"apiVersion\":\"authentication.k8s.io/v1\",\"metadata\":{\"creationTimestamp\":null},\"spec\":{\"token\":\"9Gsnl6CnQDrUlgdh1l4E-f5JJ1WC1tDOowbQLYBQ9Vs\"}}";

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setErrorPageExpectations(action, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5374E_MISSING_KEY_IN_KUBERNETES_USER_API_RESPONSE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /*************************** tooling methods ******************************/
    private List<validationData> setGoodStubbedTestsExpectations(SocialTestSettings settings, String access_token) throws Exception {
        List<validationData> expectations = setGoodHelloWorldExpectations(settings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);
        return expectations;
    }

    private List<validationData> setBadStubbedTestsExpectations(SocialTestSettings settings, String keyword) throws Exception {
        int expectedStatus = SocialConstants.UNAUTHORIZED_STATUS;
        List<validationData> expectations = setErrorPageExpectations(action, expectedStatus);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
        expectations = vData.addExpectation(expectations, action, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, action, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5374E_MISSING_KEY_IN_KUBERNETES_USER_API_RESPONSE + ".*" + keyword + ".*");

        return expectations;
    }

}
