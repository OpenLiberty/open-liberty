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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Utils;
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
public class CustomSelectionPageTests extends CommonMultiProviderLocalAuthenticationTests {

    public static Class<?> thisClass = CustomSelectionPageTests.class;

    public static final String CUSTOM_PAGE_TITLE = "Custom Selection Page";
    public static final String CUSTOM_PAGE_WITH_LOCAL_AUTH_TITLE = "Custom Selection Page";
    public static final String CUSTOM_SELECTION_SERVLET_PATH = "/customSelection/select";
    public static final String CUSTOM_SELECTION_WITH_LOCAL_AUTH_SERVLET_PATH = "/customSelection/localAuth";
    protected static final String DOES_NOT_EXIST_PATH = "/does/not/exist";

    static String classLoginStyle = null;
    static String providerId = null;
    static String providerDisplayName = null;

    @BeforeClass
    public static void setUp() throws Exception {
        classOverrideValidationEndpointValue = SocialConstants.USERINFO_ENDPOINT;

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        createAndStartRpServer();
        createAndStartOpServer();

        setGenericVSSpeicificProviderFlags(GenericConfig);
        chooseLoginStyle();

    }

    static void createAndStartRpServer() throws Exception {
        List<String> rpStartMsgs = new ArrayList<String>();
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.JWT_DEFAULT_CONTEXT_ROOT);
        rpStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.HELLOWORLD_SERVLET);

        List<String> rpApps = new ArrayList<String>();
        rpApps.add(SocialConstants.HELLOWORLD_SERVLET);

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.social", "server_customSelection.xml", SocialConstants.GENERIC_SERVER, rpApps, SocialConstants.DO_NOT_USE_DERBY, rpStartMsgs);

        // Each test will have to reconfigure, so don't bother restoring in between tests
        genericTestServer.setRestoreServerBetweenTests(false);
    }

    static void createAndStartOpServer() throws Exception {
        List<String> opStartMsgs = new ArrayList<String>();
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OAUTH_ROOT);
        opStartMsgs.add(SocialMessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + SocialConstants.OIDC_DEFAULT_CONTEXT_ROOT);

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP);
    }

    static void chooseLoginStyle() {
        // Randomly choose whether we'll use the OAuth or OIDC login flow
        //        classLoginStyle = Utils.getRandomSelection(SocialConstants.OAUTH_OP, SocialConstants.OIDC_OP);
        classLoginStyle = Utils.getRandomSelection(SocialConstants.OIDC_OP, SocialConstants.OIDC_OP);
        setProviderForLoginStyle();
    }

    static void setProviderForLoginStyle() {
        // Create a map of all available social media providers with valid configurations for this login style
        if (SocialConstants.OAUTH_OP.equals(classLoginStyle)) {
            setProviderForOAuth();
        } else {
            setProviderForOidc();
        }
    }

    static void setProviderForOAuth() {
        providerId = "oauth2Login1";
        providerDisplayName = "oauth2Login1";
    }

    static void setProviderForOidc() {
        providerId = "oidcLogin1";
        providerDisplayName = "oidcLogin";
    }

    /*************************************************** Tests ***************************************************/

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: ""
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - The default selection page should be displayed since the custom page is not set.
     * - The full authentication flow should be successful.
     */
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_empty() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_empty.xml", _testName);

        // Empty selection page URL should be ignored by the config, so the default selection page will be used
        runSuccessfulDefaultSelectionPageTest();
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: " " (just white space)
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - The default selection page should be displayed since the custom page is not a valid URL.
     * - The full authentication flow should be successful.
     */
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_whitespace() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_whitespace.xml", _testName);

        // Whitespace-only selection page URL should be ignored by the config, so the default selection page will be used
        runSuccessfulDefaultSelectionPageTest();
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Some invalid URL format
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - The default selection page should be displayed since the custom page is not a valid URL.
     * - Warning message should be emitted saying the custom URL was not valid and that the default selection page will be used
     * instead.
     * - The full authentication flow should be successful.
     */
    @Mode(TestMode.LITE)
    @ExpectedFFDC({ "java.security.PrivilegedActionException" })
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_invalid() throws Exception {

        List<String> startMsgs = Arrays.asList(SocialMessageConstants.CWWKS5430W_SELECTION_PAGE_URL_NOT_VALID);

        genericTestServer.reconfigServer("server_customSelection_invalid.xml", _testName, SocialConstants.JUNIT_REPORTING, startMsgs);

        for (String msg : startMsgs) {
            genericTestServer.addIgnoredServerException(msg);
        }

        // Invalid selection page URL should issue error message and not be used; the default selection page will be used
        runSuccessfulDefaultSelectionPageTest();
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - The custom selection page should be displayed.
     * - The full authentication flow should be successful.
     */
    @Mode(TestMode.LITE)
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_relativeUrl() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_relativeUrl.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForCustomSelectionTests();

        runSuccessfulCustomSelectionPageSocialLoginTest(updatedSocialTestSettings);
    }

    // TODO - relative URL that does not start with "/"

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to non-existent web page
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - Should get a 404 when redirected to the non-existent page.
     */
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_relativeUrl_doesNotExist() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_relativeUrl_doesNotExist.xml", _testName);

        runNonExistentCustomPageTest();
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Absolute URL to valid custom selection page hosted on the Liberty server
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - The custom selection page should be displayed.
     * - The full authentication flow should be successful.
     */
    @Mode(TestMode.LITE)
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_absoluteUrl() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_absoluteUrl.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForCustomSelectionTests();

        runSuccessfulCustomSelectionPageSocialLoginTest(updatedSocialTestSettings);
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Absolute URL to non-existent web page
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     *
     * Expected Results:
     * - Should get a 404 when redirected to the non-existent page.
     */
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_absoluteUrl_doesNotExist() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_absoluteUrl_doesNotExist.xml", _testName);

        runNonExistentCustomPageTest();
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Absolute URL to valid custom selection page hosted somewhere external to the Liberty server
     * - Invoke the protected resource.
     *
     * Expected Results:
     * - Should be redirected to the external page.
     * - External page isn't a real selection page, so we just verify that we were properly redirected.
     */
    @Test
    public void CustomSelectionPageTests_selectionPageUrl_absoluteUrl_remoteLocation() throws Exception {

        // Simply verify that we were properly redirected to the remote location
        final String remoteUrl = "http://www.example.com";

        if (helpers.pingExternalServer(_testName, remoteUrl, "Example Domain", 30)) {

            genericTestServer.reconfigServer("server_customSelection_absoluteUrl_remoteLocation.xml", _testName);

            SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForCustomSelectionTests();

            List<validationData> expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);

            expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not find the expected custom social media selection page URL.", null, remoteUrl);

            genericSocial(_testName, getAndSaveWebClient(), SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
        } else {
            testSkipped();
        }
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Local authentication: Disabled
     * - Custom selection page offers username/password as a log in option.
     * - Follow the usual protected resource invocation and log in using valid user credentials.
     *
     * Expected Results:
     * - The custom selection page should be displayed with username and password inputs.
     * - The full authentication flow should be successful.
     */
    @Test
    public void CustomSelectionPageTests_usesLocalAuthentication_localAuthenticationDisabled() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_withLocalAuth_localAuthenticationDisabled.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = getSuccessfulCustomSelectionPageWithLocalAuthenticationFlowExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Local authentication: Enabled
     * - No user registry configured.
     * - Custom selection page offers username/password as a log in option.
     * - Follow the usual protected resource invocation and log in using user credentials.
     *
     * Expected Results:
     * - The custom selection page should be displayed with username and password inputs.
     * - The full authentication flow should fail because a registry does not exist.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.registry.RegistryException" })
    @Test
    public void CustomSelectionPageTests_usesLocalAuthentication_noRegistry() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_withLocalAuth_noRegistry.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = set401LocalLoginExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying a user registry was not found.", SocialMessageConstants.CWIMK0011E_MISSING_REGISTRY_DEFINITION);

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Local authentication: Enabled
     * - Custom selection page offers username/password as a log in option.
     * - Follow the usual protected resource invocation and log in using user credentials.
     * - Enter a valid username and password on the custom selection page, but for a user who is not authorized to access the
     * resource.
     *
     * Expected Results:
     * - The custom selection page should be displayed with username and password inputs.
     * - The full authentication flow should fail because the user is not authorized.
     */
    @Test
    public void CustomSelectionPageTests_usesLocalAuthentication_userNotAuthorized() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_withLocalAuth_noAppRoles.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = set403LocalLoginExpectations(updatedSocialTestSettings);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.PERFORM_CREDENTIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_MATCHES, "Did not find message in log saying authorization failed for the user.", SocialMessageConstants.CWWKS9104A_AUTHORIZATION_FAILED + ".+" + updatedSocialTestSettings.getUserName());

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Local authentication: Enabled
     * - Custom selection page offers username/password as a log in option.
     * - Follow the usual resource invocation flow.
     * - Enter a valid username and bad password on the custom selection page.
     *
     * Expected Results:
     * - The custom selection page should be displayed with username and password inputs.
     * - The full authentication flow should fail because the password is incorrect.
     */
    @Test
    public void CustomSelectionPageTests_usesLocalAuthentication_badCredentials() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_withLocalAuth.xml", _testName);

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
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Local authentication: Enabled
     * - Custom selection page offers username/password as a log in option.
     * - Follow the usual resource invocation flow.
     * - Enter a valid username and password on the custom selection page.
     *
     * Expected Results:
     * - The custom selection page should be displayed with username and password inputs.
     * - The full authentication flow should be successful.
     */
    @Mode(TestMode.LITE)
    @Test
    public void CustomSelectionPageTests_usesLocalAuthentication_validCredentials() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_withLocalAuth.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings = updateSettingsForLocalAuthentication(updatedSocialTestSettings);

        List<validationData> expectations = getSuccessfulCustomSelectionPageWithLocalAuthenticationFlowExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), ACTIONS_WITH_LOCAL_AUTHENTICATION, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * - socialMediaSelectionPageUrl: Relative URL to valid custom selection page hosted on the Liberty server
     * - Follow the usual protected resource invocation and social login flow using a Liberty OP.
     * - Include request parameters in the initial protected resource invocation.
     *
     * Expected Results:
     * - The custom selection page should be displayed.
     * - The full authentication flow should be successful.
     * - The original request parameters should be present in the final protected resource request URL.
     */
    @Test
    public void CustomSelectionPageTests_withRequestParameters() throws Exception {

        genericTestServer.reconfigServer("server_customSelection_relativeUrl.xml", _testName);

        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForCustomSelectionTests();

        Map<String, List<String>> paramMap = getRequestParameters();

        updatedSocialTestSettings = addParametersToProtectedResourceUrl(updatedSocialTestSettings, paramMap);

        String lastAction = invoke_social_login_actions[invoke_social_login_actions.length - 1];

        List<validationData> expectations = getSuccessfulCustomSelectionPageWithSocialLoginFlowExpectations(updatedSocialTestSettings);
        expectations = addQueryParameterExpectations(expectations, paramMap, lastAction);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /*************************************************** Helper methods ***************************************************/

    /**
     * Performs the usual authentication flow with a Liberty OP provider. Verifies that the default social media selection page is
     * encountered instead of the custom selection page.
     */
    void runSuccessfulDefaultSelectionPageTest() throws Exception {
        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForCustomSelectionTests();

        List<validationData> expectations = getSuccessfulDefaultSelectionPageExpectations(updatedSocialTestSettings);

        genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Performs the entire authentication flow with the selected provider. Verifies that the custom selection page is reached and
     * that all expected values appear on it.
     */
    void runSuccessfulCustomSelectionPageSocialLoginTest(SocialTestSettings settings) throws Exception {
        settings = updateSettingsWithNewProvider(settings, providerId, providerDisplayName);

        List<validationData> expectations = getSuccessfulCustomSelectionPageWithSocialLoginFlowExpectations(settings);

        runSocialLoginFlowWithProvider(settings, providerId, expectations);
    }

    /**
     * Updates the appropriate buttons to press based on the specified Liberty OP provider ID and display name.
     */
    SocialTestSettings updateSettingsWithNewProvider(SocialTestSettings settings, String id, String displayName) {
        Log.info(thisClass, _testName, "Overriding chosen provider with: " + id + " (display value: " + displayName + ")");
        settings.setProviderButton(id);
        settings.setProviderButtonDisplay(displayName);

        return settings;
    }

    /**
     * Runs the standard social login flow (invoke protected resource, pick a provider on the selection page, log in, get
     * redirected back to protected resource) with the specified provider.
     */
    void runSocialLoginFlowWithProvider(SocialTestSettings settings, String providerName, List<validationData> expectations) throws Exception {
        try {
            Log.info(thisClass, _testName, "Beginning login flow with provider: " + providerName);

            genericSocial(_testName, getAndSaveWebClient(), invoke_social_login_actions, settings, expectations);

        } catch (Exception e) {
            throw new Exception("Error encountered authenticating with Liberty OP provider [" + providerName + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Invokes the protected resource expecting to be redirected to a non-existent web page. Verifies that we receive a 404 from
     * the expected URL.
     */
    void runNonExistentCustomPageTest() throws Exception {
        SocialTestSettings updatedSocialTestSettings = getUpdatedSettingsForCustomSelectionTests();

        // Make sure we were redirected to the custom selection page and got a 404
        List<validationData> expectations = vData.addResponseStatusExpectation(null, SocialConstants.INVOKE_SOCIAL_RESOURCE, HttpServletResponse.SC_NOT_FOUND);

        String customUrl = genericTestServer.getHttpsString() + DOES_NOT_EXIST_PATH;
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not find the expected custom social media selection page URL.", null, customUrl);

        genericSocial(_testName, getAndSaveWebClient(), SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY, updatedSocialTestSettings, expectations);
    }

    /**
     * Sets the actions to be performed for a Liberty OP provider, updates test settings to use Liberty OP provider values, and
     * randomly chooses a valid Liberty OP provider to use for authentication.
     */
    SocialTestSettings getUpdatedSettingsForCustomSelectionTests() throws Exception {
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();

        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, classLoginStyle, true);

        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings, genericTestServer);

        updatedSocialTestSettings = updateButtonSettingsForProvider(updatedSocialTestSettings);

        return updatedSocialTestSettings;
    }

    /**
     * Updates the button and button display values in the provided settings object for the Liberty OP provider corresponding to
     * this run's login type (e.g. OAuth or OIDC).This will be the provider button that is clicked on the social media selection
     * page.
     */
    SocialTestSettings updateButtonSettingsForProvider(SocialTestSettings settings) {
        settings.setProviderButton(providerId);
        settings.setProviderButtonDisplay(providerDisplayName);
        return settings;
    }

    /**
     * Sets expectations that we successfully go through the entire social login flow and that the expected provider button
     * appears on the default social media selection page.
     */
    List<validationData> getSuccessfulDefaultSelectionPageExpectations(SocialTestSettings settings) throws Exception {
        List<validationData> expectations = setGoodSocialExpectations(settings, doNotAddJWTTokenValidation);
        // Expect local authentication to be disabled for these tests
        expectations = setLocalAuthenticationDisabledExpectations(expectations);
        expectations = setDefaultSelectionPageExpectations(expectations, settings);
        // Make sure we find the button for the provider we're using in this test
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not find the required provider button on the default selection page.", null, "<button [^>]+>" + settings.getProviderButtonDisplay() + "</button>");
        return expectations;
    }

    /**
     * Adds expectations that check that we receive all 200 status codes, that local authentication values appear on the selection
     * page after we invoke the protected resource, that we reached the custom selection page, and that we successfully reached
     * the protected resource.
     */
    List<validationData> getSuccessfulCustomSelectionPageWithLocalAuthenticationFlowExpectations(SocialTestSettings settings) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(ACTIONS_WITH_LOCAL_AUTHENTICATION);
        expectations = setLocalAuthenticationEnabledExpectations(expectations);
        expectations = getGoodCustomSelectionWithLocalAuthPageExpectations(expectations, settings);
        expectations = setGoodHelloWorldExpectations(expectations, settings, doNotAddJWTTokenValidation, SocialConstants.PERFORM_CREDENTIAL_LOGIN);
        return expectations;
    }

    /**
     * Adds expectations that check for the correct response URL and page title for the custom social media selection page with
     * local authentication options, and that the page contains the appropriate basic auth form.
     */
    List<validationData> getGoodCustomSelectionWithLocalAuthPageExpectations(List<validationData> expectations, SocialTestSettings settings) throws Exception {
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not find the expected URL for the custom social media selection page with local authentication.", null, genericTestServer.getHttpsString() + CUSTOM_SELECTION_WITH_LOCAL_AUTH_SERVLET_PATH);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Did not find the expected title for the custom social media selection page with local authentication.", null, CUSTOM_PAGE_WITH_LOCAL_AUTH_TITLE);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Custom page's form action did not contain a form with the action \"j_security_check\".", null, "<form action=\"j_security_check\"");
        return expectations;
    }

    /**
     * Adds expectations that check that we receive all 200 status codes, that local authentication values do NOT appear on the
     * custom selection page, that we do in fact get to the custom selection page, that the appropriate social login provider
     * information is on the custom selection page, that we successfully reach the login page, and that we ultimately reach the
     * protected resource.
     */
    List<validationData> getSuccessfulCustomSelectionPageWithSocialLoginFlowExpectations(SocialTestSettings settings) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        expectations = setLocalAuthenticationDisabledExpectations(expectations);
        expectations = getGoodCustomSelectionPageForSocialLoginExpectations(expectations, settings);
        expectations = setLoginPageExpectation(expectations, settings, SocialConstants.SELECT_PROVIDER);
        expectations = setGoodHelloWorldExpectations(expectations, settings, doNotAddJWTTokenValidation, perform_social_login);
        return expectations;
    }

    /**
     * Adds expectations that check for the correct response URL and page title for the custom social media selection page, and
     * that the page contains the appropriate form and button content for the social media provider being used for this test.
     */
    List<validationData> getGoodCustomSelectionPageForSocialLoginExpectations(List<validationData> expectations, SocialTestSettings settings) throws Exception {
        // Check that we reached the correct URL and have the expected page title
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_URL, SocialConstants.STRING_CONTAINS, "Did not find the expected custom social media selection page URL.", null, genericTestServer.getHttpsString() + CUSTOM_SELECTION_SERVLET_PATH);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, "Did not find the expected custom social media selection page title.", null, CUSTOM_PAGE_TITLE);

        // Form action should match the initial request URL
        String expectedFormAction = getExpectedFormAction(settings);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, "Custom page's form action did not match the original protected resource request URL.", null, "<form action=\"" + expectedFormAction + "\"");

        // Check that there's a button for the social login provider we're using in this test
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_MATCHES, "Did not find the required provider button on the custom selection page.", null, "<button [^>]+>" + settings.getProviderButtonDisplay() + "</button>");
        return expectations;
    }

    /**
     * Verifies that we successfully reached the custom selection page with local authentication inputs present, but also that we
     * get a 401 when performing the credential login.
     */
    List<validationData> set401LocalLoginExpectations(SocialTestSettings settings) throws Exception {
        return setUnsuccessfulLocalLoginExpectations(settings, HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Verifies that we successfully reached the custom selection page with local authentication inputs present, but also that we
     * get a 403 when performing the credential login.
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

        expectations = setLocalAuthenticationEnabledExpectations(expectations);
        expectations = getGoodCustomSelectionWithLocalAuthPageExpectations(expectations, settings);

        return expectations;
    }

}
