/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.config.tests;

import static io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers.buildAccessTokenScopeString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition scope and scopeExpression.
 *
 * This class contain tests for the regular scope attribute, tests that ensure that the scopeExpression attribute overrides the scope attribute, and tests some negative cases.
 *
 * In the positive test cases, these tests will validate that the correct scope is being sent to the server, by validating the scopes returned in the access token.
 * Also, in the positive test cases, a mock userinfo endpoint (JsonUserInfoScopeServlet) was created to test that the scopes sent from the client via the access token are correct.
 * The mock userinfo endpoint returns claims based on the scopes it receives in the access token and the client verifies these claims sent back.
 * Only a subset of the profile scope claims are used to be sent back from the mock userinfo endpoint for brevity.
 * In the negative test cases, the normal userinfo endpoint is called to verify the error messages.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigurationScopeTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationScopeTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.scope")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeat(Constants.JWT_TOKEN_FORMAT);

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_scope.xml", waitForMsgs);
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

        // deploy the userinfo endpoint apps
        swh.dropinAppWithJose4j(rpServer, "UserInfo.war", "userinfo.servlets");

        swh.deployConfigurableTestApps(rpServer, "ScopeOpenId.war", "ScopeOpenId.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "ScopeOpenId", "allValues.openIdConfig.properties", new HashMap<>()),
                                       "oidc.client.scopeOpenId.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ScopeOpenIdProfile.war", "ScopeOpenIdProfile.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "ScopeOpenIdProfile", "allValues.openIdConfig.properties", new HashMap<>()),
                                       "oidc.client.scopeOpenIdProfile.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ScopeOpenIdEmail.war", "ScopeOpenIdEmail.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "ScopeOpenIdEmail", "allValues.openIdConfig.properties", new HashMap<>()),
                                       "oidc.client.scopeOpenIdEmail.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "ScopeOpenIdProfileEmail.war", "ScopeOpenIdProfileEmail.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "ScopeOpenIdProfileEmail", "allValues.openIdConfig.properties", new HashMap<>()),
                                       "oidc.client.scopeOpenIdProfileEmail.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "scopeOpenIdProfileEmailELOpenId.war", "ScopeOpenIdProfileEmailWithEL.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "scopeOpenIdProfileEmailELOpenId", "allValues.openIdConfig.properties",
                                                                             TestConfigMaps.getScopeExpressionOpenId()),
                                       "oidc.client.scopeOpenIdProfileEmailWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeOpenIdProfileEmailELOpenIdProfile.war", "ScopeOpenIdProfileEmailWithEL.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "scopeOpenIdProfileEmailELOpenIdProfile", "allValues.openIdConfig.properties",
                                                                             TestConfigMaps.getScopeExpressionOpenIdProfile()),
                                       "oidc.client.scopeOpenIdProfileEmailWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeOpenIdProfileEmailELOpenIdEmail.war", "ScopeOpenIdProfileEmailWithEL.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "scopeOpenIdProfileEmailELOpenIdEmail", "allValues.openIdConfig.properties",
                                                                             TestConfigMaps.getScopeExpressionOpenIdEmail()),
                                       "oidc.client.scopeOpenIdProfileEmailWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeOpenIdProfileEmailELOpenIdProfileEmail.war", "ScopeOpenIdProfileEmailWithEL.war",
                                       buildUpdatedConfigMapWithMockUserInfo(opServer, rpServer, "scopeOpenIdProfileEmailELOpenIdProfileEmail", "allValues.openIdConfig.properties",
                                                                             TestConfigMaps.getScopeExpressionOpenIdProfileEmail()),
                                       "oidc.client.scopeOpenIdProfileEmailWithEL.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "scopeELNoOpenId.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELNoOpenId", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionNoOpenId()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeELEmpty.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELEmpty", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionEmpty()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeELNoScopesInCommonExceptOpenId.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELNoScopesInCommonExceptOpenId", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionNoScopesInCommonExceptOpenId()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeELMoreScopesThanConfiguredOnServer.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELMoreScopesThanConfiguredOnServer", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionMoreScopesThanConfiguredOnServer()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeELUppercaseScopes.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELUppercaseScopes", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionUppercaseScopes()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeELUnknownScope.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELUnknownScope", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionUnknownScope()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "scopeELDuplicateScope.war", "GenericOIDCAuthMechanism.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "scopeELDuplicateScope", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getScopeExpressionDuplicateScope()),
                                       "oidc.client.generic.servlets", "oidc.client.base.*");

    }

    public static Map<String, Object> buildUpdatedConfigMapWithMockUserInfo(LibertyServer opServer, LibertyServer rpServer, String appName, String configFileName,
                                                                            Map<String, Object> overrideConfigSettings) throws Exception {

        Map<String, Object> userInfoConfigMap = TestConfigMaps.getUserInfo(rpHttpsBase, "JsonUserInfoScopeServlet");
        overrideConfigSettings.putAll(userInfoConfigMap);
        return buildUpdatedConfigMap(opServer, rpServer, appName, configFileName, overrideConfigSettings);
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     *
     * Tests with scope = "openid".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openId() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdServlet";
        String url = rpHttpsBase + "/ScopeOpenId/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid profile".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdProfile() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdProfileServlet";
        String url = rpHttpsBase + "/ScopeOpenIdProfile/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid email".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdEmail() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = new String[] { Constants.OPENID_SCOPE, Constants.EMAIL_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdEmailServlet";
        String url = rpHttpsBase + "/ScopeOpenIdEmail/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid profile email".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdProfileEmail() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE, Constants.EMAIL_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdProfileEmailServlet";
        String url = rpHttpsBase + "/ScopeOpenIdProfileEmail/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid profile email" and scopeExpression = "openid".
     * The scope requested by client to the server should be "openid".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdProfileEmail_scopeExpression_openId() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdProfileEmailWithELServlet";
        String url = rpHttpsBase + "/scopeOpenIdProfileEmailELOpenId/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid profile email" and scopeExpression = "openid profile".
     * The scope requested by client to the server should be "openid profile".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdProfileEmail_scopeExpression_openIdProfile() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdProfileEmailWithELServlet";
        String url = rpHttpsBase + "/scopeOpenIdProfileEmailELOpenIdProfile/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid profile email" and scopeExpression = "openid email".
     * The scope requested by client to the server should be "openid email".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdProfileEmail_scopeExpression_openIdEmail() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE, Constants.EMAIL_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdProfileEmailWithELServlet";
        String url = rpHttpsBase + "/scopeOpenIdProfileEmailELOpenIdEmail/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "openid profile email" and scopeExpression = "openid profile email".
     * The scope requested by client to the server should be "openid profile email".
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scope_openIdProfileEmail_scopeExpression_openIdProfileEmail() throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;
        String[] scopes = { Constants.OPENID_SCOPE, Constants.PROFILE_SCOPE, Constants.EMAIL_SCOPE };

        WebClient webClient = getAndSaveWebClient();

        String app = "ScopeOpenIdProfileEmailWithELServlet";
        String url = rpHttpsBase + "/scopeOpenIdProfileEmailELOpenIdProfileEmail/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextAccessTokenScopeExpectations(null, expectations, requester, scopes);
        OpenIdContextExpectationHelpers.getOpenIdContextMockUserInfoExpectations(null, expectations, requester, scopes);

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope without the "openid" scope.
     * This should fail, since OIDC requires the "openid" scope.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void ConfigurationScopeTests_scopeExpression_noOpenId() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELNoOpenId/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2416E_TOKEN_REQUEST_ERROR + ".*" + MessageConstants.CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER + ".*" + "id_token", "Did not receive an error message stating that the id_token parameter is missing from the token response."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scope = "".
     * This should fail, since there is no scope.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    public void ConfigurationScopeTests_scopeExpression_empty() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELEmpty/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0064E_SCOPE_MISMATCH, "Did not receive an error message stating that the scope does not match any of the registered scopes."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with scopes that do not overlap with what is supported by the server, except for "openid".
     * Only the common "openid" scope should be recognized by the server and returned.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scopeExpression_noScopesInCommonExceptOpenId() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELNoScopesInCommonExceptOpenId/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, buildAccessTokenScopeString(requester,
                                                                                                                             Constants.OPENID_SCOPE), "The access token scope claim returned by the server should not have included the scopes that don't overlap with the client."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Tests with more scopes than what is configured by the server.
     * The extra unknown scopes should be ignored by the server.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scopeExpression_moreScopesThanConfiguredOnServer() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELMoreScopesThanConfiguredOnServer/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, buildAccessTokenScopeString(requester,
                                                                                                                             Constants.OPENID_SCOPE,
                                                                                                                             Constants.PROFILE_SCOPE,
                                                                                                                             Constants.EMAIL_SCOPE), "The access token scope claim returned by the server should not have included the extra unknown scope claim specified by the client."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with scope = "OPENID PROFILE EMAIL".
     * This should fail, since the scopes are case sensitive.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    public void ConfigurationScopeTests_scopeExpression_uppercaseScopes() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELUppercaseScopes/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0064E_SCOPE_MISMATCH, "Did not receive an error message stating that the scope does not match any of the registered scopes."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with an unknown scope.
     * This should fail, since the scope is unknown and there is no "openid" scope.
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.AuthenticationResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    public void ConfigurationScopeTests_scopeExpression_unknownScope() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELUnknownScope/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(opServer, MessageConstants.CWOAU0064E_SCOPE_MISMATCH, "Did not receive an error message stating that the scope does not match any of the registered scopes."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     *
     * Test with a duplicate scope.
     * The duplicate scope should be removed.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationScopeTests_scopeExpression_duplicateScope() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;

        String app = "GenericOIDCAuthMechanism";
        String url = rpHttpsBase + "/scopeELDuplicateScope/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, buildAccessTokenScopeString(requester,
                                                                                                                             Constants.OPENID_SCOPE), "The access token scope claim returned by the server should not contains duplicate scopes."));

        validationUtils.validateResult(response, expectations);

    }

}
