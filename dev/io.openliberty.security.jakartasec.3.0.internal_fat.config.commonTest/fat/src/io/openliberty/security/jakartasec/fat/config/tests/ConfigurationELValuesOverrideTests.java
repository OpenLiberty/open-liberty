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
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests to validate that values from EL "Expression" attributes override the values coded in the annotations.  These are attributes like useSessionExpression overriding useSession.
 *
 * Check if the openIdContext contains the annotation settings - if we log that, we can hopefully check in the response output.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class ConfigurationELValuesOverrideTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationELValuesOverrideTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.ELOverride.jwt")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.ELOverride.opaque")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

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

        // We need to create new apps with the servlet containing good/bad values for the attribute and then create apps
        // using that as a base and populate the openIdConfig.properties file to specify the <attr>Expression var value
        // with alternate/conflicting override string equivalent values
        swh.deployConfigurableTestApps(rpServer, "useSessionTrueELTrue.war", "UseSessionTrue.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "useSessionTrueELTrue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseSessionExpressionTrue()),
                                       "oidc.client.useSessionTrue.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "useSessionTrueELFalse.war", "UseSessionTrue.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "useSessionTrueELFalse", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseSessionExpressionFalse()),
                                       "oidc.client.useSessionTrue.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "useSessionFalseELTrue.war", "UseSessionFalse.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "useSessionFalseELTrue", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseSessionExpressionTrue()),
                                       "oidc.client.useSessionFalse.servlets",
                                       "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "useSessionFalseELFalse.war", "UseSessionFalse.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "useSessionFalseELFalse", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getUseSessionExpressionFalse()),
                                       "oidc.client.useSessionFalse.servlets",
                                       "oidc.client.base.*");

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * The annotation contains useSession=false, we'll test with useSessionExpression = true and expect the state validation to fail
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationELValuesOverrideTests_useSession_true_useSessionExpression_true() throws Exception {

        runGoodEndToEndTest("useSessionTrueELTrue", "UseSessionTrueServlet");

    }

    /**
     * The annotation contains useSession=false, we'll test with useSessionExpression = false and expect the state validation to succeed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationELValuesOverrideTests_useSession_true_useSessionExpression_false() throws Exception {

        runGoodEndToEndTest("useSessionTrueELFalse", "UseSessionTrueServlet");

    }

    /**
     * The annotation contains useSession=false, we'll test with useSessionExpression = true and expect the state validation to fail
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationELValuesOverrideTests_useSession_false_useSessionExpression_true() throws Exception {

        runGoodEndToEndTest("useSessionFalseELTrue", "UseSessionFalseServlet");

    }

    /**
     * The annotation contains useSession=false, we'll test with useSessionExpression = false and expect the state validation to succeed
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationELValuesOverrideTests_useSession_false_useSessionExpression_false() throws Exception {

        runGoodEndToEndTest("useSessionFalseELFalse", "UseSessionFalseServlet");

    }

    // useSession - may need a different OP with different httpSession values in the server config in order to fully test
    // <httpSession cookieHttpOnly="false" cookieName="clientJSESSIONID"/>
}
