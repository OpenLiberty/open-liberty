/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.social.fat.multiProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.multiProvider.commonTests.CommonMultiProviderLocalAuthenticationTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class MultiProvider_LocalAuthentication extends CommonMultiProviderLocalAuthenticationTests {

    public static Class<?> thisClass = MultiProvider_LocalAuthentication.class;

    @BeforeClass
    public static void setUp() throws Exception {
        classOverrideValidationEndpointValue = SocialConstants.USERINFO_ENDPOINT;

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        createAndStartRpServer();
        createAndStartOpServer();

        setGenericVSSpeicificProviderFlags(GenericConfig);
    }

    static void createAndStartRpServer() throws Exception {
        List<String> rpStartMsgs = new ArrayList<String>();
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.JWT_DEFAULT_CONTEXT_ROOT);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.HELLOWORLD_SERVLET);

        List<String> rpApps = new ArrayList<String>();
        rpApps.add(SocialConstants.HELLOWORLD_SERVLET);

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.social", "server_localAuthentication_enabled.xml", SocialConstants.GENERIC_SERVER, rpApps, SocialConstants.DO_NOT_USE_DERBY, rpStartMsgs);
    }

    static void createAndStartOpServer() throws Exception {
        List<String> opStartMsgs = new ArrayList<String>();
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_DEFAULT_CONTEXT_ROOT);

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP);
    }

    /*************************************************** Tests ***************************************************/

    /**
     * Test Purpose:
     * - Local authentication: Not configured (default: disabled)
     * - Follow the usual resource invocation and authentication flow using a Liberty OP.
     *
     * Expected Results:
     * - The default selection page should not display user credentials options since local authentication is disabled by default.
     * - The full authentication flow should be successful.
     */
    @Test
    public void MultiProvider_LocalAuthentication_notConfigured() throws Exception {

        genericTestServer.reconfigServer("server_localAuthentication_notConfigured.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForSocialLogin(updatedSocialTestSettings, SocialConstants.OAUTH_2_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        expectations = setLocalAuthenticationDisabledExpectations(expectations);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Disabled
     * - Follow the usual resource invocation and authentication flow using a Liberty OP.
     *
     * Expected Results:
     * - The default selection page should not display user credentials options since local authentication is disabled.
     * - The full authentication flow should be successful.
     */
    @Test
    public void MultiProvider_LocalAuthentication_disabled() throws Exception {

        genericTestServer.reconfigServer("server_localAuthentication_disabled.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForSocialLogin(updatedSocialTestSettings, SocialConstants.OAUTH_2_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        expectations = setLocalAuthenticationDisabledExpectations(expectations);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation and authentication flow using a Liberty OP.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should be successful.
     */
    @Test
    public void MultiProvider_LocalAuthentication_useSocialLogin() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForSocialLogin(updatedSocialTestSettings, SocialConstants.OAUTH_2_LOGIN, SocialConstants.OAUTH_OP);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        expectations = setLocalAuthenticationEnabledExpectations(expectations);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation flow.
     * - Enter a username and password on the default selection page.
     * - There is no registry configured in the Liberty server.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should fail because a registry does not exist.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void MultiProvider_LocalAuthentication_noRegistry() throws Exception {

        genericTestServer.reconfigServer("server_localAuthentication_enabled_noRegistry.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = set401LocalLoginExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying a user registry was not found.", SocialMessageConstants.CWIMK0011E_MISSING_REGISTRY_DEFINITION);

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation flow.
     * - Enter a valid username and password on the default selection page.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should be successful.
     */
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_LocalAuthentication_validCredentials() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = setSuccessfulFlowExpectations(updatedSocialTestSettings, ACTIONS_WITH_LOCAL_AUTHENTICATION);
        expectations = setLocalAuthenticationEnabledExpectations(expectations);

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation flow.
     * - Enter a username on the default selection page that doesn't exist in the registry.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should fail because the user does not exist.
     */
    @Test
    public void MultiProvider_LocalAuthentication_userNotInRegistry() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        String badUser = "doesNot" + VALID_USERNAME + "Exist";
        updatedSocialTestSettings.setUserName(badUser);

        List<validationData> expectations = set401LocalLoginExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying authentication did not succeed for the bad user.", SocialMessageConstants.CWWKS1100A_LOGIN_FAILED + ".+" + badUser);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying authentication did not succeed because of incorrect credentials.", SocialMessageConstants.CWIML4537E_PRINCIPAL_NOT_FOUND + ".+" + updatedSocialTestSettings.getUserName());

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation flow.
     * - Enter a valid username and bad password on the default selection page.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should fail because the password is incorrect.
     */
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_LocalAuthentication_badPassword() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        String badPassword = "badPassword" + VALID_PASSWORD + "IsBad";
        updatedSocialTestSettings.setUserPassword(badPassword);

        List<validationData> expectations = set401LocalLoginExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying authentication did not succeed because of incorrect credentials.", SocialMessageConstants.CWWKS1100A_LOGIN_FAILED + ".+" + updatedSocialTestSettings.getUserName());
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying authentication did not succeed because of incorrect credentials.", SocialMessageConstants.CWIML4537E_PRINCIPAL_NOT_FOUND + ".+" + updatedSocialTestSettings.getUserName());

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation flow.
     * - Enter a valid username and password on the default selection page, but for a user who is not authorized to access the
     * resource.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should fail because the user is not authorized.
     */
    @Test
    public void MultiProvider_LocalAuthentication_unauthorizedUser() throws Exception {

        genericTestServer.reconfigServer("server_localAuthentication_enabled_noAppRoles.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = set403LocalLoginExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying authorization failed for the user.", SocialMessageConstants.CWWKS9104A_AUTHORIZATION_FAILED + ".+" + updatedSocialTestSettings.getUserName());

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - Local authentication: Enabled
     * - Follow the usual resource invocation flow with request parameters in the initial resource request.
     * - Enter a valid username and password on the default selection page.
     *
     * Expected Results:
     * - The default selection page should display user credentials options since local authentication is enabled.
     * - The full authentication flow should be successful.
     * - The original request parameters should still be present in the final authenticated resource invocation.
     */
    @Test
    public void MultiProvider_LocalAuthentication_withRequestParameters() throws Exception {

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        // Add request parameters to the protected resource URL
        Map<String, List<String>> reqParams = getRequestParameters();
        updatedSocialTestSettings = addParametersToProtectedResourceUrl(updatedSocialTestSettings, reqParams);

        String firstAction = ACTIONS_WITH_LOCAL_AUTHENTICATION[0];
        String lastAction = ACTIONS_WITH_LOCAL_AUTHENTICATION[ACTIONS_WITH_LOCAL_AUTHENTICATION.length - 1];

        List<validationData> expectations = setSuccessfulFlowExpectations(updatedSocialTestSettings, ACTIONS_WITH_LOCAL_AUTHENTICATION);
        expectations = setLocalAuthenticationEnabledExpectations(expectations);
        expectations = addQueryParameterExpectations(expectations, reqParams, firstAction);
        expectations = addQueryParameterExpectations(expectations, reqParams, lastAction);

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /************************************************* Helper methods *************************************************/

    /**
     * Verifies that all status codes are 200s, that we hit the selection page, and that we successfully reached the protected
     * application and created a subject with the correct values.
     */
    List<validationData> setSuccessfulFlowExpectations(SocialTestSettings settings, String[] actions) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(actions);

        expectations = setDefaultSelectionPageExpectations(expectations, settings);

        String finalAction = actions[actions.length - 1];
        expectations = setGoodHelloWorldExpectations(expectations, settings, doNotAddJWTTokenValidation, finalAction);

        return expectations;
    }

    /**
     * Verifies that we successfully reached the default selection page and that the local authentication information is present,
     * but also that we get a 401 when performing the credential login.
     */
    List<validationData> set401LocalLoginExpectations(SocialTestSettings settings) throws Exception {
        return setUnsuccessfulLocalLoginExpectations(settings, HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Verifies that we successfully reached the default selection page and that the local authentication information is present,
     * but also that we get a 403 when performing the credential login.
     */
    List<validationData> set403LocalLoginExpectations(SocialTestSettings settings) throws Exception {
        return setUnsuccessfulLocalLoginExpectations(settings, HttpServletResponse.SC_FORBIDDEN);
    }

    /**
     * Verifies that we successfully reached the default selection page and that the local authentication information is present,
     * but also that we get a the specified response status when performing the credential login.
     */
    List<validationData> setUnsuccessfulLocalLoginExpectations(SocialTestSettings settings, int expectedResponseStatus) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.PERFORM_CREDENTIAL_LOGIN, ACTIONS_WITH_LOCAL_AUTHENTICATION);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, expectedResponseStatus);

        expectations = setDefaultSelectionPageExpectations(expectations, settings);
        expectations = setLocalAuthenticationEnabledExpectations(expectations);

        return expectations;
    }

}
