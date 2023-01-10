/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.config.tests;

import static io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers.buildNonceString;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition useNonce and useNonceExpression
 *
 * This class contains tests to validate that a nonce is added to the auth endpoint call
 * and id token claims if useNonce is set to true, and not added if useNonce is set to false.
 * Additionally, it validates that useNonceExpression overrides the value of useNonce.
 * Lastly, it contains a test to verify that an error occurs if the nonce claim in the
 * id token doesn't match the nonce value stored before the auth endpoint call.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class ConfigurationUseNonceTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationUseNonceTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.useNonce")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    @ClassRule
    public static RepeatTests repeat = createRandomTokenTypeRepeats();

    private static Pattern NONCE_REGEX = Pattern.compile("nonce=[^&]+");

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        setTokenTypeInBootstrap(opServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_useNonce.xml", waitForMsgs);
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

        swh.defaultDropinApp(rpServer, "UseNonceTrue.war", "oidc.client.useNonceTrue.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "UseNonceFalse.war", "oidc.client.useNonceFalse.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "UseNonceTrueELTrue.war", "UseNonceTrueWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UseNonceTrueELTrue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseNonceExpressionTrue()),
                                       "oidc.client.useNonceTrueWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UseNonceTrueELFalse.war", "UseNonceTrueWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UseNonceTrueELFalse", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseNonceExpressionFalse()),
                                       "oidc.client.useNonceTrueWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UseNonceFalseELTrue.war", "UseNonceFalseWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UseNonceFalseELTrue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseNonceExpressionTrue()),
                                       "oidc.client.useNonceFalseWithEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "UseNonceFalseELFalse.war", "UseNonceFalseWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "UseNonceFalseELFalse", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseNonceExpressionFalse()),
                                       "oidc.client.useNonceFalseWithEL.servlets", "oidc.client.base.*");

    }

    private void runGoodEndToEndTestWithNonce(String appRoot, String app) throws Exception {

        runGoodEndToEndWithNonceCheck(appRoot, app, true);

    }

    private void runGoodEndToEndTestWithoutNonce(String appRoot, String app) throws Exception {

        runGoodEndToEndWithNonceCheck(appRoot, app, false);

    }

    private void runGoodEndToEndWithNonceCheck(String appRoot, String app, boolean useNonce) throws Exception {

        String requester = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        // disable redirects so we can validate the 302 responses
        webClient.getOptions().setRedirectEnabled(false);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        // follow redirect from login page to the auth endpoint
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        String authEndpointNonceRegex = "https:\\/\\/localhost:" + opServer.getBvtSecurePort() + "\\/oidc\\/endpoint\\/OP1\\/authorize\\?.*" + NONCE_REGEX;

        // validates:
        // - 302 response
        // - if useNonce = true, then a nonce was included in the req to the auth endpoint
        // - if useNonce = false, then a nonce was not included in the req to the auth endpoint
        Expectations expectations = new Expectations();
        expectations.addFoundStatusCodeAndMessageForCurrentAction();
        if (useNonce) {
            expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_MATCHES, authEndpointNonceRegex, "Did not find nonce in authorization endpoint request."));
        } else {
            expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_DOES_NOT_MATCH, authEndpointNonceRegex, "Found nonce in authorization endpoint request."));
        }
        validationUtils.validateResult(response, expectations);

        // follow redirect from auth endpoint to callback
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // follow redirect from callback to original request
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        // validates:
        // - 200 response
        // - if useNonce = true, then a nonce was included in the id token claims
        // - if useNonce = false, then a nonce was not included in the id token claims
        expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        if (useNonce) {
            expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, buildNonceString(requester), "Did not find an nonce claim in the id token in the OpenIdContext."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_DOES_NOT_CONTAIN, buildNonceString(requester), "Found nonce claim in the id token in the OpenIdContext."));
        }
        validationUtils.validateResult(response, expectations);

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     *
     * Test with useNonce = true.
     * A nonce should be included in the auth endpoint call and in the id token claims.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_true() throws Exception {

        runGoodEndToEndTestWithNonce("UseNonceTrue", "UseNonceTrueServlet");

    }

    /**
     *
     * Test with useNonce = false.
     * A nonce should not be included in the auth endpoint call nor in the id token claims.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_false() throws Exception {

        runGoodEndToEndTestWithoutNonce("UseNonceFalse", "UseNonceFalseServlet");

    }

    /**
     *
     * Test with useNonce = true and useNonceExpression = true.
     * The value used in useNonceExpression should take precedence.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_true_useNonceExpression_true() throws Exception {

        runGoodEndToEndTestWithNonce("UseNonceTrueELTrue", "UseNonceTrueWithELServlet");

    }

    /**
     *
     * Test with useNonce = true and useNonceExpression = false.
     * The value used in useNonceExpression should take precedence.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_true_useNonceExpression_false() throws Exception {

        runGoodEndToEndTestWithoutNonce("UseNonceTrueELFalse", "UseNonceTrueWithELServlet");

    }

    /**
     *
     * Test with useNonce = false and useNonceExpression = true.
     * The value used in useNonceExpression should take precedence.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_false_useNonceExpression_true() throws Exception {

        runGoodEndToEndTestWithNonce("UseNonceFalseELTrue", "UseNonceFalseWithELServlet");

    }

    /**
     *
     * Test with useNonce = false and useNonceExpression = false.
     * The value used in useNonceExpression should take precedence.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_false_useNonceExpression_false() throws Exception {

        runGoodEndToEndTestWithoutNonce("UseNonceFalseELFalse", "UseNonceFalseWithELServlet");

    }

    /**
     *
     * Test with useNonceExpression = true.
     * Intercepts the call to the auth endpoint and alters the nonce value to be all lowercase/uppercase.
     * The test should fail, since the id token returned should contain the all lowercase/uppercase nonce,
     * which will not match the nonce in the RP which was stored before the auth endpoint call as nonces
     * are case-sensitive.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationUseNonceTests_useNonce_true_nonceDoesntMatch() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        // disable redirects, so we can intercept the 302 redirect from the
        // original request to the auth endpoint
        webClient.getOptions().setRedirectEnabled(false);

        String app = "UseNonceTrueServlet";
        String url = rpHttpsBase + "/UseNonceTrue/" + app;

        Page response = actions.invokeUrl(_testName, webClient, url);

        String authenticationEndpoint = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);

        // modify nonce to be all lowercase
        Matcher matcher = NONCE_REGEX.matcher(authenticationEndpoint);
        matcher.find();
        String authenticationEndpointWithModifiedNonce = authenticationEndpoint.replaceFirst(NONCE_REGEX.pattern(), matcher.group().toLowerCase());

        // if original auth endpoint did not have any uppercase, then turn it all uppercase
        if (authenticationEndpoint.equals(authenticationEndpointWithModifiedNonce)) {
            authenticationEndpointWithModifiedNonce = authenticationEndpointWithModifiedNonce.toUpperCase();
        }

        // re-enable redirect to finish the flow normally
        webClient.getOptions().setRedirectEnabled(true);

        response = actions.invokeUrl(_testName, webClient, authenticationEndpointWithModifiedNonce);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2504E_CREDENTIAL_VALIDATION_ERROR, "Did not receive an error stating that an error occurred while validaitng the client credentials."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2415E_TOKEN_VALIDATION_EXCEPTION, "Did not receive an error stating that an error occured while validating the id token."));

        validationUtils.validateResult(response, expectations);

    }

}
