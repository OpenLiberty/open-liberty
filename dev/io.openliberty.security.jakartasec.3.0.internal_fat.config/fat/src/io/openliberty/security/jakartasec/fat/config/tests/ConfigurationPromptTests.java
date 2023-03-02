/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.config.tests;

import java.util.List;
import java.util.StringJoiner;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition prompt and promptExpression
 *
 * This class contains tests to validate that the prompt param is correctly added to the auth endpoint request.
 * Tests are included for no prompts, a single prompt, and multiple prompts.
 * Additionally, tests are included to test cases where an error is return from the OP
 * and that promptExpression takes precedence over prompt.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigurationPromptTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationPromptTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.prompt")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    // create repeats for opaque and jwt tokens - in lite mode, only run with opaque tokens
    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats(TestMode.LITE, Constants.OPAQUE_TOKEN_FORMAT);

    private static String LOGIN_REQUIRED = "login_required";
    private static String CONSENT_REQUIRED = "consent_required";

    private static String CONSENT_ALLOW_AND_REMEMBER = "Allow, remember my decision";

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_prompt.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps(); // run this after starting the RP so we have the rp port to update the openIdConfig.properties file within the apps

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);

        swh.defaultDropinApp(rpServer, "PromptNone.war", "oidc.client.promptNone.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "PromptLogin.war", "oidc.client.promptLogin.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "PromptConsent.war", "oidc.client.promptConsent.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "PromptSelectAccount.war", "oidc.client.promptSelectAccount.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "PromptLoginConsent.war", "oidc.client.promptLoginConsent.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "PromptEmpty.war", "oidc.client.promptEmpty.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "PromptELNone.war", "PromptEL.war", TestConfigMaps.getOP2(),
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELNone", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionNone()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELNoneLoginRequired.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELNoneLoginRequired", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionNone()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELNoneConsentRequired.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELNoneConsentRequired", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionNone()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELLogin.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELLogin", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionLogin()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELConsent.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELConsent", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionConsent()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELSelectAccount.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELSelectAccount", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionSelectAccount()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELLoginConsent.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELLoginConsent", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionLoginAndConsent()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELEmpty.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELEmpty", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionEmpty()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptELDuplicates.war", "PromptEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptELDuplicates", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionDuplicates()),
                                       "oidc.client.promptEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "PromptLoginPromptELConsent.war", "PromptLoginWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "PromptLoginPromptELConsent", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getPromptExpressionConsent()),
                                       "oidc.client.promptLoginWithEL.servlets", "oidc.client.base.*");

    }

    private void runGoodEndToEndTestWithPromptCheck(String appRoot, String app, PromptType... expectedPromptTypes) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        webClient.getOptions().setRedirectEnabled(false);

        StringJoiner joiner = new StringJoiner("\\+");
        for (PromptType promptType : expectedPromptTypes) {
            joiner.add(promptType.toString().toLowerCase());
        }
        String prompt = joiner.toString();

        boolean containsNonePrompt = prompt.contains(PromptType.NONE.toString().toLowerCase());

        Page response = invokeAuthEndpointAndValidatePrompt(webClient, appRoot, app, containsNonePrompt, prompt);

        webClient.getOptions().setRedirectEnabled(true);

        String redirect = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
        response = actions.invokeUrl(_testName, webClient, redirect);

        if (!containsNonePrompt) {
            response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
            response = actions.doConsent(response, CONSENT_ALLOW_AND_REMEMBER);
        }

        validationUtils.validateResult(response, getGeneralAppExpecations(app));

    }

    public void runBadEndToEndTestWithPromptNone(String appRoot, String app, boolean useBasicAuth, String error) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        webClient.getOptions().setRedirectEnabled(false);

        String prompt = PromptType.NONE.toString().toLowerCase();

        Page response = invokeAuthEndpointAndValidatePrompt(webClient, appRoot, app, useBasicAuth, prompt);

        String callback = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
        response = actions.invokeUrl(_testName, webClient, callback);

        Expectations errorExpectations = new Expectations();
        errorExpectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        errorExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        errorExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER + ".*\\[" + error
                                                                                + "]", "Did not receive an error message stating that the callback url includes the [" + error
                                                                                       + "] error param."));
        validationUtils.validateResult(response, errorExpectations);

    }

    private Page invokeAuthEndpointAndValidatePrompt(WebClient webClient, String appRoot, String app, boolean useBasicAuth, String promptString) throws Exception {

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeApp(webClient, url);

        String authEndpoint = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
        if (useBasicAuth) {
            response = actions.invokeUrlWithBasicAuth(_testName, webClient, authEndpoint, Constants.TESTUSER, Constants.TESTUSERPWD);
        } else {
            response = actions.invokeUrl(_testName, webClient, authEndpoint);
        }

        String checkType = Constants.STRING_MATCHES;
        String failureMessage = "Did not find the correct prompt in authorization endpoint request.";
        if (promptString.isEmpty()) {
            promptString = ".*";
            checkType = Constants.STRING_DOES_NOT_MATCH;
            failureMessage = "Found the prompt param in the authorization endpoint request, but should not have.";
        }
        String authEndpointPromptRegex = "https:\\/\\/localhost:" + opServer.getBvtSecurePort() + "\\/oidc\\/endpoint\\/OP[0-9]*\\/authorize\\?.*prompt=" + promptString + "(&|$)";

        Expectations authExpectations = new Expectations();
        authExpectations.addFoundStatusCodeAndMessageForCurrentAction();
        authExpectations.addExpectation(new ResponseUrlExpectation(checkType, authEndpointPromptRegex, failureMessage));
        validationUtils.validateResult(response, authExpectations);

        return response;
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     *
     * Tests with prompt=none with basic auth and auto-authorize
     * The auth endpoint request should contain the 'prompt=none' param and the OP should not display any login and consent pages.
     * The user should be logged in via basic auth and consent should be auto authorized.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationPromptTests_prompt_none() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptNone", "PromptNoneServlet", PromptType.NONE);

    }

    /**
     *
     * Tests with prompt=login
     * The auth endpoint request should contain the 'prompt=login' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationPromptTests_prompt_login() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptLogin", "PromptLoginServlet", PromptType.LOGIN);

    }

    /**
     *
     * Tests with prompt=consent
     * The auth endpoint request should contain the 'prompt=consent' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationPromptTests_prompt_consent() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptConsent", "PromptConsentServlet", PromptType.CONSENT);

    }

    /**
     *
     * Tests with prompt=select_account
     * The auth endpoint request should contain the 'prompt=select_account' param and the OP should display the login and consent pages.
     * The OP being used does not support prompt=select_account, so the OP won't prompt to select an account.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationPromptTests_prompt_selectAccount() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptSelectAccount", "PromptSelectAccountServlet", PromptType.SELECT_ACCOUNT);

    }

    /**
     *
     * Tests with prompt=[login, consent]
     * The auth endpoint request should contain the 'prompt=login+consent' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationPromptTests_prompt_loginAndConsent() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptLoginConsent", "PromptLoginConsentServlet", new PromptType[] { PromptType.LOGIN, PromptType.CONSENT });

    }

    /**
     *
     * Tests with prompt=[]
     * The auth endpoint request should not contain the prompt param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_prompt_empty() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptEmpty", "PromptEmptyServlet", new PromptType[] {});

    }

    /**
     *
     * Tests with promptExpression=none
     * The auth endpoint request should contain the 'prompt=none' param and the OP should not display the login and consent pages.
     * The user should be logged in via basic auth and consent should be auto authorized.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_none() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELNone", "PromptELServlet", PromptType.NONE);

    }

    /**
     *
     * Tests with promptExpression=none without basic auth
     * The auth endpoint request should contain the 'prompt=none' param and the OP should try to display the login page.
     * This should result in a 'login_required' error from the OP, since the OP cannot display a login page with prompt=none.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20Exception", "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException" })
    @Test
    public void ConfigurationPromptTests_promptEL_none_loginRequired() throws Exception {

        runBadEndToEndTestWithPromptNone("PromptELNoneLoginRequired", "PromptELServlet", false, LOGIN_REQUIRED);

    }

    /**
     *
     * Tests with promptExpression=none with basic auth without auto-authorize
     * The auth endpoint request should contain the 'prompt=none' param and the OP should not display the login page, but should try to display the consent page
     * This should result in a 'consent_required' error from the OP, since the OP cannot display a consent with prompt=none.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20Exception", "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException" })
    @Test
    public void ConfigurationPromptTests_promptEL_none_consentRequired() throws Exception {

        runBadEndToEndTestWithPromptNone("PromptELNoneConsentRequired", "PromptELServlet", true, CONSENT_REQUIRED);

    }

    /**
     *
     * Tests with promptExpression=login
     * The auth endpoint request should contain the 'prompt=login' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_login() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELLogin", "PromptELServlet", PromptType.LOGIN);

    }

    /**
     *
     * Tests with promptExpression=consent
     * The auth endpoint request should contain the 'prompt=consent' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_consent() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELConsent", "PromptELServlet", PromptType.CONSENT);

    }

    /**
     *
     * Tests with promptExpression=select_account
     * The auth endpoint request should contain the 'prompt=select_account' param and the OP should display the login and consent pages.
     * The OP being used does not support prompt=select_account, so the OP won't prompt to select an account.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_selectAccount() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELSelectAccount", "PromptELServlet", PromptType.SELECT_ACCOUNT);

    }

    /**
     *
     * Tests with promptExpression=[login, consent]
     * The auth endpoint request should contain the 'prompt=login+consent' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_loginAndConsent() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELLoginConsent", "PromptELServlet", new PromptType[] { PromptType.LOGIN, PromptType.CONSENT });

    }

    /**
     *
     * Tests with promptExpression=[]
     * The auth endpoint request should not contain the prompt param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_empty() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELEmpty", "PromptELServlet", new PromptType[] {});

    }

    /**
     *
     * Tests with promptExpression=[login, login]
     * The auth endpoint request should contain the 'prompt=login+login' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_promptEL_duplicates() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptELDuplicates", "PromptELServlet", new PromptType[] { PromptType.LOGIN, PromptType.LOGIN });

    }

    /**
     *
     * Tests with prompt=login and promptExpression=consent
     * promptExpression should take precedence over prompt
     * The auth endpoint request should contain the 'prompt=consent' param and the OP should display the login and consent pages.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationPromptTests_prompt_login_promptEL_consent() throws Exception {

        runGoodEndToEndTestWithPromptCheck("PromptLoginPromptELConsent", "PromptLoginWithELServlet", PromptType.CONSENT);

    }

}
