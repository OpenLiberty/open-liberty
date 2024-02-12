/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

package com.ibm.ws.security.backchannelLogout.fat.CommonTests;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.backchannelLogout.fat.utils.AfterLogoutStates;
import com.ibm.ws.security.backchannelLogout.fat.utils.AfterLogoutStates.BCL_FORM;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.backchannelLogout.fat.utils.TokenKeeper;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test class contains tests that validate the proper behavior in end-to-end revocation requests.
 * These tests will focus on the proper revocation behavior based on the OP and OAuth registered client
 * configs.
 **/

@SuppressWarnings("serial")
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
public class RevocationBCLTests extends BackChannelLogoutCommonTests {

    protected static Class<?> thisClass = RevocationBCLTests.class;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /**
     * Repeat tests using OIDC (with a Local Store) or OIDC with SAML OP's, OIDC and Social clients. Use the same tests cases to
     * test the revoke endpoint.
     *
     * @return RepeatTests object for each variation of this class that will be run
     */
    public static RepeatTests createRepeats(String callingProject) {

        // note:  using the method addRepeat below instead of adding test repeats in line to simplify hacking up the tests locally to only run one or 2 variations (all the calls are the same.
        // Don't have to worry about using "with" vs "andWith")
        RepeatTests rTests = null;
        if (callingProject.equals(Constants.OIDC)) {
            rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.REVOCATION_ENDPOINT + "_" + localStore));

        } else {
            if (callingProject.equals(Constants.SOCIAL)) {
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.REVOCATION_ENDPOINT));

            } else {
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.REVOCATION_ENDPOINT));
            }
        }

        return rTests;

    }

    @BeforeClass
    public static void setUp() throws Exception {
        sharedSetUp();
    }

    /********************************************** Tests **********************************************/

    /**
     * Main path revoking the access_token.
     * The config used does have a valid/real bcl endpoint configured.
     * This test just makes sure that only the access_token is cleaned up - make sure that BCL is NOT used in this flow
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void RevocationBCLTests_mainPath() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "bcl_mainPath_confClient", false);

        accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the revoke page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(vSettings.finalAppWithoutPostRedirect), tokens.getAccessToken());

        // Test client config contains the standard backchannelLogoutUri - make sure that we don't use it
        AfterLogoutStates states = new AfterLogoutStates(BCL_FORM.OMITTED, updatedTestSettings, vSettings);

        webClient.getOptions().setJavaScriptEnabled(true);
        // Make sure that all cookies and tokens exist (except for the access_token)
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * The same user logs in using different clients within the same provider.
     * Invoke revoke passing the id_token as the id_token_hint - show that
     * no bcl requests will be made
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void RevocationBCLTests_confirmBCLUriCalledForEachLogin_withIdTokenHint() throws Exception {

        starIt = true;

        clientServer.getServer().initializeAnyExistingMarks();

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);
        WebClient webClient4 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-3", false);
        TestSettings updatedTestSettings4 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-4", false);

        //        Object response1 =
        accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);
        accessProtectedApp(webClient4, updatedTestSettings4);
        TokenKeeper keeper1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(vSettings.finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that the OP endpoints are not being called from the logout test app that calls req.logout(): ", "NOT Invoking provider logout or end_session endpoint on the OP");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, "1");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, "2");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, "3");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings4, "4");
        //        }
        // Logout
        //        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, keeper1.getAccessToken());

    }
}
