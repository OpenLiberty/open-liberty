/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.commonTests;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.MessageConstants;
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
// some of the ffdc can be delayed and are logged after the test that caused them completes - this causes the test that actually recieves it to fail
// we're checking status codes and error messages, so, we shouldn't have to rely on the ffdcs to validate that we got the correct error.
@AllowedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException", "java.net.NoRouteToHostException", "java.net.SocketException", "java.net.SocketTimeoutException", "java.security.cert.CertPathBuilderException", "org.apache.http.conn.ConnectTimeoutException", "org.apache.http.conn.HttpHostConnectException", "sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
@Mode(TestMode.FULL)
public class Social_BasicConfigTests_NoServerSSL extends SocialCommonTest {

    public static Class<?> thisClass = Social_BasicConfigTests_NoServerSSL.class;
    public static boolean isTestingOidc = false; // can be changed by subclass
    public static boolean isOPOAuth = false;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /**
     * The global SSL config uses the trust store that has the provider, so, adding it to the provider config does little.
     * We just want to show that it doesn't break anything...
     *
     * @throws Exception
     */
    // NOTE: bad global provider trust with good provider specific trust is tested in test class: Social_BasicConfigTests_omittedGlobalTrust
    // Reconfiguring the global trust can be problematic!
    @Test
    @Mode(TestMode.LITE)
    public void Social_BasicConfigTests_NoServerrSSL_goodTrust() throws Exception {

        //        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_noServerSSL_goodTrust.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodTrust");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * This tests the configuration attribute useSystemPropertiesForHttpClientConnections for social oauth clients.
     * All social oauth clients use most of the same code, so one flavor is sufficient.
     *
     * A proxy host and port are defined in jvm.options, but won't take effect until this attribute is set to true.
     * When the attribute is set to true, we expect a failure because the token or userapi retrieval call should
     * be redirected to the non-existent proxy server.
     *
     * Testing the full path would require a proxy server, which the FAT framework does not have, but it has been done manually.
     * For social oidc clients, coverage in the oidcclient bucket covers the same code path.
     *
     */
    @Test
    @Mode(TestMode.LITE)
    public void Social_BasicConfigTests_NoServerrSSL_useJvmProps() throws Exception {
        if (!isOPOAuth) {
            Log.info(thisClass, "info", "not Liberty Oauth provider, skipping test");
            return;
        }

        WebClient webClient = getAndSaveWebClient();
        webClient.getOptions().setTimeout(10000);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_jvmprops_goodTrust");
        String lastStep = perform_social_login;
        String[] steps = invoke_social_login_actions;
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(lastStep, steps);
        expectations = vData.addResponseStatusExpectation(expectations, lastStep, SocialConstants.UNAUTHORIZED_STATUS);
        // Error message may or may not be emitted
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKS5451E_AUTH_CODE_ERROR_GETTING_TOKENS);
        // The call to an unreachable IP address can leave a thread hanging that doesn't get cleaned up and causes some warning messages during shutdown
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKE1102W_QUIESCE_WARNING);
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE);
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD);
        try {
            genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
        } catch (Exception e) {
            if (isAcceptableBadConnectionException(e)) {
                Log.info(thisClass, "info", "Caught any acceptable bad connection exception (" + e + ")");
            } else {
                throw e;
            }
        }
    }

    private boolean isAcceptableBadConnectionException(Exception e) {
        if (e instanceof java.net.SocketException || e instanceof java.net.SocketTimeoutException) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause == null) {
            return false;
        }
        if (cause instanceof java.net.SocketException || cause instanceof java.net.SocketTimeoutException) {
            return true;
        }
        return false;
    }

    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfOAuthStyleProvider.class) // skip test if NOT OIDC style
    public void Social_BasicConfigTests_NoServerrSSL_goodTrust_goodJwksUri() throws Exception {

        // re-enabled the reconfig when/if we can automate google testing
        //        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_noServerSSL_goodTrust.xml", null);

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_goodJwksUri_goodTrust");

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
    @Test
    public void Social_BasicConfigTests_NoServerrSSL_badTrust() throws Exception {

        reconfigIfProviderSpecificConfig(genericTestServer, providerConfigString + "_noServerSSL.xml", null);

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

        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, lastStep, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message saying an error was found making the request.", SocialMessageConstants.CWWKS5424E_TWITTER_RESPONSE_FAILURE + ".+" + SocialMessageConstants.CWWKS5476E_ERROR_MAKING_REQUEST);
        } else {
            expectations = setLoginPageExpectation(expectations, updatedSocialTestSettings, SocialConstants.INVOKE_SOCIAL_RESOURCE);
            if (isTestingOidc) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, lastStep, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message saying an error was found making the request.", SocialMessageConstants.CWWKS1708E_UNABLE_TO_CONTACT_PROVIDER);
            } else {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, lastStep, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message saying an error was found making the request.", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO + ".+" + SocialMessageConstants.CWWKS5476E_ERROR_MAKING_REQUEST);
            }
        }
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, lastStep, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Message log did not contain message indicating there was a Handshake Exception", SocialMessageConstants.CWPKI0823E_HANDSHAKE_EXCEPTION + ".*SSL HANDSHAKE FAILURE.*" + provider);

        genericSocial(_testName, webClient, steps, updatedSocialTestSettings, expectations);
    }

}
