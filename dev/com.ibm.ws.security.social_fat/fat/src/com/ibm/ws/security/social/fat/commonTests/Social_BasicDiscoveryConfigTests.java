/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.commonTests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The purpose of this test is to verify that discovered endpoints can be used with the Social Login feature and configurations.
 * Discovered endpoints for authorization, token and JwkUri are specified using the config parameter "discoveryEndpoint"
 * This test class tests the discoveryEndpoint config parameter along with a variety of other config parameters for common main paths.  In this class,
 * the discoveryEndpoint config results in successfully discovered endpoints. The Social_ErrorDiscoveryConfigTests tests errors and messages
 * with the discoveryEndpoint config parameter.
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class Social_BasicDiscoveryConfigTests extends SocialCommonTest {
    
    public static boolean isTestingOidc = false;  // subclasses may set this to true

    public static Class<?> thisClass = Social_BasicDiscoveryConfigTests.class;
    // list of providers that support calls using Basic (instead of post)
    // update list as we automate the other providers
    // facebook does NOT support
    // gitHub DOES support
    List<String> supportsClientSecretBasic = Arrays.asList(SocialConstants.GITHUB_PROVIDER, SocialConstants.LIBERTYOP_PROVIDER);

    /***
     * Please NOTE:
     * These tests will run with the generic "social" configurations, or with each of the
     * provider configs which extend this class. Currently the LibertyOP provider is the only provider
     * which supports discovery and extends this class. Google supports discovery, but the testing cannot be automated.
     * 
     * The provider configs only support 1 (one) instance of that configuration in
     * a server.xml. This means that we need to reconfigure for each of those tests. (reconfigIfProviderSpecificConfig
     * will only do a reconfig to the server.xml specified IFFFFF  the extending class has indicated that we're
     * using a provider specific configuration.
     *
     * Note that at creation time, this class is only extended for use by the LibertyOP provider.
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
                // 248970 this next one is seen occasionally on windows when a bad ssl config is being deliberately used. 
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL);
                if(isTestingOidc){
                    genericTestServer.addIgnoredServerException(SocialMessageConstants.SRVE8094W);
                }
            }
            if(testOPServer != null && isTestingOidc){
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKS1751E_INVALID_ISSUER);
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            }
        }
    }
   
    /**
     * Verify that when the authorization and token endpoints are discovered, that a bad client Id still results in an error.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    @Test
    public void Social_BasicDiscoveryConfigTests_badClientId() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badClientId.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badClientId");

        List<validationData> expectations = null;
        expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not get a message about an unknown clientId", null, SocialMessageConstants.CWOAU0061E_BAD_CLIENTID);

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);

    }
    
    /**
     * Verify that when the authorization and token endpoints are discovered, that a bad client secret still results in an error.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Test
    public void Social_BasicDiscoveryConfigTests_blankClientSecret() throws Exception {
        
        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_blankClientSecret.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_blankClientSecret");

        String[] actions = invoke_social_login_actions;
        String finalAction = invoke_social_login_actions[invoke_social_login_actions.length - 1];
        List<validationData> expectations = null;
        expectations = vData.addResponseStatusExpectation(expectations, finalAction, SocialConstants.BAD_REQUEST_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unable to contact provider exception", SocialMessageConstants.CWWKS1708E_UNABLE_TO_CONTACT_PROVIDER);        genericSocial(_testName, webClient, actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that a main path works with social media enabled for login.
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicDiscoveryConfigTests_enabledTrue() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_enabledTrue");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }


    /**
     * Verify that when the authorization and token endpoints are discovered, that a bad auth filter ref results in expected error on config.
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicDiscoveryConfigTests_badAuthFilterRef() throws Exception {

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
     * The global SSL config uses the trust store that has the provider, so, adding it to the provider config does little.
     * We just want to show that it doesn't break anything when endpoints are  discovered.
     *
     * @throws Exception
     */
    // NOTE: bad global provider trust with good provider specific trust is tested in test class: Social_BasicConfigTests_omittedGlobalTrust
    // Reconfiguring the global trust can be problematic!
    @Test
    public void Social_BasicDiscoveryConfigTests_goodSSLRef_akaGoodTrust() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodTrust.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodTrust");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that the main flow is successful with configured
     * tokenEndpointAuthMethod=clientSecretBasic
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicDiscoveryConfigTests_tokenEndpointAuthMethod_clientSecretBasic() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tEAM_clientSecretBasic.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_tEAM_clientSecretBasic");

        List<validationData> expectations = null;
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that the main flow is successful with configured
     * tokenEndpointAuthMethod=clientSecretPost
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicDiscoveryConfigTests_tokenEndpointAuthMethod_clientSecretPost() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_tEAM_clientSecretPost.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_tEAM_clientSecretPost");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that the main flow is successful with configured realm
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicDiscoveryConfigTests_realmName() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_realmName.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_realmName");
        updatedSocialTestSettings.setRealm("myLibertyOPRealm");
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that the main flow is successful with configured
     * mapToUserRegistry false
     *
     * @throws Exception
     */
    @Test
    public void Social_BasicDiscoveryConfigTests_mapToUserRegistryFalse() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_mapToUserRegistryFalse.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryFalse");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }
    /**
     * Verify that when the authorization and token endpoints are discovered, that the expected error is received when 
     * mapToUserRegistry is true and the user is not found in the registry.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" })
    @Test
    public void Social_BasicDiscoveryConfigTests_mapToUserRegistryTrue_userNotInRegistry() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_mapToUserRegistryTrue_userNotInRegistry.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_mapToUserRegistryTrue");

        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the authentication failed", SocialMessageConstants.CWWKS1106A_AUTHENTICATION_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating the user id was not good", updatedSocialTestSettings.getUserName());
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that the main flow is successful with JWT and good
     * jwt builder and default signature algorithm RS256.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicDiscoveryConfigTests_goodJwt_builder() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_goodJwt_builder.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_good_jwt_builder");
        updatedSocialTestSettings.setIssuer(updateDefaultIssuer(updatedSocialTestSettings, "goodJwtBuilder"));

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, addJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Verify that when the authorization and token endpoints are discovered, that the main flow is successful with JWT and good
     * jwt builder and configured signature algorithm HS256.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidTokenException" })
    @Test
    public void Social_BasicDiscoveryConfigTests_jwt_builder_HS256() throws Exception {

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


    /**
     * Verify that when the authorization and token endpoints are discovered, that the expected error is received for a bad scope.
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicDiscoveryConfigTests_badScope() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_badScope.xml", null);

        WebClient webClient = getAndSaveWebClient();
        String[] steps = invoke_social_login_actions;

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badScope");
        Log.info(thisClass, "**debug**"," oidcLoginStyle = " + oidcLoginStyle);
        List<validationData> expectations = null;
   
        expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        expectations = vData.addExpectation(expectations, perform_social_login, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we can't process the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that we cannot process the token", SocialMessageConstants.CWWKS1712E_ID_TOKEN_MISSING);
        
        genericSocial(_testName, webClient, steps, updatedSocialTestSettings, expectations);

    }
    
    /**
     * Verify that when the authorization and token endpoints are discovered, that additional parameters and values can be passed
     * on both the authorization and token endpoints. With the OIDC OpenID Connect provider, these additional parameters are ignored and the 
     * main path flow is successful.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicDiscoveryConfigTests_addExtraAuthAndTokenParms_ignoredByOP() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_addParms.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_addParms");

        List<validationData> expectations = null;
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }
    
    /**
     * Verify that when the authorization and token endpoints are discovered, that additional parameters and values can be passed
     * on both the authorization and token endpoints. With the OIDC OpenID Connect provider, that bad parameters are not passed along
     * and the main flow is successful.
     * 
     * @throws Exception
     */
    @Test
    public void Social_BasicDiscoveryConfigTests_addBadAuthAndTokenParms_parmsNotSent() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_addBadParms.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_addBadParms");

        List<validationData> expectations = null;
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }
    
    /**
     * Verify that when the authorization and token endpoints are discovered, that additional parameters and values can be passed
     * to the authorization endpoint with forwardLoginParameter (login_hint=bob@example.com). The value of the forwarded parameter is taken from the request parameters on the
     * protected resource invocation. When combined with the authzParameter, both parameters are found on the call to the authorization endpoint.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicDiscoveryConfigTests_forwardLoginParameter() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_forwardLoginParameter.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_forwardLoginParameter?login_hint=bob@example.com");

        List<validationData> expectations = null;
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.TRACE_LOG, SocialConstants.STRING_CONTAINS, 
                "OP trace log did not contain entry indicating that the login_hint parameter was forwarded to the OP with value.", "name:.*response_type.*values:.*\\[code\\].*name:.*login_hint.*values:.*\\[bob@example.com\\]");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.TRACE_LOG, SocialConstants.STRING_CONTAINS, 
                "OP trace log did not contain entry indicating that the authzParameter was passed to the OP.", "name:.*response_type.*values:.*\\[code\\].*name:.*mq_authz1.*values:.*\\[mqa1234\\]");

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    // oauth & oidc only
    // responseType - "code", "token" (for oidc additionally: "id_token", "id_token token")- future
    // nonce -

    // oauth only - linkedin specifically
    // userApiNeedsSpecialHeader

    // oidc only
    // clockskew
    // signatureAlgorithm
    // hostNameVerificationEnabed
}
