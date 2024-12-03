/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.social.fat.commonTests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * NOTE 1: There appears to be an issue with repeating EE9 tests for some of these tests. The EE9 tests run fine when this class is run alone, but when running EE7/8 and then
 * repeating with EE9 they fail. My suspicion given the failures is that some static setting is being modified when run with the full suite, but since we have an abundance of
 * social tests, we will just skip in EE9 for now.
 */
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class Social_BasicConfigTests extends SocialCommonTest {

    public static boolean isTestingOidc = false; // subclasses may set this to true

    public static Class<?> thisClass = Social_BasicConfigTests.class;
    // list of providers that support calls using Basic (instead of post)
    // update list as we automate the other providers
    // facebook does NOT support
    // gitHub DOES support
    List<String> supportsClientSecretBasic = Arrays.asList(SocialConstants.GITHUB_PROVIDER, SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OPENSHIFT_PROVIDER);

    /***
     * Please NOTE:
     * These tests will run with the generic "social/oauth2Login" configurations, or with each of the
     * provider configs. The provider configs only support 1 (one) instance of that configuration in
     * a server.xml. This means that we need to reconfigure for each of those tests. (reconfigIfProviderSpecificConfig
     * will only do a reconfig to the server.xml specified IFFFFF we the extending class has indicated that we're
     * using a provider specific configuration.
     * Also NOTE: there are conditional rules that will allow us to only run a test if we're running
     * the generic (SocialGenericConfig) or provider specific (SocialProviderConfig) config instance.
     * I've left the "reconfig" call in the test method and specified a config (that probably doesn't exist)
     * for tests that DO NOT run in the provider specific cases. Who knows, the config attribute may be
     * added soem day.
     *
     * The goal of these tests is to test the config attributes sharing tests between the generic and
     * provider specific tests.
     * Any tests specific to the config of a specific provider, or even to the generic config could be
     * put in the appropriate extending class.
     * The extending class should initialize the correct server as well as the correct default SocialTestSettings.
     * ANY values that this code needs to validate/use/... that is specific to a provider or generic, and
     * is NOT already in SocialTestSettings will need to be set by the extending class (and most likely
     * saved in a variable assigned above)
     *
     */

    public static void addServerExceptions() throws Exception {
        if (configType.equals(GenericConfig)) {
            if (genericTestServer != null) {
                // Several configurations have missing, empty, or blank required attributes
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".+" + "tokenEndpointAuthMethod");
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKG0033W_ATTRIBUTE_VALUE_NOT_FOUND);
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL);
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS2351E_CLIENT_SECRET_MISSING_BUT_REQUIRED_BY_TOKEN_AUTH_METHOD);
                // 248970 this next one is seen occasionally on windows when a bad ssl config is being deliberately used.
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL);
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS6104W_MISSING_REQUIRED_ATTRIBUTE);
                if (isTestingOidc) {
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.SRVE8094W);
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".+" + "[id_token]");
                } else {
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".+" + "[token]");
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS6105W_REQUIRED_CONFIG_ATTRIBUTE_MISSING);
                }

                /*
                 * Here are some errors that show up on the repeat for JakartaEE9. When running EE9 repeat alone, these errors do
                 * not propagate, so I think there are some issues with the test setup on repeat.
                 */
                if (JakartaEEAction.isEE9Active()) {
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5476E_ERROR_MAKING_REQUEST);
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5447E_CAN_NOT_REDIRECT);
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5475E_URL_NULL_OR_EMPTY);
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER);
                }

            }
            if (testOPServer != null && isTestingOidc) {
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKS1751E_INVALID_ISSUER);
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            }
        }
    }

    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    @Test
    public void Social_BasicConfigTests_badClientId() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badClientId.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badClientId");

        List<validationData> expectations = null;

        if (providerDisplaysLoginOnError) {
            expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
            // TODO - do we want to do further checking or process login page?  The issue that this test surfaces is something that the external provider responds to, not us
        } else {
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get a message about an unknown clientId", null, SocialMessageConstants.CWOAU0061E_BAD_CLIENTID);
            } else {
                if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
                    int expectedStatus = SocialConstants.UNAUTHORIZED_STATUS;
                    expectations = setErrorPageExpectations(SocialConstants.INVOKE_SOCIAL_RESOURCE, expectedStatus);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the endpoint request failed.", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE + ".*" + SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
                } else {
                    if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                        expectations = vData.addResponseStatusExpectation(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.BAD_REQUEST_STATUS);
                        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did not find message in the log saying the endpoint request failed.", null, "The client is not authorized to request a token using this method.");
                    } else {
                        expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                        if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Provider should have returned a message about an invalid client id", null, "The passed in client_id is invalid .*9991117537801291");
                        } else {
                            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Provider should have returned a message about a bad client app", null, "Invalid App ID");
                        }
                    }
                }
            }
        }
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void Social_BasicConfigTests_blankClientId() throws Exception {

        blankOrEmptyClientId("_blankClientId.xml", "/helloworld/rest/helloworld_blankClientId");

    }

    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    @Test
    public void Social_BasicConfigTests_emptyClientId() throws Exception {

        blankOrEmptyClientId("_emptyClientId.xml", "/helloworld/rest/helloworld_emptyClientId");

    }

    private void blankOrEmptyClientId(String specificConfig, String appToCall) throws Exception {
        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = null;

        if (providerDisplaysLoginOnError && !isTestingOidc) {
            expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
            // TODO - do we want to do further checking or process login page?  The issue that this test surfaces is something that the external provider responds to, not us
        } else {
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (!isTestingOidc) {
                    expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                    expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get a message about a missing clientId", null, SocialMessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing clientId.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_id" + "\\]");
                } else {
                    expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.BAD_REQUEST_STATUS);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that clientid is invalid", SocialMessageConstants.CWWKS5390E_BAD_CONFIG_PARAM);
                }
            } else if (provider.equals(SocialConstants.FACEBOOK_PROVIDER)) {
                expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Got to the Login page and should NOT have", null, updatedSocialTestSettings.getLoginPage());
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did not get a message about the missing app_id parameter", null, "The parameter app_id is required");
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing clientId.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_id" + "\\]");
                if (configType.equals(ProviderConfig)) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing/empty clientId config attribute.", SocialMessageConstants.CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL + ".+\\[" + "clientId" + "\\]");
                }
            } else if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
                // TODO chc - get rid of this
                expectations = setErrorPageExpectations(SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log for a missing config attribute.", SocialMessageConstants.CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL + ".*\\[" + SocialConstants.TWITTER_CONSUMER_KEY + "\\]");
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the endpoint request failed.", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE + ".*" + SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the request might fail because the consumerKey attribute was empty.", SocialMessageConstants.CWWKS5485W_TWITTER_MISSING_REQ_ATTR + ".*\\[" + "consumerKey" + "\\]");
            } else if (provider.equalsIgnoreCase(SocialConstants.LINKEDIN_PROVIDER)) {
                expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Got to the Login page and should NOT have", null, updatedSocialTestSettings.getLoginPage());
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get a message about the missing client_id parameter", null, "You need to pass the .*client_id.* parameter");
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing clientId.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_id" + "\\]");
                if (configType.equals(ProviderConfig)) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing/empty clientId config attribute.", SocialMessageConstants.CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL + ".+\\[" + "clientId" + "\\]");
                }
            } else if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                expectations = vData.addResponseStatusExpectation(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.BAD_REQUEST_STATUS);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Got to the Login page and should NOT have", null, updatedSocialTestSettings.getLoginPage());
                //                if (specificConfig.contains("empty")) {
                //                    expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get a message about a missing client", null, "Client not found");
                //                } else {
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get a message about the server error (missing client_id parameter)", null, "server_error.+The authorization server encountered an unexpected condition that prevented it from fulfilling the request.");
                //                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing/empty clientId config attribute.", SocialMessageConstants.CWWKS5479E_CONFIG_REQUIRED_ATTRIBUTE_NULL + ".+\\[" + "clientId" + "\\]");
                //                }
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing clientId.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_id" + "\\]");
                //                }
            } else {
                expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, invoke_social_login_actions);
                expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INTERNAL_SERVER_ERROR_STATUS);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Provider should have returned a status code of 500", null, SocialConstants.INTERNAL_SERVER_ERROR);
            }

        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_badClientSecret() throws Exception {

        blankEmptyOrBadClientSecret("_badClientSecret.xml", "/helloworld/rest/helloworld_badClientSecret", false);

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_blankClientSecret() throws Exception {

        blankEmptyOrBadClientSecret("_blankClientSecret.xml", "/helloworld/rest/helloworld_blankClientSecret", false);
    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_emptyClientSecret() throws Exception {

        blankEmptyOrBadClientSecret("_emptyClientSecret.xml", "/helloworld/rest/helloworld_emptyClientSecret", true);
    }

    private void blankEmptyOrBadClientSecret(String specificConfig, String appToCall, boolean failsOidcConfigCheck) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        String[] actions = null;
        String finalAction = null;
        if (failsOidcConfigCheck && isTestingOidc) {
            actions = new String[] { SocialConstants.INVOKE_SOCIAL_RESOURCE };
            finalAction = SocialConstants.INVOKE_SOCIAL_RESOURCE;
        } else {
            actions = invoke_social_login_actions;
            finalAction = invoke_social_login_actions[invoke_social_login_actions.length - 1];
        }
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            // Twitter flow will only get so far as invoking the protected resource
            actions = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
            finalAction = SocialConstants.INVOKE_SOCIAL_RESOURCE;
        }

        List<validationData> expectations = null;
        if (failsOidcConfigCheck && isTestingOidc) {
            expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.FORBIDDEN_STATUS);
        } else {
            expectations = vData.addSuccessStatusCodesForActions(finalAction, actions);
        }
        if (isTestingOidc && !failsOidcConfigCheck) {
            expectations = vData.addResponseStatusExpectation(expectations, finalAction, SocialConstants.BAD_REQUEST_STATUS);
        } else {
            if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                expectations = vData.addResponseStatusExpectation(expectations, finalAction, SocialConstants.UNAUTHORIZED_STATUS);
            }
            //?? expectations = vData.addResponseStatusExpectation(expectations, finalAction, SocialConstants.FORBIDDEN_STATUS);
        }

        if (provider.equals(SocialConstants.FACEBOOK_PROVIDER) || provider.equals(SocialConstants.LIBERTYOP_PROVIDER) || provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (isTestingOidc) {
                    if (failsOidcConfigCheck) {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unable to contact provider exception", SocialMessageConstants.CWWKS5390E_BAD_CONFIG_PARAM);
                    } else {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unable to contact provider exception", SocialMessageConstants.CWWKS1708E_UNABLE_TO_CONTACT_PROVIDER);
                    }
                } else {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", SocialMessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Did not find message in OP log saying the client could not be verified.", SocialMessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
                }
            } else if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                if (_testName.contains("emptyClientSecret")) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an unauthorized exception", "A required parameter.*client_secret.*is missing");
                } else {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", "Client authentication failed");
                }
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", "Error validating client secret");
            }

            if (!isTestingOidc) {
                if (_testName.contains("emptyClientSecret")) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing client secret.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_secret" + "\\]");
                }
                // search for specific error content for msg id CWWKS5478E
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error attempting to access the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received general social login error", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
            }
        } else {
            if (provider.equals(SocialConstants.TWITTER_PROVIDER) || provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                expectations = vData.addExpectation(expectations, finalAction, genericTestServer, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
                expectations = vData.addExpectation(expectations, finalAction, genericTestServer, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "HTML error page did not contain message indicating that the user cannot be authenticated.", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
                if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the endpoint request failed.", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE + ".*" + SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
                    if (_testName.contains("emptyClientSecret")) {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the request might fail because the consumerSecret attribute was empty.", SocialMessageConstants.CWWKS5485W_TWITTER_MISSING_REQ_ATTR + ".*\\[" + "consumerSecret" + "\\]");
                    }
                } else {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error attempting to access the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received general social login error", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
                    if (_testName.contains("emptyClientSecret")) {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing client secret.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_secret" + "\\]");
                    }
                }
            } else {

                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error due to missing claims", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error getting user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
                if (_testName.contains("emptyClientSecret")) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not get a message in social server logs about a missing client secret.", SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER + ".+\\[" + "client_secret" + "\\]");
                }
            }
        }
        if (!isTestingOidc) {
            expectations = vData.addExpectation(expectations, finalAction, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        }

        genericSocial(_testName, webClient, actions, updatedSocialTestSettings, expectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicConfigTests_XOR_Secret() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_xorSecret.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_xorSecret");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_bad_XOR_Secret() throws Exception {

        blankEmptyOrBadClientSecret("_badXorSecret.xml", "/helloworld/rest/helloworld_bad_xorSecret", false);

    }

    // specific provider configs do NOT support enabled
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_enabledTrue() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_enabledTrue");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // specific provider configs do NOT support enabled
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    // TODO enable test when "enabled" config attribute is enabled
    //    @Test
    public void Social_BasicConfigTests_enabledFalse() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_enabledFalse");

        //TODO update for what we'll actually expect
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", "Error validating client secret");
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    // generic provider uses authFilterRef for all tests - don't waste runtime testing again
    // specific provider configs only support one instance of each provider, so, we can't use
    // a config with multiple instances and authFilters to differentiate, so, we need a test
    // to make sure that we can limit a provider to just a specific instance of an app...
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfGenericConfig.class)
    @Test
    public void Social_BasicConfigTests_goodAuthFilterRef() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodAuthFilterRef.xml", null);

        WebClient webClient = getAndSaveWebClient();

        // use the default app - which, won't match the configured filter.  Show that we get a failure - just to make sure we're not accidentally matching everything
        // TODO - currently the runtime is not returning the 401 properly - we should get an error page, but are not
        // using a browser, we get a blank page which is not correct waiting on defect 240082 to be fixed
        try {
            genericSocial(_testName, webClient, invoke_social_login_actions, socialSettings, null);
            assertTrue("Did NOT receive an exception when we tried to use an app that did NOT match the filter - should have tried basic auth which won't work in this case", false);
        } catch (Exception e) {
            assertTrue("Received Exception", true);
        }

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodAuthFilterRef");
        // create new expectations for a flow using a matching application
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // generic social config (oauth2Login) has a config with an authFilterRef that doesn't exist, but it has a default config
    // that has NO filter - this means that we'll have a successful outcome
    // Specific social config ONLY has a config with an authFilterRef that does NOT exist
    // Because the filter doesn't exist, the option is completely omitted and therefore, the config
    // will be used for ALL requests (as there are no other filters)
    // Server startup will log CWWKG0033W
    @Test
    public void Social_BasicConfigTests_badAuthFilterRef() throws Exception {

        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add(SocialMessageConstants.CWWKG0033W_ATTRIBUTE_VALUE_NOT_FOUND);
        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badAuthFilterRef.xml", extraMsgs);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badAuthFilterRef");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        if (configType.equals(ProviderConfig)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_PROTECTED_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that a config reference couldn't be found.", SocialMessageConstants.CWWKG0033W_ATTRIBUTE_VALUE_NOT_FOUND);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     */

    /**
     * The global SSL config uses the trust store that has the provider, so, adding it to the provider config does little.
     * We just want to show that it doesn't break anything...
     *
     * @throws Exception
     */
    // NOTE: bad global provider trust with good provider specific trust is tested in test class: Social_BasicConfigTests_omittedGlobalTrust
    // Reconfiguring the global trust can be problematic!
    @Test
    public void Social_BasicConfigTests_goodSSLRef_akaGoodTrust() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodTrust.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodTrust");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke Helloworld
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the login page from. After entering a valie id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ExpectedFFDC({ "javax.net.ssl.SSLHandshakeException" })
    @AllowedFFDC({ "com.ibm.security.cert.IBMCertPathBuilderException", "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_badSSLRef_akaBadSSLTrust() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badTrust.xml", null);

        WebClient webClient = getAndSaveWebClient();

        String lastStep = perform_social_login;
        String[] steps = invoke_social_login_actions;
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            lastStep = SocialConstants.INVOKE_SOCIAL_RESOURCE;
            steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
        }

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badTrust");

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(lastStep, steps);
        expectations = vData.addResponseStatusExpectation(expectations, lastStep, SocialConstants.UNAUTHORIZED_STATUS);

        if (!provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        }
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, lastStep, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating there was a Handshake Exception", SocialMessageConstants.CWPKI0823E_HANDSHAKE_EXCEPTION + ".*SSL HANDSHAKE FAILURE.*" + provider);

        if (provider.equals(SocialConstants.GITHUB_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, lastStep, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
        } else if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the endpoint request failed.", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE + ".*" + SocialMessageConstants.CWWKS5476E_ERROR_MAKING_REQUEST);
        } else if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the endpoint request failed.", SocialMessageConstants.CWWKS5476E_ERROR_MAKING_REQUEST + ".*PKIX");
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
        }

        // Could get a CWWKO0801E message on some platforms because of the MD5 cert in the trust store
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL);
        testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL);

        genericSocial(_testName, webClient, steps, updatedSocialTestSettings, expectations);
    }

    @Test
    public void Social_BasicConfigTests_blankSSLRef() throws Exception {

        blankOrEmptyTrust("_blankTrust.xml", "/helloworld/rest/helloworld_blankTrust");

    }

    /**
     * Empty trust to behave as if sslRef wasn't specified - since the server wide
     * trust is good, the request should succeed
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_emptySSLRef() throws Exception {

        blankOrEmptyTrust("_emptyTrust.xml", "/helloworld/rest/helloworld_emptyTrust");

    }

    private void blankOrEmptyTrust(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        //        List<validationData> expectations = vData.addSuccessStatusCodesForActions(PERFORM_SOCIAL_LOGIN, INVOKE_SOCIAL_LOGIN_ACTIONS);
        //        expectations = vData.addResponseStatusExpectation(expectations, PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        //
        //        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get to the Login page", null, updatedSocialTestSettings.getLoginPage());
        //        expectations = vData.addExpectation(expectations, PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        //        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating there was a Handshake Exception", SocialMessageConstants.CWPKI0022E_HANDSHAKE_EXCEPTION + ".*SSL HANDSHAKE FAILURE.*" + PROVIDER);
        //
        //        genericSocial(_testName, webClient, INVOKE_SOCIAL_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);
    }

    // generic provider uses authorizationEndpoint for all tests - don't waste runtime testing again
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfGenericConfig.class)
    @Test
    public void Social_BasicConfigTests_goodAuthorizationEndpoint() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodAuthEndpoint.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodAuthEndpoint");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicConfigTests_badAuthorizationEndpoint() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badAuthEndpoint.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badAuthEndpoint");

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.NOT_FOUND_STATUS);

        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.NOT_FOUND_MSG, null, SocialConstants.NOT_FOUND_MSG);
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the endpoint request wasn't recognized.", SocialMessageConstants.CWOAU0039W_OAUTH20_FILTER_REQUEST_NULL);
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_reallyBadAuthorizationEndpoint() throws Exception {

        blankEmptyOrReallyBadAuthorizationEndpoint("_reallyBadAuthEndpoint.xml", "/helloworld/rest/helloworld_reallyBadAuthEndpoint");

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_blankAuthorizationEndpoint() throws Exception {

        blankEmptyOrReallyBadAuthorizationEndpoint("_blankAuthEndpoint.xml", "/helloworld/rest/helloworld_blankAuthEndpoint");

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_emptyAuthorizationEndpoint() throws Exception {

        blankEmptyOrReallyBadAuthorizationEndpoint("_emptyAuthEndpoint.xml", "/helloworld/rest/helloworld_emptyAuthEndpoint");

    }

    private void blankEmptyOrReallyBadAuthorizationEndpoint(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        int badStatus = SocialConstants.FORBIDDEN_STATUS;
        String badMessage = SocialConstants.FORBIDDEN;
        String badString = SocialConstants.HTTP_ERROR_FORBIDDEN;
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            badStatus = SocialConstants.UNAUTHORIZED_STATUS;
            badMessage = SocialConstants.UNAUTHORIZED_MESSAGE;
            badString = "HTTP Error 401";
        }

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, invoke_social_login_actions);
        if (isTestingOidc) {
            expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.FORBIDDEN_STATUS);

        } else {
            expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, badStatus);
        }

        if (isTestingOidc) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the auth endpoint was bad", SocialMessageConstants.CWWKS5390E_BAD_CONFIG_PARAM);
        } else {
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + badMessage, null, badMessage);
            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Was expecting the response to contain: " + badString, null, badString);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the auth endpoint was blank/empty", SocialMessageConstants.CWWKS5475E_URL_NULL_OR_EMPTY);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we couldn not redirect", SocialMessageConstants.CWWKS5447E_CAN_NOT_REDIRECT);
        }
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    // generic provider uses authorizationEndpoint for all tests - don't waste runtime testing again
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfGenericConfig.class)
    @Test
    public void Social_BasicConfigTests_goodTokenEndpoint() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodTokenEndpoint.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodTokenEndpoint");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "java.net.UnknownHostException" })
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_badTokenEndpoint() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badTokenEndpoint.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badTokenEndpoint");

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating the hostname was not good", "java.net.UnknownHostException");
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log saying the endpoint request failed.", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE + ".*" + SocialMessageConstants.CWWKS5476E_ERROR_MAKING_REQUEST);
        } else {
            if (isTestingOidc) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS1708E_UNABLE_TO_CONTACT_PROVIDER);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
            }
        }
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "java.net.MalformedURLException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_blankTokenEndpoint() throws Exception {

        blankOrEmptyTokenEndpoint("_blankTokenEndpoint.xml", "/helloworld/rest/helloworld_blankTokenEndpoint");

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "java.net.MalformedURLException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_emptyTokenEndpoint() throws Exception {

        blankOrEmptyTokenEndpoint("_emptyTokenEndpoint.xml", "/helloworld/rest/helloworld_emptyTokenEndpoint");

    }

    /**
     * Helper method for Social_BasicConfigTests_emptyTokenEndpoint and Social_BasicConfigTests_emptyTokenEndpoint
     * Their test logic is identical, just need to use different configs and apps
     *
     * @param specificConfig
     *            - the config to use if we're running with specific provider configs
     * @param appToCall
     *            - the app to invoke to test the appropriate config
     * @throws Exception
     */
    private void blankOrEmptyTokenEndpoint(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5483E_BADTOKEN_INFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the Token Endpoint was not set", SocialMessageConstants.CWWKS5475E_URL_NULL_OR_EMPTY);
        } else {
            if (isTestingOidc) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS1708E_UNABLE_TO_CONTACT_PROVIDER);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the Token Endpoint was not set", SocialMessageConstants.CWWKS5462E_NULL_TOKEN_ENDPOINT);
            }
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // have to use AllowedFFDC because even when the rule says don't run the test, the framework expects the ffdc
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfTwitter.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_tokenEndpointAuthMethod_clientSecretBasic() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tEAM_clientSecretBasic.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_tEAM_clientSecretBasic");

        List<validationData> expectations = null;
        // if the provider accepts basic, expect success, otherwise, expect a failure
        if (supportsClientSecretBasic.contains(provider)) {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        } else {
            expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
            expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
            if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Was expecting the message log to contain: Missing client_id parameter", "A required parameter.*client_id.*is missing");
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Was expecting the message log to contain: Missing client_id parameter", "Missing client_id parameter");
            }
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that there was a bad response from the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfTwitter.class)
    @Test
    public void Social_BasicConfigTests_tokenEndpointAuthMethod_clientSecretPost() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tEAM_clientSecretPost.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_tEAM_clientSecretPost");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    // OSGI now catches the bad value and omits it from the config - therefore, at runtime, it's like it was never set.
    //    @Test
    public void Social_BasicConfigTests_tokenEndpointAuthMethod_Bad() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tEAM_clientSecretBad.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_tEAM_clientSecretBad");

        List<validationData> expectations = null;
        expectations = vData.addSuccessStatusCodesForActions(perform_social_login, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that there was a bad response from the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the feature couldn't obtain info from the endpoint", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // generic provider uses userApi for all tests - don't waste runtime testing again
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    @Test
    public void Social_BasicConfigTests_goodUserApi() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodUserApi.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodUserApi");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // since we don't run the test for oidcLogin types, we won't get the exception, so make it allowed, not expected
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "org.jose4j.lang.JoseException", "org.jose4j.json.internal.json_simple.parser.ParseException" })
    @Test
    public void Social_BasicConfigTests_badUserApi() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badUserApi.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badUserApi");

        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the info from the user api", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the info from the user api", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the info from the user api", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            if (!(provider.equals(SocialConstants.LIBERTYOP_PROVIDER) || provider.equals(SocialConstants.LINKEDIN_PROVIDER) || provider.equals(SocialConstants.OPENSHIFT_PROVIDER))) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the info from the user api", SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
            }
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't authenticate because of missing claims", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_blankUserApi() throws Exception {

        blankOrEmptyUserApi("_blankUserApi.xml", "/helloworld/rest/helloworld_blankUserApi");

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_emptyUserApi() throws Exception {

        blankOrEmptyUserApi("_emptyUserApi.xml", "/helloworld/rest/helloworld_emptyUserApi");
    }

    public void blankOrEmptyUserApi(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = null;
        expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that userApi is not specified", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that userApi was null or empty", SocialMessageConstants.CWWKS5484E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that userApi was null or empty", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that userApi is not specified", SocialMessageConstants.CWWKS5460W_NO_USERAPI_CONFIG);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * userApiType is set to basic - when the provider is NOT OpenShift, we should have
     * success (get to the test app).
     * When the provider is OpenShift, the runtime should fail to process the output from
     * the review endpoint (set in userApi)
     * The specific
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_basicUserApiType() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_basicUserApiType");

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsUserinfo()) {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        } else {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);
            }
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * userApiType is set to basic - when the provider is NOT OpenShift, we should have
     * success (get to the test app).
     * When the provider is OpenShift, the runtime should fail to process the output from
     * the review endpoint (set in userApi)
     * The specific
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "java.io.IOException" })
    @Test
    public void Social_BasicConfigTests_introspectUserApiType() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_introspectUserApiType");

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        } else {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5387E_INTROSPECT_ERROR_GETTING_INFO);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            }
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * useApitType = kube - this should only be valid with a kubernetes type provider - we will try to
     * parse the response/access_token incorrectly for other types.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "java.io.IOException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_kubeUserApiType() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_kubeUserApiType");

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        } else {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
            if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (validationEndpointIsUserinfo()) {
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
                } else {
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING);
                }
            }
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use a valid token as the userApiToken, for all other providers using the generic OAuth config, any value
     * should be considered good as we won't be using it.
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_goodUserApiToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_good_userApiToken");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_badUserApiToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_bad_userApiToken");

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*401");
        } else {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_UserApiToken_withMissingPermissions() throws Exception {

        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER) && genericTestServer.getBootstrapProperty("service.account.token.missing.permissions") != null) {
            WebClient webClient = getAndSaveWebClient();

            SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_missingPermissions_userApiToken");

            List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*403");

            genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
        }
    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_blankUserApiToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_blank_userApiToken");

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*403");
        } else {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * For OpenShift, use an invalid token as the userApiToken, for all other providers using the generic OAuth config, any or no
     * value will be ignored
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_emptyUserApiToken() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_empty_userApiToken");

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "HTML error page did not contain the expected title. We likely reached a page that we shouldn't have gotten to.", null, SocialConstants.HTTP_ERROR_MESSAGE);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Should have received an error processing the response", SocialMessageConstants.CWWKS5373E_OPENSHIFT_UNEXPECTED_RESPONSE_CODE + ".*403");
        } else {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // Omitting test with NO RealmName since 99% of the tests run that way
    // Skipping test when running with provider specific config as they do not support the realmName config attribute
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_realmName() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_realmName.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_realmName");
        updatedSocialTestSettings.setRealm("notSetRealm");
        if (provider.equals(SocialConstants.FACEBOOK_PROVIDER)) {
            updatedSocialTestSettings.setRealm("myFaceBookRealm");
        }
        if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
            updatedSocialTestSettings.setRealm("myLinkedinRealm");
        }
        if (provider.equals(SocialConstants.GITHUB_PROVIDER)) {
            updatedSocialTestSettings.setRealm("myGitHubRealm");
        }
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            updatedSocialTestSettings.setRealm("myLibertyOPRealm");
        }
        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            updatedSocialTestSettings.setRealm("myOpenShiftRealm");
        }

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // Skipping test when running with provider specific config as they do not support the realmName config attribute
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_blankRealmName() throws Exception {

        blankOrEmptyRealmName("_blankRealmName.xml", "/helloworld/rest/helloworld_blank_RealmName");

    }

    // Skipping test when running with provider specific config as they do not support the realmName config attribute
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_emptyRealmName() throws Exception {

        blankOrEmptyRealmName("_emptyRealmName.xml", "/helloworld/rest/helloworld_empty_RealmName");

    }

    /**
     * Helper method for Social_BasicConfigTests_emptyTokenEndpoint and Social_BasicConfigTests_emptyTokenEndpoint
     * Their test logic is identical, just need to use different configs and apps
     *
     * @param specificConfig
     *            - the config to use if we're running with specific provider configs
     * @param appToCall
     *            - the app to invoke to test the appropriate config
     * @throws Exception
     */
    private void blankOrEmptyRealmName(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);
        // don't override default realm name - an empty string should result in the default realm being used

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Test
    public void Social_BasicConfigTests_mapToUserRegistryFalse() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_mapToUserRegistryFalse.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryFalse");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" })
    @Test
    public void Social_BasicConfigTests_mapToUserRegistryTrue_userNotInRegistry() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_mapToUserRegistryTrue_userNotInRegistry.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryTrue");

        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the authentication failed", SocialMessageConstants.CWWKS1106A_AUTHENTICATION_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating the user id was not good", updatedSocialTestSettings.getUserName());
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicConfigTests_mapToUserRegistryTrue_userInRegistry() throws Exception {

        // if we're using provider specific, reconfig to <provider>__mapToUserRegistryTrue_userInRegistry.xml, otherwise, use the
        // generic server_facebook_basicConfigTests_usingSocialConfig_FacebookRegistry
        if (!reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_mapToUserRegistryTrue_userInRegistry.xml", null)) {
            // do reconfig for social generic config
            if (provider.equals(SocialConstants.FACEBOOK_PROVIDER)) {
                genericTestServer.reconfigServer("server_facebook_basicConfigTests_usingSocialConfig_FacebookRegistry.xml", _testName, true, null);
            }
            if (provider.equals(SocialConstants.GITHUB_PROVIDER)) {
                genericTestServer.reconfigServer("server_GitHub_basicConfigTests_usingSocialConfig_GitHubRegistry.xml", _testName, true, null);
            }
            if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                genericTestServer.reconfigServer("server_linkedin_basicConfigTests_usingSocialConfig_LinkedinRegistry.xml", _testName, true, null);
            }
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (oidcLoginStyle) {
                    genericTestServer.reconfigServer("server_LibertyOP_basicConfigTests_oidc_usingSocialConfig_LibertyOPRegistry.xml", _testName, true, null);
                } else {
                    genericTestServer.reconfigServer("server_LibertyOP_basicConfigTests_oauth_usingSocialConfig_LibertyOPRegistry.xml", _testName, true, null);
                }
            }
            if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                genericTestServer.reconfigServer("server_OpenShift_basicConfigTests_usingSocialConfig_OpenshiftRegistry.xml", _testName, true, null);
            }

        }

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryTrue");
        updatedSocialTestSettings.setRealm("BasicRealm");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicConfigTests_goodJwt_builder() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodJwt_builder.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_good_jwt_builder");
        updatedSocialTestSettings.setIssuer(updateDefaultIssuer(updatedSocialTestSettings, "goodJwtBuilder"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, addJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Test that we can use the config with an empty builder. We will not see and issuedJwt in the token since the config
     * really doesn't specify a jwt builder to use
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_blankJwt_builder() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_blankJwt_builder.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_blank_jwt_builder");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Test that we can use the config with an empty builder. We will not see and issuedJwt in the token since the config
     * really doesn't specify a jwt builder to use
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_emptyJwt_builder() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_emptyJwt_builder.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_empty_jwt_builder");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void Social_BasicConfigTests_jwt_builder_HS256() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_jwt_builder_HS256.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_jwt_builder_HS256");
        updatedSocialTestSettings.setIssuer(updateDefaultIssuer(updatedSocialTestSettings, "goodJwtBuilder_HS256"));

        // jwtBuilder will create a token using HS256 and a unique secret
        updatedSocialTestSettings.setSignatureAlg(SocialConstants.SIGALG_HS256);
        updatedSocialTestSettings.setClientSecret("someKeyValue");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, addJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    public void Social_BasicConfigTests_goodJwksUri() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodJwksUri.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodJwksUri");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException", "javax.net.ssl.SSLException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    @Test
    public void Social_BasicConfigTests_badJwksUri() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badJwksUri.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badJwksUri");

        //        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);

        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the JWK was not returned", SocialMessageConstants.CWWKS6049E_JWK_NOT_RETURNED);
        if (!isTestingOidc) {
            //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the signing key was not found", SocialMessageConstants.CWWKS6029E_NO_SIGNING_KEY);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the response", SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException", "com.ibm.websphere.security.jwt.InvalidTokenException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    @Test
    public void Social_BasicConfigTests_blankJwksUri() throws Exception {

        blankOrEmptyJwksUri("_blankJwksUri.xml", "/helloworld/rest/helloworld_blankJwksUri");

    }

    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtSignatureException", "com.ibm.websphere.security.jwt.InvalidTokenException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    @Test
    public void Social_BasicConfigTests_emptyJwksUri() throws Exception {

        blankOrEmptyJwksUri("_emptyJwksUri.xml", "/helloworld/rest/helloworld_emptyJwksUri");

    }

    public void blankOrEmptyJwksUri(String specificConfig, String appToCall) throws Exception {
        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        //        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        if (isTestingOidc) {
            //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the jwskuri was bad", SocialMessageConstants.CWWKS5500E_BAD_CONFIG_PARAM);
            // these next messages are unhelpful since the problem is a bad url but the test needs to allow them through.
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the jwskuri was bad", "CWWKS1756E");
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the jwskuri was bad", "CWWKS1706E");
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the signature was invalid", SocialMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the response", SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        }
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    public void Social_BasicConfigTests_jwksUri_jwkDisabledInOP() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_jwksUri_jwkDisabledInOP.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_jwksUri_jwkDisabledInOP");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Mode(TestMode.LITE)
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    @Test
    public void Social_BasicConfigTests_badIssuer() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badIssuer.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badIssuer");

        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        if (isTestingOidc) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the issuer is invalid", SocialMessageConstants.CWWKS1751E_INVALID_ISSUER);
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the issuer is invalid", SocialMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the response", SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        }
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // if provider is IBM OP, we'll construct an issuer value from the token endpoint, so, the requests should succeed
    // we're still discussing the behavior for Google and any other OIDC types
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    @Test
    public void Social_BasicConfigTests_blankIssuer() throws Exception {

        blankOrEmptyIssuer("_blankIssuer.xml", "/helloworld/rest/helloworld_blankIssuer");

    }

    // if provider is IBM OP, we'll construct an issuer value from the token endpoint, so, the requests should succeed
    // we're still discussing the behavior for Google and any other OIDC types
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    @Test
    public void Social_BasicConfigTests_emptyIssuer() throws Exception {

        blankOrEmptyIssuer("_emptyIssuer.xml", "/helloworld/rest/helloworld_emptyIssuer");

    }

    public void blankOrEmptyIssuer(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = null;
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        } else {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the issuer is invalid", SocialMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the response", SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Test
    public void Social_BasicConfigTests_isClientSideRedirectSupported_true() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_isClientSideRedirectSupported_true.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_isClientSideRedirectSupported_true");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @Test
    public void Social_BasicConfigTests_isClientSideRedirectSupported_false() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_isClientSideRedirectSupported_false.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_isClientSideRedirectSupported_false");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_badScope() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badScope.xml", null);

        WebClient webClient = getAndSaveWebClient();
        String[] steps = invoke_social_login_actions;

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badScope");
        Log.info(thisClass, "**debug**", " oidcLoginStyle = " + oidcLoginStyle);
        List<validationData> expectations = null;
        if (oidcLoginStyle) {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the scope is invalid", SocialMessageConstants.CWWKS5460W_NO_USERAPI_CONFIG);
            //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS1712E_ID_TOKEN_MISSING);
        } else {
            if (provider.equals(SocialConstants.FACEBOOK_PROVIDER)) {
                // facebook issues the 500 status and returns to the test client directly
                steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
                expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, steps);
                expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INTERNAL_SERVER_ERROR_STATUS);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Provider should have returned a status code of 500", null, SocialConstants.INTERNAL_SERVER_ERROR);
            } else {
                if (provider.equals(SocialConstants.GITHUB_PROVIDER)) {
                    expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
                    expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                } else {
                    if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                        steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
                        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, steps);
                        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Title did NOT indicate an issue", null, SocialConstants.HTTP_ERROR_MESSAGE);
                        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authenticate the user", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);
                    } else {
                        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
                            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
                            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Title did NOT indicate an issue", null, SocialConstants.HTTP_ERROR_MESSAGE);
                            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT indicate that we can not authenticate the user", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
                            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);
                        } else {
                            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
                        }
                    }
                }
            }
        }
        genericSocial(_testName, webClient, steps, updatedSocialTestSettings, expectations);

    }

    // twitter doesn't support scope
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfTwitter.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_blankScope() throws Exception {

        blankOrEmptyLimitedScope("_blankScope.xml", "/helloworld/rest/helloworld_blankScope");

    }

    // twitter doesn't support scope
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfTwitter.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_emptyScope() throws Exception {

        blankOrEmptyLimitedScope("_emptyScope.xml", "/helloworld/rest/helloworld_emptyScope");

    }

    public void blankOrEmptyLimitedScope(String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = null;

        if (provider.equals(SocialConstants.FACEBOOK_PROVIDER) || provider.equals(SocialConstants.LINKEDIN_PROVIDER) || provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            // Facebook allows no scope to be specified
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        } else {
            if (!isTestingOidc) {
                expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
                expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
            } else {
                // oidc will 401 before it makes it to the login page because the invalid config is detected sooner.
                expectations = vData.addResponseStatusExpectation(expectations, perform_social_login, SocialConstants.UNAUTHORIZED_STATUS);

            }

            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {

                if (isTestingOidc) {
                    if (_testName.contains("Social_BasicConfigTests_badScope_limitedOPScope")) {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain CWWKS1713E message indicating that the scope is invalid", SocialMessageConstants.CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);
                    } else {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain CWWKS1713E message indicating that the scope is invalid", SocialMessageConstants.CWWKS1713E_SCOPE_INVALID);
                    }
                } else {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the scope is invalid", SocialMessageConstants.CWOAU0064E_SCOPE_MISMATCH);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the request contained an error.", SocialMessageConstants.CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR + ".*" + "invalid_scope");
                }
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            }
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNotLibertyOP.class) // skip test if NOT using the IBM OP
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    public void Social_BasicConfigTests_badScope_limitedOPScope() throws Exception {

        blankOrEmptyLimitedScope("_limitedOPScope_badScope.xml", "/helloworld/rest/helloworld_limitedOPScope_badScope");

    }

    /***
     * Tests behavior when the userNameAttribute specifies a good/valid value - meaning a claim can be found with the name/value
     * specified
     * Expect the value of the key specified - in this case, the username to be set in "Principal ID"
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_goodUserNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_goodUserNameAttribute.xml", "/helloworld/rest/helloworld_goodUserNameAttribute", true, "Principal ID: ", socialSettings.getUserName(), true);

    }

    /***
     * Tests behavior when the userNameAttribute specifies a bad value - meaning a claim that can not be found with the name/value
     * Expect the request to fail - we'll receive an 401 status code and error messages indicating that the claim did NOT exist
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_badUserNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_badUserNameAttribute.xml", "/helloworld/rest/helloworld_badUserNameAttribute", false, null, null, false);

    }

    /***
     * Tests behavior when the userNameAttribute specifies a blank " " value
     * Expect the request to fail - we'll receive an 401 status code and error messages indicating that the claim did NOT exist
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_blankUserNameAttribute() throws Exception {

        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            goodBadBlankOrEmptyAttribute(socialSettings, "_blankUserNameAttribute.xml", "/helloworld/rest/helloworld_blankUserNameAttribute", true, null, null, true);
            genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5381W_ATTRIBUTE_NOT_FOUND); //TODO
        } else {
            goodBadBlankOrEmptyAttribute(socialSettings, "_blankUserNameAttribute.xml", "/helloworld/rest/helloworld_blankUserNameAttribute", false, null, null, false);
        }

    }

    /***
     * Tests behavior when the userNameAttribute specifies a empty "" value
     * Expect the request to fail - we'll receive an 401 status code and error messages indicating that the claim did NOT exist
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicConfigTests_emptyUserNameAttribute() throws Exception {

        if (provider.equals(SocialConstants.OPENSHIFT_PROVIDER)) {
            goodBadBlankOrEmptyAttribute(socialSettings, "_emptyUserNameAttribute.xml", "/helloworld/rest/helloworld_emptyUserNameAttribute", true, null, null, true);
            genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5381W_ATTRIBUTE_NOT_FOUND); // TODO
        } else {
            goodBadBlankOrEmptyAttribute(socialSettings, "_blankUserNameAttribute.xml", "/helloworld/rest/helloworld_blankUserNameAttribute", false, null, null, false);
        }

    }

    /***
     * Tests behavior when the groupNameAttribute specifies a good/valid value - meaning a claim can be found with the name/value
     * specified
     * Expect the value of the key specified - in this case, the username to be set in "groups"
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_goodGroupNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_goodGroupNameAttribute.xml", "/helloworld/rest/helloworld_goodGroupNameAttribute", true, "groups", socialSettings.getUserName(), true);

    }

    /***
     * Tests behavior when the groupNameAttribute specifies a bad value - meaning a claim can not be found with the name/value
     * specified
     * If we can't find the config specified groupNameAttribute, we will NOT see "groups" in the output
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_badGroupNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_badGroupNameAttribute.xml", "/helloworld/rest/helloworld_badGroupNameAttribute", true, "groups", null, false);

    }

    /***
     * Tests behavior when the groupNameAttribute specifies a blank " " value
     * If we can't find the config specified groupNameAttribute, we will NOT see "groups" in the output
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_blankGroupNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_blankGroupNameAttribute.xml", "/helloworld/rest/helloworld_blankGroupNameAttribute", true, "groups", null, false);

    }

    /***
     * Tests behavior when the groupNameAttribute specifies a empty "" value
     * If we can't find the config specified groupNameAttribute, we will NOT see "groups" in the output
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_emptyGroupNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_emptyGroupNameAttribute.xml", "/helloworld/rest/helloworld_emptyGroupNameAttribute", true, "groups", null, false);

    }

    /***
     * Tests behavior when the realmNameAttribute specifies a good/valid value - meaning a claim can be found with the name/value
     * specified
     * Expect the value of the key specified - in this case, the username
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_goodRealmNameAttribute() throws Exception {

        SocialTestSettings updatedSettings = socialSettings.copyTestSettings();
        updatedSettings.setRealm(socialSettings.getUserName());
        goodBadBlankOrEmptyAttribute(updatedSettings, "_goodRealmNameAttribute.xml", "/helloworld/rest/helloworld_goodRealmNameAttribute", true, "realmName", socialSettings.getUserName(), true);

    }

    /***
     * Tests behavior when the realmNameAttribute specifies a bad value - meaning a claim can not be found with the name/value
     * specified
     * If we can't find the config specified realmNameAttribute, we'll get the default realm
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_badRealmNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_badRealmNameAttribute.xml", "/helloworld/rest/helloworld_badRealmNameAttribute", true, "realmName", socialSettings.getRealm(), true);

    }

    /***
     * Tests behavior when the realmNameAttribute specifies a blank " " value
     * If we can't find the config specified realmNameAttribute, we'll get the default realm
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_blankRealmNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_blankRealmNameAttribute.xml", "/helloworld/rest/helloworld_blankRealmNameAttribute", true, "realmName", socialSettings.getRealm(), true);

    }

    /***
     * Tests behavior when the realmNameAttribute specifies a empty "" value
     * If we can't find the config specified realmNameAttribute, we'll get the default realm
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_emptyRealmNameAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_emptyRealmNameAttribute.xml", "/helloworld/rest/helloworld_emptyRealmNameAttribute", true, "realmName", socialSettings.getRealm(), true);

    }

    /***
     * Tests behavior when the userUniqueIdAttribute specifies a good/valid value - meaning a claim can be found with the
     * name/value specified
     * Expect the value from the attribute specified (in our test's case, the userId)
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_goodUserUniqueIdAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_goodUserUniqueIDAttribute.xml", "/helloworld/rest/helloworld_goodUserUniqueIDAttribute", true, "UniqueSecurityName", "UniqueSecurityName=" + socialSettings.getUserId(), true);

    }

    /***
     * Tests behavior when the userUniqueIdAttribute specifies a bad value - meaning a claim can not be found with the name/value
     * specified
     * Expect the default value of the username
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_badUserUniqueIdAttribute() throws Exception {

        goodBadBlankOrEmptyAttribute(socialSettings, "_badUserUniqueIDAttribute.xml", "/helloworld/rest/helloworld_badUserUniqueIDAttribute", true, "UniqueSecurityName", "UniqueSecurityName=" + socialSettings.getAdminUser(), true);

    }

    /***
     * Tests behavior when the userUniqueIdAttribute specifies a blank " " value
     * Expect the default value of the username
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_blankUserUniqueIdAttribute() throws Exception {

        //        goodBadBlankOrEmptyAttribute(socialSettings, "_blankUserUniqueIDAttribute.xml", "/helloworld/rest/helloworld_blankUserUniqueIDAttribute", true, "UniqueSecurityName", "UniqueSecurityName=" + socialSettings.getUserName(), true);
        goodBadBlankOrEmptyAttribute(socialSettings, "_blankUserUniqueIDAttribute.xml", "/helloworld/rest/helloworld_blankUserUniqueIDAttribute", true, "UniqueSecurityName", "UniqueSecurityName=" + socialSettings.getAdminUser(), true);
        ;

    }

    /***
     * Tests behavior when the userUniqueAttribute specifies a empty "" value
     * Expect the default value of the username
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfNonOIDCProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_emptyUserUniqueIdAttribute() throws Exception {

        //        goodBadBlankOrEmptyAttribute(socialSettings, "_emptyUserUniqueIDAttribute.xml", "/helloworld/rest/helloworld_emptyUserUniqueIDAttribute", true, "UniqueSecurityName", "UniqueSecurityName=" + socialSettings.getUserName(), true);
        goodBadBlankOrEmptyAttribute(socialSettings, "_emptyUserUniqueIDAttribute.xml", "/helloworld/rest/helloworld_emptyUserUniqueIDAttribute", true, "UniqueSecurityName", "UniqueSecurityName=" + socialSettings.getAdminUser(), true);

    }

    /***
     * Test userNameAttribute, userUniqueIdAttribute, groupNameAttribute, or realmNameAttribute
     *
     * @param settings
     *            - current test settings
     * @param specificConfig
     *            - the specific config suffix (for specific provider config flows, we use this to build the config name that
     *            we'll reconfigure to)
     * @param appToCall
     *            - the app name suffix to use (allows us to use filters to get the correct config in oauth/oidc generic config
     *            flows)
     * @param requestShouldSucceed
     *            - is this test supposed to be successful (used to determine the behavior to expect)
     * @param key
     *            - keyname in the output that we should validate
     * @param value
     *            - the value that we should validate for the specified key
     * @param keyExists
     *            - true/false should the key exist in the output
     * @throws Exception
     */
    public void goodBadBlankOrEmptyAttribute(SocialTestSettings settings, String specificConfig, String appToCall, Boolean requestShouldSucceed, String key, String value, Boolean keyExists) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = settings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = null;
        if (requestShouldSucceed) {
            expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
            if (key != null) {
                if (keyExists) {
                    expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT contain key:" + key + " with value: " + value + " (key Missing)", null, key);
                    expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did NOT contain key:" + key + " with value: " + value + " (value Missing)", null, value);
                } else {
                    expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Response did contain key:" + key + " with value: " + value, null, key);
                }
            }
        } else {
            expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not indicate an unauthorized error", null, SocialConstants.HTTP_ERROR_UNAUTHORIZED);
            if (isTestingOidc) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the user info does not contain the claim", SocialMessageConstants.CWWKS1738E_JWT_MISSING_CLAIM);
            } else {
                if (provider.contains(SocialConstants.OPENSHIFT_PROVIDER)) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the user name can not be extracted from the token.", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the user name can not be extracted from the token.", SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the user name can not be extracted from the token.", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                } else {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the user info does not contain the claim", SocialMessageConstants.CWWKS6102E_USER_INFO_DOES_NOT_CONTAIN_CLAIM);
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the user name can not be extracted from the token.", SocialMessageConstants.CWWKS5435E_BAD_USER_TOKEN);
                }
            }
        }
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // all
    // redirectToRpHostAndPort - helps with firewall - can't be verified with FVT, but, we should be able to ensure that we do use the value

    /***
     * Tests behavior when the redirectToRPHostAndPort specifies the valid context root for the redirect url
     * Expect the requests to complete successfully
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_goodRedirectToRPHostAndPort() throws Exception {

        genericGoodResponseTest(socialSettings, "_goodRedirectToRPHostAndPort.xml", "/helloworld/rest/helloworld_goodRedirectToRPHostAndPort");

    }

    /***
     * Tests behavior when the redirectToRPHostAndPort specifies the valid context root for the redirect url
     * Expect the requests to complete successfully
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void Social_BasicConfigTests_badRedirectToRPHostAndPort() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badRedirectToRPHostAndPort.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badRedirectToRPHostAndPort");

        List<validationData> expectations = null;
        String[] steps = null;

        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            // twitter marks the redirect as bad in the login page that it returns - you fill in your id/pw and the login request fails - throwing an exception
            steps = invoke_social_login_actions;
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.EXCEPTION_MESSAGE, SocialConstants.STRING_CONTAINS, "Should have received an unknownHost exception", null, "java.net.UnknownHostException");
        } else {
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
                expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
                expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not indicate an error processing the redirect", null, SocialMessageConstants.CWOAU0062E_REDIRECT_URI_INVALID);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the redirect uri was invalid", SocialMessageConstants.CWOAU0056E_REDIRECT_URI_NOT_REGISTERED);
            } else {
                if (provider.equalsIgnoreCase(SocialConstants.FACEBOOK_PROVIDER)) {
                    steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
                    expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                    expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Title did not contain ERROR", null, SocialConstants.ERROR_TITLE);
                    expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not indicate an error processing the redirect", null, "Not Logged In: You are not logged in. Please login and try again");
                } else {
                    if (provider.equalsIgnoreCase(SocialConstants.LINKEDIN_PROVIDER)) {
                        steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
                        expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
                        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Title did not contain " + SocialConstants.LINKEDIN_LOGIN_AND_AUTHORIZE_TITLE, null, SocialConstants.LINKEDIN_LOGIN_AND_AUTHORIZE_TITLE);
                        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Response did not indicate an error processing the redirect", null, "The redirect_uri does not match the registered value");
                    } else {
                        if (provider.equalsIgnoreCase(SocialConstants.OPENSHIFT_PROVIDER)) {
                            steps = SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY;
                            expectations = vData.addResponseStatusExpectation(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.BAD_REQUEST_STATUS);
                            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Did not find " + SocialConstants.BAD_REQUEST + " in the response message.", null, SocialConstants.BAD_REQUEST);
                            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Response did not indicate an error processing the redirect", null, "\"invalid_request\".*\"The request is missing a required parameter");
                        } else {
                            fail("Test does not currently support provider type, " + provider + ", for this test");
                        }
                    }
                }
            }
        }
        genericSocial(_testName, webClient, steps, updatedSocialTestSettings, expectations);

    }

    /***
     * Tests behavior when the redirectToRPHostAndPort specifies blank for the context root for the redirect url
     * Expect the requests to complete successfully
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_blankRedirectToRPHostAndPort() throws Exception {

        genericGoodResponseTest(socialSettings, "_blankRedirectToRPHostAndPort.xml", "/helloworld/rest/helloworld_blankRedirectToRPHostAndPort");

    }

    /***
     * Tests behavior when the redirectToRPHostAndPort specifies an empty string for the context root for the redirect url
     * Expect the requests to complete successfully
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_emptyRedirectToRPHostAndPort() throws Exception {

        genericGoodResponseTest(socialSettings, "_emptyRedirectToRPHostAndPort.xml", "/helloworld/rest/helloworld_emptyRedirectToRPHostAndPort");

    }

    /**
     * Invokes an app protected by the social login feature. The configs will vary from request to request, but this method
     * expects a good response (meaning it gets to the requested app)
     *
     * @param settings
     *            - the settings used to make the necessary calls and to validate the output from 1) each step and 2) finally the
     *            output from the app
     * @param specificConfig
     *            - the config file name suffix that we'll use to reconfigure the server (if the tests being run are using the
     *            provider specific config type of xml)
     * @param appToCall
     *            - the suffix of the application name (when using the generic config style, we have many configs in one
     *            server.xml and unique application names allow us to use filters to use the configuration that we want)
     * @return - returns the final response from the calls - if the caller needs to continue processing, they can use this
     *         response
     * @throws Exception
     */
    private Object genericGoodResponseTest(SocialTestSettings settings, String specificConfig, String appToCall) throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + specificConfig, null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = settings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + appToCall);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        return genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * responseType tests
     */

    /**
     * Social client config has responseType set to code
     * OP server allows all grantTyps
     * responseTYpe="code" requires authorization_code grantType
     * Test results in a successful end to end auth_code flow
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_codeResponseType() throws Exception {

        genericGoodResponseTest(socialSettings, "_codeResponseType.xml", "/helloworld/rest/helloworld_codeResponseType");

    }

    /**
     * Social client config has responseType set to "id_token token"
     * OP server allows all grantTyps
     * responseTYpe="id_token token" requires implicit grantType
     * Test results in a successful end to end implicit flow
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class)
    @Test
    public void Social_BasicConfigTests_idTokenTokenResponseType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_idTokenTokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_idTokenTokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);
        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_AUTH_SERVER);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);

        expectations = setGoodHelloWorldExpectations(expectations, updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN);

        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);

    }

    /**
     * Social client config has responseType set to "id_token"
     * OP server allows all grantTyps
     * responseTYpe="id_token" requires implicit grantType
     * Test results in a successful end to end implicit flow
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class)
    @AllowedFFDC({ "com.ibm.ws.security.openidconnect.server.plugins.OIDCUnsupportedResponseTypeException" })
    //enable test when response support is added @Test
    public void Social_BasicConfigTests_idTokenResponseType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_idTokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_idTokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        //        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(null, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);

        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        //       This is the logic for a positive test - once id_token alone is supported in the OP, this code
        //       should be enabled and the current test code should be deleted
        //         // Ensure that the subject principals do NOT include a JWT
        //         String jwtUserPrincipal = "Principal: {";
        //         expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);
        //
        //         expectations = setGoodHelloWorldExpectations(expectations, updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN);
        //
        //         genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);

        /** Negative test logic - remove when id_token alone is supported **/
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Response did not contain a message stating that id_token alone is NOT currently supported.", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log stating that id_token alone is NOT currently supported.", SocialMessageConstants.CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);
        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);

    }

    /**
     * Social client config has responseType set to "token"
     * OP server allows all grantTyps
     * responseTYpe="token" requires implicit grantType
     * Test results in a successful end to end implicit flow
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    //enable test when response support is added @Test
    public void Social_BasicConfigTests_tokenResponseType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_tokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);
        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_AUTH_SERVER);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);

        expectations = setGoodHelloWorldExpectations(expectations, updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN);

        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);
    }

    /**
     * Social client config has responseType set to "code"
     * OP server allows all grantTypes except authorization_code
     * responseTYpe="code" requires authorization_code grantType
     * Test results in a 401 when we make the initial request
     *
     * @throws Exception
     */
    //@ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException" })
    @Test
    public void Social_BasicConfigTests_codeResponseType_OPMissingAuthCodeGrant() throws Exception {

        if (provider.equalsIgnoreCase(SocialConstants.OPENSHIFT_PROVIDER)) {
            Log.info(thisClass, "skipIfOpenShift", "OpenShift Config - skip test");
            testSkipped();
            return;
        }
        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_codeResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_opMissingAuthCodeGrantType_codeResponseType");

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Token response did not contain the correct error description.", null, Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + Constants.MSG_INVALID_GRANT_TYPE + ".*" + Constants.AUTH_CODE_GRANT_TYPE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Token response did not contain the correct error description.", null, Constants.INVALID_GRANT_TYPE_MSG);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * Social client config has responseType set to "id_token token"
     * OP server allows all grantTyps except implicit
     * responseTYpe="id_token token" requires implicit grantType
     * Test results in a 401 when we make the initial request
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException" })
    @Test
    public void Social_BasicConfigTests_idTokenTokenResponseType_OPMissingImplicitGrantType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_idTokenTokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_opMissingImplicitGrantType_idTokenTokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Token response did not contain the correct error description.", null, Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + Constants.MSG_INVALID_GRANT_TYPE + ".*" + Constants.IMPLICIT_GRANT_TYPE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Token response did not contain the correct error description.", null, Constants.INVALID_GRANT_TYPE_MSG);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * Social client config has responseType set to "id_token"
     * OP server allows all grantTyps except implicit
     * responseTYpe="id_token" requires implicit grantType
     * Test results in a 401 when we make the initial request
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException" })
    //enable test when response support is added @Test
    public void Social_BasicConfigTests_idTokenResponseType_OPMissingImplicitGrantType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_idTokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_opMissingImplicitGrantType_idTokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Token response did not contain the correct error description.", null, Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + Constants.MSG_INVALID_GRANT_TYPE + ".*" + Constants.IMPLICIT_GRANT_TYPE + ".*" + Constants.INVALID_GRANT_TYPE_MSG);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * Social client config has responseType set to "token"
     * OP server allows all grantTyps except implicit
     * responseTYpe="token" requires implicit grantType
     * Test results in a 401 when we make the initial request
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException" })
    //enable test when response support is added @Test
    public void Social_BasicConfigTests_tokenResponseType_OPMissingImplicitGrantType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_opMissingImplicitGrantType_tokenResponseType");

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Token response did not contain the correct error description.", null, Constants.ERROR_RESPONSE_DESCRIPTION + ".*" + Constants.MSG_INVALID_GRANT_TYPE + ".*" + Constants.IMPLICIT_GRANT_TYPE + ".*" + Constants.INVALID_GRANT_TYPE_MSG);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * responseMode tests - test with responseType to show that Auth_code and Implicit flows work with
     * responseMode = "form_post" - we can't really validate anything in the conversations - we can
     * only validate that the end to end flow works
     */

    /**
     * Same setup plus responseMode="form_post" and results as the Social_BasicConfigTests_codeResponseType tests
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicConfigTests_responseModeFormPost_codeResponseType() throws Exception {

        genericGoodResponseTest(socialSettings, "_codeResponseType.xml", "/helloworld/rest/helloworld_responseModeFormPost_codeResponseType");

    }

    /**
     * Same setup plus responseMode="form_post" and results as the Social_BasicConfigTests_idTokenTokenResponseType tests
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class)
    @Test
    public void Social_BasicConfigTests_responseModeFormPost_idTokenTokenResponseType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_responseModeFormPost_idTokenTokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_responseModeFormPost_idTokenTokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);
        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_AUTH_SERVER);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);

        expectations = setGoodHelloWorldExpectations(expectations, updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN);

        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);

    }

    /**
     * Same setup plus responseMode="form_post" and results as the Social_BasicConfigTests_idTokenResponseType tests
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class)
    @AllowedFFDC({ "com.ibm.ws.security.openidconnect.server.plugins.OIDCUnsupportedResponseTypeException" })
    //enable test when response support is added @Test
    public void Social_BasicConfigTests_responseModeFormPost_idTokenResponseType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_responseModeFormPost_idTokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_responseModeFormPost_idTokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        //        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(null, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);

        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        //       This is the logic for a positive test - once id_token alone is supported in the OP, this code
        //       should be enabled and the current test code should be deleted
        //         // Ensure that the subject principals do NOT include a JWT
        //         String jwtUserPrincipal = "Principal: {";
        //         expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);
        //
        //         expectations = setGoodHelloWorldExpectations(expectations, updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN);
        //
        //         genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);

        /** Negative test logic - remove when id_token alone is supported **/
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Response did not contain a message stating that id_token alone is NOT currently supported.", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in the log stating that id_token alone is NOT currently supported.", SocialMessageConstants.CWWKS5495E_REDIRECT_REQUEST_CONTAINED_ERROR);
        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);

    }

    /**
     * Same setup plus responseMode="form_post" and results as the Social_BasicConfigTests_tokenResponseType tests
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProvider.class)
    //enable test when response support is added @Test
    public void Social_BasicConfigTests_responseModeFormPost_tokenResponseType() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_responseModeFormPost_tokenResponseType.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_responseModeFormPost_tokenResponseType");
        updatedSocialTestSettings.setFlowType(Constants.IMPLICIT_FLOW);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS);
        expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token", null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null, Constants.LOGIN_PROMPT);

        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_AUTH_SERVER);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);

        expectations = setGoodHelloWorldExpectations(expectations, updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_IMPLICIT_LOGIN);

        genericSocial(_testName, webClient, SocialConstants.LIBERTYOP_INVOKE_SOCIAL_IMPLICIT_LOGIN_ACTIONS, updatedSocialTestSettings, expectations);
    }

    /**
     * accessTokenRequired is set to true - test will pass a good token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_tokenPassed_noLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will pass a good token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_tokenPassed_badLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will pass a good token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_tokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        String access_token = getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will NOT pass a token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_tokenNotPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will NOT pass a token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_tokenNotPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will NOT pass a token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_tokenNotPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will pass a bad token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_badTokenPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = null;
        if (provider.contains(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        } else {
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (validationEndpointIsIntrospect()) {
                    expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                } else {
                    expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
                }
            }
        }
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will pass a bad token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_badTokenPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = null;
        if (provider.contains(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        } else {
            if (validationEndpointIsIntrospect()) {
                expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            } else {
                expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
            }
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (validationEndpointIsIntrospect()) {
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                } else {
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
                }
            }
        }
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true - test will pass a bad token
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "org.jose4j.lang.JoseException" })
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_badTokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_usingIntrospect_accessTokenRequiredTrue");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredTrue");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = null;
        if (provider.contains(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        } else {
            if (validationEndpointIsIntrospect()) {
                expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
            } else {
                expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
            }
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (validationEndpointIsIntrospect()) {
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                } else {
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
                }
            }
        }
        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is not set, so default is false - test will pass a token
     * request should fail
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredDefault_tokenPassed() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is not set, so default is false - test will not pass a token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredDefault_tokenNotPassed() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is not set, so default is false - test will pass a bad token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredDefault_badTokenPassed() throws Exception {

        String access_token = "somebadvalueForAnAccessToken";
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will pass a token
     * request should fail
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_tokenPassed_noLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will pass a token
     * request should fail
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_tokenPassed_badLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        String access_token = getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will pass a token
     * request should fail
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_tokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        String access_token = getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will not pass a token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_tokenNotPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will not pass a token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_tokenNotPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will not pass a token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_tokenNotPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will pass a bad token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_badTokenPassed_noLTPA() throws Exception {

        String access_token = "somebadvalueForAnAccessToken";
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will pass a bad token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_badTokenPassed_badLTPA() throws Exception {

        String access_token = "somebadvalueForAnAccessToken";
        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to false - test will pass a bad token
     * request should succeed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredFalse_badTokenPassed_goodLTPA() throws Exception {

        String access_token = "somebadvalueForAnAccessToken";
        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenRequiredFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_tokenPassed_noLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_tokenPassed_badLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_tokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        String access_token = getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsIncluded_tokenNotPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsIncluded");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsIncluded_tokenNotPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsIncluded");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsIncluded_tokenNotPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsIncluded");

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" }) // LibertyOP issues this
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsOmitted_tokenNotPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        List<validationData> expectations = null;
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
            expectations = setUnexpectedErrorPageForSocialLogin(SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER);
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" }) // LibertyOP issues this
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsOmitted_tokenNotPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        List<validationData> expectations = null;
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
            expectations = setUnexpectedErrorPageForSocialLogin(SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER);
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" }) // LibertyOP issues this
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsOmitted_tokenNotPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
        }

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsIncluded_badTokenPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsIncluded");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (validationEndpointIsIntrospect()) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
            }
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        }
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsIncluded_badTokenPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsIncluded");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (validationEndpointIsIntrospect()) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
            }
        } else {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not get user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        }

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsIncluded_badTokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsIncluded");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (validationEndpointIsIntrospect()) {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
            }
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsOmitted_badTokenPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        List<validationData> expectations = null;
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
            expectations = setUnexpectedErrorPageForSocialLogin(SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER);
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (validationEndpointIsIntrospect()) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
            }
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException", "com.ibm.ws.security.social.error.SocialLoginException" }) // LibertyOP issues this
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsOmitted_badTokenPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        List<validationData> expectations = null;
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
            expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
            expectations = setUnexpectedErrorPageForSocialLogin(SocialMessageConstants.CWWKS5416W_OUTGOING_REQUEST_MISSING_PARAMETER);
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (validationEndpointIsIntrospect()) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
            }
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "org.jose4j.lang.JoseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException", "com.ibm.ws.security.social.error.SocialLoginException" }) // LibertyOP issues this
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedTrue_optionalParmsOmitted_badTokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        // when using the introspect endpoint, we need the clientId and ClientSecret in the config (we want to test with minimum configs, so, don't want to
        // add clientId and clientSecret in all cases, so, have 2 configs, ...
        if (provider.contentEquals(SocialConstants.LIBERTYOP_PROVIDER) && validationEndpointIsIntrospect()) {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_introspect_optionalParmsOmitted");
        } else {
            updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedTrue_optionalParmsOmitted");
        }
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (validationEndpointIsIntrospect()) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we could not authenticate the user", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            } else {
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
            }
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedDefault_tokenPassed() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedDefault_tokenNotPassed() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedDefault_badTokenPassed() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_tokenPassed_noLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_tokenPassed_badLTPA() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // We should have access to the app, but, should NOT have been because of the access_token we passed
        expectations = vData.addExpectation(expectations, SocialConstants.OPENSHIFT_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_tokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        String access_token = getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_tokenNotPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_tokenNotPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_tokenNotPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");

        //        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        //
        //        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_badTokenPassed_noLTPA() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_badTokenPassed_badLTPA() throws Exception {

        WebClient webClient = createWebClientWithBadCookieValue("SomeBadCookieValue");

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenSupportedFalse_badTokenPassed_goodLTPA() throws Exception {

        WebClient webClientForAccess = getAndSaveWebClient();
        getAccessToken(webClientForAccess);
        WebClient webClient = createWebClientWithGoodCookieValue(webClientForAccess);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenSupportedFalse");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    // Test with both accessTokenRequired and AccessTokenSupported set to true - show that we
    // get the accessTokenRequired behavior.  Combinations of the two config attributes set to true/false, false/true and false/false
    // are tested via other test cases targetting specific values for one and taking the default for the other.
    /**
     * accessTokenRequired is set to true, accessTokenSupported is set to true - test will pass a good token
     * we should have access to the app using the token passed
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_accessTokenSupportedTrue_tokenPassed() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_both_accessTokenRequiredTrue_accessTokenSupportedTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true, accessTokenSupported is set to true - test will NOT pass a token
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_accessTokenSupportedTrue_tokenNotPassed() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_both_accessTokenRequiredTrue_accessTokenSupportedTrue");

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenRequired is set to true, accessTokenSupported is set to true - test will pass a bad token
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "org.jose4j.lang.JoseException" })
    @Test
    public void Social_BasicConfigTests_accessTokenRequiredTrue_accessTokenSupportedTrue_badTokenPassed() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_both_accessTokenRequiredTrue_accessTokenSupportedTrue");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred("somebadvalueForAnAccessToken"));

        List<validationData> expectations = null;
        if (provider.contains(SocialConstants.OPENSHIFT_PROVIDER)) {
            expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5371E_OPENSHIFT_USER_API_RESPONSE_BAD);
        } else {
            expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE, SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
        }
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
                if (validationEndpointIsIntrospect()) {
                    expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                } else {
                    expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO, SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS, SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
                    expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Message log did not contain message indicating that the token could not be verified.", SocialMessageConstants.CWWKS1617E_USERINFO_WITH_BAD_ACCESS_TOKEN);
                }
            }
        }

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenHeaderName is set to "Authorization" - test will pass the access_token using "Authorization:"
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenHeaderNameBearer_passAsBearer() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenHeaderNameBearer");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenHeaderName is set to "Authorization:" - test will pass the access_token using "X-Forwarded-Access-Token"
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenHeaderNameBearer_passAsXForwardedAccessToken() throws Exception {
        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenHeaderNameBearer");
        updatedSocialTestSettings.setHeaderName(SocialConstants.X_FORWARDED_ACCESS_TOKEN_HEADER);
        updatedSocialTestSettings.setHeaderValue(access_token);

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5376W_TOKEN_NOT_FOUND_IN_REQUEST_HEADER, SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenHeaderName is set to "X-Forwarded-Access-Token" - test will pass the access_token using
     * "X-Forwarded-Access-Token"
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenHeaderNameXForwardedAccessToken_passAsXForwardedAccessToken() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenHeaderNameXForwardedAccessToken");
        updatedSocialTestSettings.setHeaderName(SocialConstants.X_FORWARDED_ACCESS_TOKEN_HEADER);
        updatedSocialTestSettings.setHeaderValue(access_token);

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenHeaderName is set to "X-Forwarded-Access-Token" - test will pass the access_token using "Authorization:"
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenHeaderNameXForwardedAccessToken_passAsBearer() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenHeaderNameXForwardedAccessToken");
        updatedSocialTestSettings.setHeaderName(SocialConstants.BEARER_HEADER);
        updatedSocialTestSettings.setHeaderValue(cttools.buildBearerTokenCred(access_token));

        List<validationData> expectations = setErrorPageForSocialLogin(SocialMessageConstants.CWWKS5376W_TOKEN_NOT_FOUND_IN_REQUEST_HEADER, SocialMessageConstants.CWWKS5375E_MISSING_REQUIRED_ACCESS_TOKEN);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /**
     * accessTokenHeaderName is set to "Authorization:" - test will pass the access_token using "Authorization:"
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOIDCStyleProviderOrProviderConfig.class)
    @Test
    public void Social_BasicConfigTests_accessTokenHeaderNameUserDefined_passUserDefined() throws Exception {

        String access_token = getAccessToken();
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_accessTokenHeaderNameUserDefined");
        updatedSocialTestSettings.setHeaderName("UserDefinedHeader");
        updatedSocialTestSettings.setHeaderValue(access_token);

        List<validationData> expectations = setGoodHelloWorldExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation, SocialConstants.INVOKE_SOCIAL_RESOURCE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Did NOT find the access_token in the helloworld output.", null, access_token);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }

    /** maybe test with setting other values in the call from the client */

    public String getAccessToken() throws Exception {

        return getAccessToken(null);
    }

    public String getAccessToken(WebClient webClient) throws Exception {

        if (webClient == null) {
            webClient = getAndSaveWebClient();
        }

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        Object response = genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        msgUtils.printAllCookies(webClient);

        return validationTools.getValueFromTestAppOutput(response, "Access Token: ");

    }

    public WebClient createWebClientWithGoodCookieValue(WebClient origClient) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Cookie badCookie = new Cookie(origClient.getCookieManager().getCookie("LtpaToken2").getDomain(), "LtpaToken2", origClient.getCookieManager().getCookie("LtpaToken2").getValue());
        Log.info(thisClass, _testName, "Before updating Cookies");
        msgUtils.printAllCookies(webClient);
        webClient.getCookieManager().addCookie(badCookie);
        Log.info(thisClass, _testName, "After updating Cookies");
        msgUtils.printAllCookies(webClient);

        return webClient;
    }

    public WebClient createWebClientWithBadCookieValue(String badValue) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Cookie badCookie = new Cookie("localhost", "LtpaToken2", badValue);
        Log.info(thisClass, _testName, "Before updating Cookies");
        msgUtils.printAllCookies(webClient);
        webClient.getCookieManager().addCookie(badCookie);
        Log.info(thisClass, _testName, "After updating Cookies");
        msgUtils.printAllCookies(webClient);

        return webClient;
    }

    // nonce -

    // oauth only - linkedin specifically
    // userApiNeedsSpecialHeader

    // oidc only
    // clockskew
    // signatureAlgorithm
    // hostNameVerificationEnabed
}
