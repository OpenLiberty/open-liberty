/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.config.tests;

import static io.openliberty.security.jakartasec.fat.utils.Constants.DEFAULT_TOKEN_MIN_VALIDITY;

import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.CommonExpectations;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShortTokenLifetimePrep;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition tokenMinValidity and tokenMinValidityExpression
 *
 * This class contains tests which tests different tokenMinValidity values such as less than
 * token lifetime, equal token lifetime, greater than token lifetime, and 0. Additionally,
 * ensures that tokenMinValidityExpression takes precedence over tokenMinValidity and that the default
 * value of 10 * 1000 is used if no tokenMinValidity nor tokenMinValidityExpression is specified.
 *
 * The test apps also set prompt=login, accessTokenExpiry=true, and identityTokenExpiry=true.
 * This is to make the RP redirect to the login page to make testing easier when the tokens are
 * consider expired based on the tokenMinValidity and token lifetime (60s).
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigurationTokenMinValidityTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationTokenMinValidityTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.tokenMinValidity.jwt")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.tokenMinValidity.opaque")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeat(Constants.OPAQUE_TOKEN_FORMAT);

    private static int BUFFER_SECONDS = 2; // account for code execution delays

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_tokenMinValidity.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps(); // run this after starting the RP so we have the rp port to update the openIdConfig.properties file within the apps

        ShortTokenLifetimePrep s = new ShortTokenLifetimePrep();
        s.shortTokenLifetimePrep(opServer, rpHttpsBase, "TokenMinValidity5s/TokenMinValidity5sServlet", "TokenMinValidity20s/TokenMinValidity20sServlet",
                                 "TokenMinValidity60s/TokenMinValidity60sServlet", "TokenMinValidity90s/TokenMinValidity90sServlet",
                                 "TokenMinValidity0s/TokenMinValidity0sServlet");

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);

        swh.defaultDropinApp(rpServer, "TokenMinValidity5s.war", "oidc.client.tokenMinValidity5s.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "TokenMinValidity20s.war", "oidc.client.tokenMinValidity20s.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "TokenMinValidity60s.war", "oidc.client.tokenMinValidity60s.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "TokenMinValidity90s.war", "oidc.client.tokenMinValidity90s.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "TokenMinValidity0s.war", "oidc.client.tokenMinValidity0s.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "TokenMinValidityNegative5s.war", "oidc.client.tokenMinValidityNegative5s.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "TokenMinValidityDefault.war", "oidc.client.tokenMinValidityDefault.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "TokenMinValidityEL5s.war", "TokenMinValidityEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidityEL5s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidity5s()),
                                       "oidc.client.tokenMinValidityEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "TokenMinValidityEL20s.war", "TokenMinValidityEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidityEL20s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidity20s()),
                                       "oidc.client.tokenMinValidityEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "TokenMinValidityEL60s.war", "TokenMinValidityEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidityEL60s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidity60s()),
                                       "oidc.client.tokenMinValidityEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "TokenMinValidityEL90s.war", "TokenMinValidityEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidityEL90s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidity90s()),
                                       "oidc.client.tokenMinValidityEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "TokenMinValidityEL0s.war", "TokenMinValidityEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidityEL0s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidity0s()),
                                       "oidc.client.tokenMinValidityEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "TokenMinValidityELNegative5s.war", "TokenMinValidityEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidityELNegative5s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidityNegative5s()),
                                       "oidc.client.tokenMinValidityEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "TokenMinValidity5sEL15s.war", "TokenMinValidity5sWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "TokenMinValidity5sEL15s", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getTokenMinValidity15s()),
                                       "oidc.client.tokenMinValidity5sWithEL.servlets", "oidc.client.base.*");

    }

    /**
     * Perform a good end-to-end run using the specified tokenMinValidity.
     * The test:
     * 1. Runs a good end-to-end test
     * 2. Waits the remaining token lifetime minus the token min validity minus a small buffer period
     * 3. Invokes the app again and makes sure we can still get to the protected app right before the tokens expire
     * 4. Waits the small buffer period (x2) to ensure that the tokens have expired
     * 5. Invokes the app again and ensures that we need to authenticate again due to token expiry
     *
     * @param appRoot - the root of the app to invoke
     * @param app - the name of the app to invoke
     * @param tokenMinValidity - the token minimum validity in milliseconds
     * @return the HtmlUnit Page response
     * @throws Exception
     */
    private Page runGoodEndToEndTokenMinValidityTest(String appRoot, String app, int tokenMinValidity) throws Exception {

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        WebClient webClient = getAndSaveWebClient();
        Page response = runGoodEndToEndTest(webClient, appRoot, app);

        actions.testLogAndSleep(getSecondsTilTokenExpiration(response) - (tokenMinValidity / 1000) - BUFFER_SECONDS);
        response = invokeAppGetToApp(webClient, url);

        actions.testLogAndSleep(2 * BUFFER_SECONDS);
        response = invokeAppReturnLoginPage(webClient, url);

        return response;
    }

    private int getSecondsTilTokenExpiration(Page response) throws Exception {
        long exp = getExpFromIdToken(response);
        long now = (long) Math.ceil(System.currentTimeMillis() / 1000.0);
        return (int) (exp - now);
    }

    private long getExpFromIdToken(Page response) throws Exception {
        String token = AutomationTools.getTokenFromResponse(response, ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT + ServletMessageConstants.ID_TOKEN);
        JwtTokenForTest jwtToken = new JwtTokenForTest(token);
        JsonObject payload = jwtToken.getJsonPayload();
        JsonValue exp = payload.get(OpenIdConstant.EXPIRATION_IDENTIFIER);
        return Long.parseLong(exp.toString());
    }

    /**
     * Perform a good end-to-end run on an app which is assumed to have configured a negative tokenMinValidity.
     * The test runs a good end-to-end tokenMinValidity test using the default tokenMinValidity, since negative
     * tokenMinValidity values must not be used and checks that the appropriate warning message is logged by the RP.
     *
     * @param appRoot - the root of the app to invoke
     * @param app - the name of the app to invoke
     * @throws Exception
     */
    private void runGoodEndToEndNegativeTokenMinValidityTest(String appRoot, String app) throws Exception {

        Page response = runGoodEndToEndTokenMinValidityTest(appRoot, app, DEFAULT_TOKEN_MIN_VALIDITY);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2507W_OIDC_MECH_CONFIG_NEGATIVE_INT, "Did not receive an error message stating that the OpenIdAuthenticationMechanismDefinition attribute should not be negative."));
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Perform a bad end-to-end run on an app which is assumed to have configured a tokenMinValidity
     * equal to or greater than the token lifetime. After submitting the login form and obtaining the
     * tokens, the RP should immediately redirect to the authentication endpoint for re-authentication,
     * since the tokens should be immediately considered expired based on the tokenMinValidity.
     *
     * @param appRoot - the root of the app to invoke
     * @param app - the name of the app to invoke
     * @param tokenMinValidity - the token minimum validity in milliseconds
     * @throws Exception
     */
    private void runBadEndToEndTokenMinValidityGreaterThanTokenLifetimeTest(String appRoot, String app) throws Exception {

        String url = rpHttpsBase + "/" + appRoot + "/" + app;
        WebClient webClient = getAndSaveWebClient();
        Page response = invokeAppReturnLoginPage(webClient, url);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        validationUtils.validateResult(response, CommonExpectations.successfullyReachedOidcLoginPage());

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Tests with tokenMinValidity = 5 * 1000.
     * The token lifetime should effectively be 60s - 5s = 55s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_5s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidity5s", "TokenMinValidity5sServlet", 5 * 1000);

    }

    /**
     * Tests with tokenMinValidity = 20 * 1000.
     * The token lifetime should effectively be 60s - 20s = 40s.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_20s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidity20s", "TokenMinValidity20sServlet", 20 * 1000);

    }

    /**
     * Tests with tokenMinValidity = 60 * 1000.
     * The token lifetime should effectively be 60s - 60s = 0s.
     * The protected app should not be able to be reached, since the token is effectively never valid.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_60s() throws Exception {

        runBadEndToEndTokenMinValidityGreaterThanTokenLifetimeTest("TokenMinValidity60s", "TokenMinValidity60sServlet");

    }

    /**
     * Tests with tokenMinValidity = 90 * 1000.
     * The token lifetime should effectively be 60s - 90s = -30s.
     * The protected app should not be able to be reached, since the token is effectively never valid.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_90s() throws Exception {

        runBadEndToEndTokenMinValidityGreaterThanTokenLifetimeTest("TokenMinValidity90s", "TokenMinValidity90sServlet");

    }

    /**
     * Tests with tokenMinValidity = 0.
     * The token lifetime should effectively be 60s - 0s = 60s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_0s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidity0s", "TokenMinValidity0sServlet", 0);

    }

    /**
     * Tests with tokenMinValidity = -5 * 1000.
     * Negative values must not be used and the default value of 10 * 1000 is used instead.
     * The token lifetime should effectively be 60s - 10s = 50s.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_negative5s() throws Exception {

        runGoodEndToEndNegativeTokenMinValidityTest("TokenMinValidityNegative5s", "TokenMinValidityNegative5sServlet");
    }

    /**
     * Tests with tokenMinValidityExpression = 5 * 1000.
     * The token lifetime should effectively be 60s - 5s = 55s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidityExpression_5s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidityEL5s", "TokenMinValidityELServlet", 5 * 1000);

    }

    /**
     * Tests with tokenMinValidityExpression = 20 * 1000.
     * The token lifetime should effectively be 60s - 20s = 40s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidityExpression_20s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidityEL20s", "TokenMinValidityELServlet", 20 * 1000);

    }

    /**
     * Tests with tokenMinValidityExpression = 60 * 1000.
     * The token lifetime should effectively be 60s - 60s = 0s.
     * The protected app should not be able to be reached, since the token is effectively never valid.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidityExpression_60s() throws Exception {

        runBadEndToEndTokenMinValidityGreaterThanTokenLifetimeTest("TokenMinValidityEL60s", "TokenMinValidityELServlet");

    }

    /**
     * Tests with tokenMinValidityExpression = 90 * 1000.
     * The token lifetime should effectively be 60s - 90s = -30s.
     * The protected app should not be able to be reached, since the token is effectively never valid.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidityExpression_90s() throws Exception {

        runBadEndToEndTokenMinValidityGreaterThanTokenLifetimeTest("TokenMinValidityEL90s", "TokenMinValidityELServlet");

    }

    /**
     * Tests with tokenMinValidityExpression = 0 * 1000.
     * The token lifetime should effectively be 60s - 0s = 60s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidityExpression_0s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidityEL0s", "TokenMinValidityELServlet", 0);

    }

    /**
     * Tests with tokenMinValidityExpression = -5 * 1000.
     * Negative values must not be used and the default value of 10s is used instead.
     * The token lifetime should effectively be 60s - 10s = 50s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidityExpression_negative5s() throws Exception {

        runGoodEndToEndNegativeTokenMinValidityTest("TokenMinValidityELNegative5s", "TokenMinValidityELServlet");

    }

    /**
     * Tests with tokenMinValidity = 5 * 1000 and tokenMinValidityExpression = 15 * 1000.
     * tokenMinValidityExpression should take precedence over tokenMinValidity.
     * The token lifetime should effectively be 60s - 15s = 45s.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_5s_tokenMinValidityExpression_15s() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidity5sEL15s", "TokenMinValidity5sWithELServlet", 15 * 1000);

    }

    /**
     * Tests with no tokenMinValidity nor tokenMinValidityExpression.
     * tokenMinValidity should use the default value of 10 * 1000.
     * The token lifetime should effectively be 60s - 10s = 50s.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationTokenMinValidityTests_tokenMinValidity_default() throws Exception {

        runGoodEndToEndTokenMinValidityTest("TokenMinValidityDefault", "TokenMinValidityDefaultServlet", DEFAULT_TOKEN_MIN_VALIDITY);

    }

}
