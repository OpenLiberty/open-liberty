/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import java.util.List;

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
import io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;
import io.openliberty.security.jakartasec.fat.utils.WsSubjectExpectationHelpers;

/**
 * Tests various values set for annotation attributes within the ClaimsDefinitiion
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigurationClaimsDefinitionTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationClaimsDefinitionTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.claimsDef.jwt")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.claimsDef.opaque")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    protected static String app = "ClaimsDefinitionServlet";

    // create repeats for opaque and jwt tokens - in lite mode, only run with jwt tokens
    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats(TestMode.LITE, Constants.JWT_TOKEN_FORMAT);

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
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

        // deploy the apps that are defined 100% by the source code tree
        swh.defaultDropinApp(rpServer, "ClaimsDefinition.war", "oidc.client.claimsDefinition.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "ClaimsDefinitionNoRole.war", "oidc.client.claimsDefinitionNoRole.servlets", "oidc.client.base.*");

        // deploy the apps that will be updated at runtime (now) (such as deploying the same app runtime with different embedded configs)
        swh.deployConfigurableTestApps(rpServer, "badCallerNameClaim.war", "ClaimsDefinition.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "badCallerNameClaim", "claimDefinitions.openIdConfig.properties",
                                                             TestConfigMaps.getBadCallerNameClaim()),
                                       "oidc.client.claimsDefinition.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyCallerNameClaim.war", "ClaimsDefinition.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "emptyCallerNameClaim", "claimDefinitions.openIdConfig.properties",
                                                             TestConfigMaps.getEmptyCallerNameClaim()),
                                       "oidc.client.claimsDefinition.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "badCallerGroupsClaim.war", "ClaimsDefinition.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "badCallerGroupsClaim", "claimDefinitions.openIdConfig.properties",
                                                             TestConfigMaps.getBadCallerGroupsClaim()),
                                       "oidc.client.claimsDefinition.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "emptyCallerGroupsClaim.war", "ClaimsDefinition.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "emptyCallerGroupsClaim", "claimDefinitions.openIdConfig.properties",
                                                             TestConfigMaps.getEmptyCallerGroupsClaim()),
                                       "oidc.client.claimsDefinition.servlets",
                                       "oidc.client.base.*");

    }

    public Expectations get401Expectations() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();

        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_DOES_NOT_CONTAIN, "got here", "Did not land on the servlet."));

        return expectations;
    }

    public Expectations get403Expectations(boolean includeSendFailure) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addForbiddenStatusCodeAndMessageForCurrentAction();

        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_DOES_NOT_CONTAIN, "got here", "Did not land on the servlet."));

        if (includeSendFailure) {
            expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS1652A_AUTH_SEND_FAILURE, "Did not receive an error message stating that Authentication failed with a SEND_FAILURE."));
        }

        return expectations;
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    // ConfigurationClaimsDefinitionTests_goodCallerNameClaim() - test is not needed as all other tests must set a good callerNameClaim
    /**
     * Test with an invalid callerNameClaim (using the Liberty OP, something other than sub)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationClaimsDefinitionTests_badCallerNameClaim() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/badCallerNameClaim/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = get401Expectations();

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test with callerNameClaim set to an empty string (using the Liberty OP, this will result in a failure as we use sub and the default is "preferred_username")
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationClaimsDefinitionTests_emptyCallerNameClaim() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/emptyCallerNameClaim/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = get401Expectations();

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test with a valid callerGroupsClaim (groups - that is the default) - we probably one other test that specifies this claim, but a quick test here can't hurt
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationClaimsDefinitionTests_goodCallerGroupsClaim() throws Exception {

        runGoodEndToEndTest("ClaimsDefinition", app);

    }

    /**
     * Test with an invalid callerGroupsClaim (something other than groups - that is the default)
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationClaimsDefinitionTests_badCallerGroupsClaim() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/badCallerGroupsClaim/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = get403Expectations(false);
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2104I_USER_NOT_IN_STATE, "Did not receive an error message stating that there is no group to check role access for."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS9104A_NO_ACCESS_FOR_USER, "Did not receive an error message stating that a user is not granted access to a resource."));

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test with callerGroupsClaim set to an empty string - test should succeed as the Liberty OP uses the default value of "groups"
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationClaimsDefinitionTests_emptyCallerGroupsClaim() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/emptyCallerGroupsClaim/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        Expectations expectations = get403Expectations(false);
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2104I_USER_NOT_IN_STATE, "Did not receive an error message stating that there is no group to check role access for."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS9104A_NO_ACCESS_FOR_USER, "Did not receive an error message stating that a user is not granted access to a resource."));

        validationUtils.validateResult(response, expectations);

    }

    // @Test
    public void ConfigurationClaimsDefinitionTests_goodCallerGroupsClaim_noRolesInApp() throws Exception {

        rspValues.setSubject(null);
        rspValues.setClientId(null);
        rspValues.setRealm(null);
        rspValues.setIssuer(null);
        rspValues.setTokenType(null);

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/ClaimsDefinitionNoRole/ClaimsDefinitionNoRoleServlet";

        Page response = invokeApp(webClient, url);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        OpenIdContextExpectationHelpers.getOpenIdContextExpectations(null, expectations, ServletMessageConstants.SERVLET, rspValues);
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, expectations, ServletMessageConstants.SERVLET, rspValues);

        validationUtils.validateResult(response, expectations);

    }

    @Test
    public void ConfigurationClaimsDefinitionTests_goodCallerGroupsClaim_userNotInRole() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/ClaimsDefinition/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = actions.doFormLogin(response, "user1", "user1pwd");

        Expectations expectations = new Expectations();
        expectations.addForbiddenStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, Constants.AUTHORIZATION_ERROR, "Did not receive an authorization failure."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS9104A_NO_ACCESS_FOR_USER, "Did not receive an error message stating that the user is not granted access."));

        validationUtils.validateResult(response, expectations);

    }
}
