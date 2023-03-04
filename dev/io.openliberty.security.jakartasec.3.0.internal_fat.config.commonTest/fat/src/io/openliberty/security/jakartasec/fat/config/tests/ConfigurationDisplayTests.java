/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

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
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;

/**
 * Tests @OpenIdAuthenticationMechanismDefinition display and displayExpression
 *
 * This class contains tests to validate that the display/displayExpression value is
 * correctly added to the authentication endpoint request as a query param.
 * Additionally, it validates that not specifying any display/displayExpression value
 * uses the default of page and that the displayExpression value takes precedence over
 * the display value.
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigurationDisplayTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = ConfigurationDisplayTests.class;

    @Server("jakartasec-3.0_fat.config.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.config.rp.display.jwt")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.config.rp.display.opaque")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    // create repeats for opaque and jwt tokens - in lite mode, only run with opaque tokens
    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats(TestMode.LITE, Constants.OPAQUE_TOKEN_FORMAT);

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_display.xml", waitForMsgs);
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

        swh.defaultDropinApp(rpServer, "DisplayPage.war", "oidc.client.displayPage.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "DisplayPopup.war", "oidc.client.displayPopup.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "DisplayTouch.war", "oidc.client.displayTouch.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "DisplayWap.war", "oidc.client.displayWap.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "DisplayEmpty.war", "oidc.client.displayEmpty.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "DisplayELPage.war", "DisplayEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "DisplayELPage", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getDisplayPage()),
                                       "oidc.client.displayEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "DisplayELPopup.war", "DisplayEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "DisplayELPopup", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getDisplayPopup()),
                                       "oidc.client.displayEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "DisplayELTouch.war", "DisplayEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "DisplayELTouch", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getDisplayTouch()),
                                       "oidc.client.displayEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "DisplayELWap.war", "DisplayEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "DisplayELWap", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getDisplayWap()),
                                       "oidc.client.displayEL.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "DisplayPopupELTouch.war", "DisplayPopupWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "DisplayPopupELTouch", "allValues.openIdConfig.properties",
                                                             TestConfigMaps.getDisplayTouch()),
                                       "oidc.client.displayPopupWithEL.servlets", "oidc.client.base.*");
    }

    private void runGoodEndToEndTestWithDisplayCheck(String appRoot, String app, DisplayType displayType) throws Exception {

        WebClient webClient = getAndSaveWebClient();
        webClient.getOptions().setRedirectEnabled(false);

        String url = rpHttpsBase + "/" + appRoot + "/" + app;
        Page response = invokeApp(webClient, url);

        String authEndpoint = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
        response = actions.invokeUrl(_testName, webClient, authEndpoint);

        String displayString = displayType.toString().toLowerCase();
        String authEndpointDisplayRegex = "https:\\/\\/localhost:" + opServer.getBvtSecurePort() + "\\/oidc\\/endpoint\\/OP[0-9]*\\/authorize\\?.*display=" + displayString
                                          + "(&|$)";

        Expectations authExpectations = new Expectations();
        authExpectations.addFoundStatusCodeAndMessageForCurrentAction();
        authExpectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_MATCHES, authEndpointDisplayRegex, "Did not find the correct d in authorization endpoint request."));
        validationUtils.validateResult(response, authExpectations);

        webClient.getOptions().setRedirectEnabled(true);

        String loginPageUrl = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
        response = actions.invokeUrl(_testName, webClient, loginPageUrl);
        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        validationUtils.validateResult(response, getGeneralAppExpecations(app));

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     *
     * Tests with display=page
     * The authentication endpoint request should append 'display=page' as a query parameter.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationDisplayTests_display_page() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayPage", "DisplayPageServlet", DisplayType.PAGE);

    }

    /**
     *
     * Tests with display=popup
     * The authentication endpoint request should append 'display=popup' as a query parameter.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationDisplayTests_display_popup() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayPopup", "DisplayPopupServlet", DisplayType.POPUP);

    }

    /**
     *
     * Tests with display=touch
     * The authentication endpoint request should append 'display=touch' as a query parameter.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationDisplayTests_display_touch() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayTouch", "DisplayTouchServlet", DisplayType.TOUCH);

    }

    /**
     *
     * Tests with display=wap
     * The authentication endpoint request should append 'display=wap' as a query parameter.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ConfigurationDisplayTests_display_wap() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayWap", "DisplayWapServlet", DisplayType.WAP);

    }

    /**
     *
     * Tests with displayExpression=page
     * The authentication endpoint request should append 'display=page' as a query parameter.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationDisplayTests_displayEL_page() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayELPage", "DisplayELServlet", DisplayType.PAGE);

    }

    /**
     *
     * Tests with displayExpression=popup
     * The authentication endpoint request should append 'display=popup' as a query parameter.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationDisplayTests_displayEL_popup() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayELPopup", "DisplayELServlet", DisplayType.POPUP);

    }

    /**
     *
     * Tests with displayExpression=touch
     * The authentication endpoint request should append 'display=touch' as a query parameter.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationDisplayTests_displayEL_touch() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayELTouch", "DisplayELServlet", DisplayType.TOUCH);

    }

    /**
     *
     * Tests with displayExpression=wap
     * The authentication endpoint request should append 'display=wap' as a query parameter.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationDisplayTests_displayEL_wap() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayELWap", "DisplayELServlet", DisplayType.WAP);

    }

    /**
     *
     * Tests without display nor displayExpression
     * The default value of page should be used.
     * The authentication endpoint request should append 'display=page' as a query parameter.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationDisplayTests_display_empty() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayEmpty", "DisplayEmptyServlet", DisplayType.PAGE);

    }

    /**
     *
     * Tests with display=popup and displayExpression=touch
     * The displayExpression should take precedence over display.
     * The authentication endpoint request should append 'display=touch' as a query parameter.
     *
     * @throws Exception
     */
    @Test
    public void ConfigurationDisplayTests_display_popup_EL_touch() throws Exception {

        runGoodEndToEndTestWithDisplayCheck("DisplayPopupELTouch", "DisplayPopupWithELServlet", DisplayType.TOUCH);

    }

}
