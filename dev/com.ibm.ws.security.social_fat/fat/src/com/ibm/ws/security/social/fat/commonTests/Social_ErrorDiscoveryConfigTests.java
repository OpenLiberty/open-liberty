/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfGenericConfig;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfNonOIDCProviderConfig;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfNotLibertyOP;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfOAuthStyleProvider;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfOIDCStyleProvider;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfProviderConfig;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest.skipIfTwitter;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test covers common error conditions and startup messages with the configuration of Social Login discovery. This testing automates the 
 * test cases to ensure that messages are generated to enable customers to diagnose and fix common errors on their own.
 * 
 * The social login discovery function differs from OIDC discovery in how the server reconfig is processed.  Once an initial discovery is performed
 * in the test setup, a reconfig with a new server config file does NOT immediately result in processing discovery again. Instead, discovery using the new config is 
 * processed after a resource access is attempted. Therefore, the tests check for error messages with a bad config on the attempt to 
 * display the login form (rather than on the reconfigServer step as is done with OIDC client discovery).
 * 
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class Social_ErrorDiscoveryConfigTests extends SocialCommonTest {
    
    public static boolean isTestingOidc = false;  // subclasses may set this to true

    public static Class<?> thisClass = Social_ErrorDiscoveryConfigTests.class;

    List<String> supportsClientSecretBasic = Arrays.asList(SocialConstants.GITHUB_PROVIDER, SocialConstants.LIBERTYOP_PROVIDER);

    /***
     * Please NOTE:
     * These tests will run with the generic "social" configurations, or with each of the
     * provider configs which extend this class. Currently the LibertyOP provider is the only provider
     * which supports discovery besides Google so the Liberty OP is the only provider extending this class.
     * 
     * The provider configs only support 1 (one) instance of that configuration in
     * a server.xml. This means that we need to reconfigure for each of those tests. (reconfigIfProviderSpecificConfig
     * will only do a reconfig to the server.xml specified IFFFFF  the extending class has indicated that we're
     * using a provider specific configuration.
     * Also NOTE: there are conditional rules that will allow us to only run a test if we're running
     * the generic (SocialGenericConfig) or provider specific (SocialProviderConfig) config instance.
     * I've left the "reconfig" call in the test method and specified a config (that probably doesn't exist)
     * for tests that DO NOT run in the provider specific cases. Who knows, the config attribute may be
     * added soem day.
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
               genericTestServer.addIgnoredServerException(SocialMessageConstants.SRVE8094W); 
               // 248970 this next one is seen occasionally on windows when a bad ssl config is being deliberately used. 
                genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL);
            }
            if(testOPServer != null && isTestingOidc){
                testOPServer.addIgnoredServerException(SocialMessageConstants.CWOAU0039W_OAUTH20_FILTER_REQUEST_NULL);  // malformed url
            }
        }
    }
   
    /**
     * Verify when using discovered endpoints and having hostNameVerificationEnabledTrue set to true, the discovery will fail as we have to use
     * localhost in our test automation. 
     *
     * CWWKS6115E: A successful response was not returned from the URL of [https://localhost:8947/oidc/endpoint/OidcConfigSample/.mal-formed/openid-configuration]. ...
     * CWWKS5391E: The social login client [oidcLogin_hostNameVerificationEnabledTrue] failed to obtain OpenID Connect provider endpoint information through the discovery endpoint URL of [https://localhost:8947/oidc/endpoint/OidcConfigSample_JWT_JWK/.well-known/openid-configuration].
     *   Update the configuration for the Social Login (oidcLogin configuration) with the correct HTTPS discovery endpoint URL. 
     * @throws Exception
     */
    @AllowedFFDC({"javax.net.ssl.SSLException", "javax.net.ssl.SSLPeerUnverifiedException",
        "com.ibm.websphere.security.jwt.InvalidTokenException",
        "com.ibm.websphere.security.jwt.InvalidClaimException", 
        "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_ErrorDiscoveryConfigTests_discoveryFailsWithHostNameVerificationEnabledTrue() throws Exception {
        testOPServer.reconfigServer("op_server_disc_hostNameVerify.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_hostNameVerify.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_hostNameVerificationEnabledTrue");
        
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.FORBIDDEN, null, SocialConstants.FORBIDDEN);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS6115E");
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5391E");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
    }

    /**
     * Verify that when the discovery endpoint Url is mal-formed that error messages are logged so the administrator can diagnose and fix the error.
     *
     * CWWKS6114E: A successful response was not returned from the URL of [https://localhost:8947/oidc/endpoint/OidcConfigSample/.mal-formed/openid-configuration]. 
     *    The [404] response status and the [Not Found] error are from the discovery request.
     * CWWKS5391E: The social login client [oidcLogin1] failed to obtain OpenID Connect provider endpoint information through the discovery endpoint URL of [https://localhost:8947/oidc/endpoint/OidcConfigSample/.mal-formed/openid-configuration].
     *    Update the configuration for the Social Login (oidcLogin configuration) with the correct HTTPS discovery endpoint URL. 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_ErrorDiscoveryConfigTests_malFormedUrl() throws Exception {

        genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_malFormedUrl.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
        
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS6114E.*404");
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5391E");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
    }
    
    /**
     * Verify that when the discovery endpoint Url is empty that error message is logged as shown below. The failure occurs on access
     * to the authorization endpoint because blank or null indicates no discovery and it is not detected as a discovery endpoint error.
     *
     * CWWKS5479E: The configuration attribute [authorizationEndpoint] that is required in the social login configuration [oidcLogin1] is missing or empty. 
     * Verify that the attribute is configured, that it is not empty, and that it does not consist of only white space characters.
     * 
     * @throws Exception
     */
    @Test
    public void Social_ErrorDiscoveryConfigTests_emptyDiscoveryEndptUrl() throws Exception {

        genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_emptyDiscoveryUrl.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
        
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5479E.*authorizationEndpoint.*oidcLogin1");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
    }
    
    /**
     * Verify that when the discovery endpoint Url is blank that an error messages is logged as shown below. The failure occurs on access
     * to the authorization endpoint because blank or null indicates no discovery and it is not detected as a discovery endpoint error.
     *
     * CWWKS5479E: The configuration attribute [authorizationEndpoint] that is required in the social login configuration [oidcLogin1] is missing or empty. 
     * Verify that the attribute is configured, that it is not empty, and that it does not consist of only white space characters.     * @throws Exception
     */
    @Test
    public void Social_ErrorDiscoveryConfigTests_emptyDiscoveryEndptBlank() throws Exception {

        genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_blankDiscoveryUrl.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
        
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5479E.*authorizationEndpoint.*oidcLogin1");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
    }

    /**
     * Verify that when the discovery endpoint Url is not HTTPS that an error is logged so the administrator can diagnose and fix the error.
     * 
     * CWWKS5391E: The social login client [oidcLogin1] failed to obtain OpenID Connect provider endpoint information through the discovery endpoint URL of [http://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration].
     *   Update the configuration for the Social Login (oidcLogin configuration) with the correct HTTPS discovery endpoint URL. 
     * 
     * @throws Exception
     */
    @Test
    public void Social_ErrorDiscoveryConfigTests_nonHttpsUrl() throws Exception {

        genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_nonHttpsUrl.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
        
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5391E");

        genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
    }
  
  /**
   * Verify that when discovery is configured that the discovered endpoints override any explicitly configured endpoints and that access
   * to the protected resource is successful with the discovered endpoints. 
   * 
   * The messages should be logged to indicate that the discovered endpoints override the configured endpoints, but then access to the
   * social resource should succeed.
   * 
   * CWWKS6107W: The social login client [oidcLogin_overrideEndpts] configuration specifies both the [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration] 
   *    discovery endpoint URL and the other endpoints, but must be configured with either the discovery endpoint or the other endpoints.
   *    The client used the information from the discovery request and ignored the other endpoints [authorizationEndpoint, tokenEndpoint, ].
   * 
   * CWWKS6110I: The social login client [oidcLogin_overrideEndpts] configuration has been established with the information from the discovery endpoint URL 
   *    [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. This information enables the client to interact with the 
   *    OpenID Connect provider to process the requests such as authorization and token.
   *
   * @throws Exception
   */
  @Mode(TestMode.LITE)
  @Test
  public void Social_ErrorDiscoveryConfigTests_discoveredEndpointsOverrideConfigured() throws Exception {

      genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_overrideEndpts.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
      WebClient webClient = getAndSaveWebClient();

      SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
      updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_overrideEndpts");

      List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovered endpoints were used", "CWWKS6107W");
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery completed", "CWWKS6110I");
     
      genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);
  }
  
  /**
   * Verify that when discovery is configured that the discovered issuer identifier overrides and explicitly configured issuer identifier.
   * 
   * The messages should be logged to indicate that the discovered issuer identifier overrides the configured identifier,  and then access to the
   * social resource should succeed.
   * 
   * CWWKS6108W: The social login client [oidcLogin_badIssuer] configuration specifies both the [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration] 
   *   discovery endpoint URL and the issuer identifier [issuer]. The client used the information from the discovery request and ignored the configured issuer identifier..
   * 
   * CWWKS6110I: The social login client [oidcLogin_badIssuer] configuration has been established with the information from the discovery endpoint URL 
   *   [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. 
   *   This information enables the client to interact with the OpenID Connect provider to process the requests such as authorization and token.
   *
   * @throws Exception
   */
  @Test
  public void Social_ErrorDiscoveryConfigTests_discoveredIssuerOverridesConfigured() throws Exception {

      genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_badIssuer.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
      WebClient webClient = getAndSaveWebClient();

      SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
      updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badIssuer");

      List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovered issuer identifier was used", "CWWKS6108W");
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery completed", "CWWKS6110I");
     
      genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);
  }
  
    /**
     * Verify that when discovery is configured that the discovered JWKS URI overrides and explicitly configured JwksURI and the protected resource succeeds.
     * The messages should be logged to indicate that the discovered JWKS URI overrides the configured identifier,  and then access to the
     * social resource should succeed.
     * 
     * CWWKS6108W: The social login client [oidcLogin_badIssuer] configuration specifies both the [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration] 
     * discovery endpoint URL and the issuer identifier [issuer]. The client used the information from the discovery request and ignored the configured issuer identifier..
     *
     * CWWKS6110I: The social login client [oidcLogin_badIssuer] configuration has been established with the information from the discovery 
     * endpoint URL [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. 
     * This information enables the client to interact with the OpenID Connect provider to process the requests such as authorization and token.
     *
     * @throws Exception
     */
   @Mode(TestMode.LITE)
   @Test
   public void Social_ErrorDiscoveryConfigTests_discoveredJwkUriOverridesConfigured() throws Exception {
      
     genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_overrideJwksUri.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
     WebClient webClient = getAndSaveWebClient();

     SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
     updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodJwksUri");

     List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
     expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovered jwks URI was used", "CWWKS6107W.*jwksUri");
     expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery completed", "CWWKS6110I");
      
     genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);

   }
   
   /**
    * Verify that when discovery is configured such that the Social client uses defaults and the OP supports a different value that the Social client
    * value will auto adjust to match the OP value.
    * 
    * Messages should be logged to indicate that the Social login client value was auto adjusted to that of the OP and discovery should complete successfully.
    * 
    * CWWKS6109I: The social login client [oidcLogin_autoAdjust] configuration specifies [client_secret_post], a default value for the [tokenEndpointAuthMethod] 
    *   and as a result of discovery this is changed to [client_secret_basic].
    * CWWKS6109I: The social login client [oidcLogin_autoAdjust] configuration specifies [profile email openid], a default value for the [scope]
    *    and as a result of discovery this is changed to [openid].
    * CWWKS6110I: The social login client [oidcLogin_autoAdjust] configuration has been established with the information from the discovery endpoint URL [https://localhost:8947/oidc/endpoint/OidcConfigSample_autoAdjust/.well-known/openid-configuration]. 
    *   This information enables the client to interact with the OpenID Connect provider to process the requests such as authorization and token.
    * 
    * @throws Exception
    */
  @Mode(TestMode.LITE)
  @Test
  public void Social_ErrorDiscoveryConfigTests_autoAdjustScopeAndTokenEndptAuthMethod() throws Exception {
     
    genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_adjustDefaults.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
    WebClient webClient = getAndSaveWebClient();

    SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
    updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_autoAdjust");

    List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the tokenEndpointAuthMethod was auto adjusted", "CWWKS6109I.*client_secret_basic");
    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that the scope was auto adjusted", "CWWKS6109I.*openid");
    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery completed successfully", "CWWKS6110I");
    
    genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);

  }
  /**
   * Verify that when discovery is configured but there is no SSL trust, that a 403 forbidden error occurs during discovery and
   * messages are logged so the administrator can diagnose and fix the problem.
   * 
   * The messages should be logged to indicate that there was a failure in discovery.
   * 
   * FFDC with SSL Handshake error.
   * 
   * CWWKS6115E: A successful response was not returned from the URL of [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration].
   *   The social client encountered [IOException: java.security.cert.CertificateException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
   *   unable to find valid certification path to requested target java.security.cert.CertificateException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: 
   *   unable to find valid certification path to requested target] error and failed to access the OpenID Connect provider discovery endpoint.
   *
   * CWWKS5391E: The OpenID Connect client [oidcLogin_badTrust] failed to obtain OpenID Connect provider endpoint information through the discovery endpoint URL of
   * [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration].
   * Update the configuration for the Social Login (oidcLogin configuration) with the correct HTTPS discovery endpoint URL.
   *
   * @throws Exception
   */
  @Test
  @ExpectedFFDC({ "javax.net.ssl.SSLHandshakeException" })
  @AllowedFFDC({ "com.ibm.security.cert.IBMCertPathBuilderException","java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.ws.security.social.error.SocialLoginException" })

  public void Social_ErrorDiscoveryConfigTests_badSSLTrust() throws Exception {

      genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_badTrust.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
      WebClient webClient = getAndSaveWebClient();

      SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();

      updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_badTrust");

      List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
      expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.FORBIDDEN_STATUS);
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating there was a Handshake Exception", SocialMessageConstants.CWPKI0823E_HANDSHAKE_EXCEPTION + ".*SSL HANDSHAKE FAILURE.*" + provider);
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery endpoint response was not successful", "CWWKS6115E");
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5391E");
     
      genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
  }
  
  /**
   * Verify that when social login is configured with an error in the discovery endpoint configuration (mal-formed endpoint), that a dynamic update to the configuration to correct
   * the endpoint results in a successful discovery and successful access to the requested resource.
   *
   * After reconfig with the error config the expected message is:
   * CWWKS5479E: The configuration attribute [authorizationEndpoint] that is required in the social login configuration [oidcLogin1] is missing or empty. 
   * Verify that the attribute is configured, that it is not empty, and that it does not consist of only white space characters.
   * 
   * Then after reconfig with a good discovery config, we receive the expected message below and access to the protected resource is successful.
   * CWWKS6110I: The social login client [oidcLogin1] configuration has been established with the information from the discovery endpoint URL [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. 
   * This information enables the client to interact with the OpenID Connect provider to process the requests such as authorization and token.
   * 
   * @throws Exception
   */
  @Mode(TestMode.LITE)
  @Test
  public void Social_ErrorDiscoveryConfigTests_dynamicUpdateBadDiscoveryConfigToGoodDiscoveryConfig() throws Exception {

      // Start with a discovery endpoint which contains an error so that discovery fails with an error message.
      genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_malFormedUrl.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

      WebClient webClient = getAndSaveWebClient();

      SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
      updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
      
      List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
      expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS6114E.*404");
      expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5391E");

      genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
      
      // Dynamically update the server to correct the discovery configuration so a valid endpoint is configured. Expect a message that discovery completed successfully and 
      // access to the protected resource succeeds.
      
      genericTestServer.reconfigServer("server_LibertyOP_minimalConfig_oidc_usingSocialDiscoveryConfig.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
      
      webClient = getAndSaveWebClient();
      List<validationData> expectations2 = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
      expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating discovery was processed successfully", "CWWKS6110I.*oidcLogin1");

      genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations2);
  }
  
  /**
   * Verify that when social login is configured with an error in the discovery endpoint configuration (mal-formed endpoint), that a dynamic update to the configuration with
   * explicitly configured endpoints results in successful access to the requested resource.
   *
   * After reconfig with the error config the expected message is:
   * CWWKS5479E: The configuration attribute [authorizationEndpoint] that is required in the social login configuration [oidcLogin1] is missing or empty. 
   * Verify that the attribute is configured, that it is not empty, and that it does not consist of only white space characters.
   * 
   * Then after reconfig with a good config with explicitly configured endpoints, the resource can be accessed successfully.
   * 
   * @throws Exception
   */
  @Test
  public void Social_ErrorDiscoveryConfigTests_dynamicUpdateBadDiscoveryConfigToGoodEndpointConfig() throws Exception {

      // Start with a discovery endpoint which contains an error so that discovery fails with an error message.
      genericTestServer.reconfigServer("server_LibertyOP_errorDiscoveryTests_oidc_nonHttpsUrl.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

      WebClient webClient = getAndSaveWebClient();

      SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
      updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
      
      List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE, inovke_social_login_actions);
    expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.UNAUTHORIZED_STATUS);
    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating that discovery failed", "CWWKS5391E");

      genericSocial(_testName, webClient, SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
      
      // Dynamically update the social config with explicitly configured endpoints and access protected resource.
      
      genericTestServer.reconfigServer("server_LibertyOP_configuredEndpoints_oidc_usingSocialConfig.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
      
      webClient = getAndSaveWebClient();
      List<validationData> expectations2 = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);

      genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations2);
  }
  
  /**
   * Verify that when social login is configured with explicitly declared endpoints, that a dynamic update to change the configuration to discovered endpoints
   * results in a successful discovery (with discovery message) and successful access to the requested resource.
   *
   * With configured endpoints, the resource is accessed successfully.
   * 
   * Then after reconfig with discovvered endpoints, we receive the expected message below and access to the protected resource is successful.
   * CWWKS6110I: The social login client [oidcLogin1] configuration has been established with the information from the discovery endpoint URL [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. 
   * This information enables the client to interact with the OpenID Connect provider to process the requests such as authorization and token.
   * 
   * @throws Exception
   */
  @Test
  public void Social_ErrorDiscoveryConfigTests_dynamicUpdateExplicitlyConfiguredEndpointsToDiscoveredEndpoints() throws Exception {

      // Start with a config that has explicitly configured endpoints and successful access to protected resource.
      genericTestServer.reconfigServer("server_LibertyOP_configuredEndpoints_oidc_usingSocialConfig.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

      WebClient webClient = getAndSaveWebClient();

      SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
      updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
      
      List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

      genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);
      
      // Dynamically update the server to use discovery with valid discovery endpoint. Expect a message that discovery completed successfully and 
      // access to the protected resource succeeds.
      
      genericTestServer.reconfigServer("server_LibertyOP_minimalConfig_oidc_usingSocialDiscoveryConfig.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
      
      webClient = getAndSaveWebClient();
      List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
      expectations2 = validationTools.addMessageExpectation(genericTestServer, expectations2, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating discovery was processed successfully", "CWWKS6110I.*oidcLogin1");

      genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations2);
  }
/**
* Verify that when social login is configured with a good discovery endpoint, that a dynamic update to change to explicitly
* configured endpoints results in a successful access to a protected resource.
*
* With good discovery endpoint, a message is logged indicated discovery succeeded and the resource is accessed successfully.
*  CWWKS6110I: The social login client [oidcLogin1] configuration has been established with the information from the discovery endpoint URL [https://localhost:8947/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration]. 
*  This information enables the client to interact with the OpenID Connect provider to process the requests such as authorization and token.
* 
* Then after reconfig to configured endpoints, the resource is accessed successfully.
* 
* @throws Exception
*/
@Test
public void Social_ErrorDiscoveryConfigTests_dynamicUpdateDiscoveredEndpointsToConfiguredEndpoints() throws Exception {

   // Start with a config that has discovered endpoints, verify that discovery was successful and protected resource is accessed.
   genericTestServer.reconfigServer("server_LibertyOP_minimalConfig_oidc_usingSocialDiscoveryConfig.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

   WebClient webClient = getAndSaveWebClient();

   SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
   updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
   
   List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
   expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating discovery was processed successfully", "CWWKS6110I.*oidcLogin1");

   genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);
   
   // Dynamically update the server to use configured endpoints and verify that the protected resource can be accessed successfully.
   genericTestServer.reconfigServer("server_LibertyOP_configuredEndpoints_oidc_usingSocialConfig.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
  
   webClient = getAndSaveWebClient();
   List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

   genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations2);
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
