/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class SimplestAnnotatedTest extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = SimplestAnnotatedTest.class;

    @Server("io.openliberty.security.jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("io.openliberty.security.jakartasec-3.0_fat.rp")
    public static LibertyServer rpServer;

    protected static ShrinkWrapHelpers swh = null;

    @BeforeClass
    public static void setUp() throws Exception {

        transformAppsInDefaultDirs(opServer, "dropins");

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "https://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        transformAppsInDefaultDirs(rpServer, "dropins");

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "https://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps();

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.defaultDropinApp(rpServer, "SimpleServlet.war", "oidc.client.base.servlets");
        swh.defaultDropinApp(rpServer, "OnlyProviderInAnnotation.war", "oidc.simple.client.onlyProvider.servlets", "oidc.client.base.servlets");
        swh.defaultDropinApp(rpServer, "SimplestAnnotated.war", "oidc.simple.client.servlets", "oidc.client.base.servlets");
        swh.defaultDropinApp(rpServer, "SimplestAnnotatedWithEL.war", "oidc.simple.client.withEL.servlets", "oidc.client.base.servlets");

        // deploy the apps that will be updated at runtime (now) (such as deploying the same app runtime with different embedded configs)
        swh.deployConfigurableTestApps(rpServer, "newApp2.war", "GenericOIDCAuthMechanism.war", TestConfigMaps.getTest1(),
                                       "oidc.simple.client.generic.servlets",
                                       "oidc.client.base.servlets");

    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/
    /**
     * Test an unprotected app
     *
     * @throws Exception
     */
    @Test
    public void testSimplestAnnotatedServlet_unprotected() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "SimpleServlet";
        String url = rpHttpsBase + "/SimpleServlet/" + app;

        //show that we get to the test app without having to log in
        Expectations expectations = getGotToTheAppExpectations(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, app, url);

        Page response = actions.invokeUrl(_testName, webClient, url);

        validationUtils.validateResult(response, expectations);

    }

    @Test
    public void testSimplestAnnotatedServlet_withoutEL() throws Exception {

        // the test app has the OP secure port hard coded (since it doesn't use expression language vars
        // if we end up using a different port, we'll need to skip this test

        runGoodEndToEndTest("SimplestAnnotated", "OidcAnnotatedServlet");

    }

    @Test
    public void testSimplestAnnotatedServlet_WithEL() throws Exception {

        runGoodEndToEndTest("SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL");

    }

    /**
     * Test deplying an app that has multiple annotations embedded - we should see a failure trying to deploy it.
     *
     * @throws Exception
     */
    //@Test
    public void testSimplestAnnotatedServlet_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar() throws Exception {

        swh.defaultDropinApp(rpServer, "SimplestAnnotatedWithAndWithoutEL" + ".war", "oidc.simple.client.withAndWithoutEL.servlets", "oidc.client.base.servlets");

        Expectations deployExpectations = new Expectations();
        deployExpectations
                        .addExpectation(new ServerMessageExpectation(rpServer, "someMessage", "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

    public void testSimplestAnnotatedServlet_ServletUsesCallbackFromAnotherApp() throws Exception {

        // TODO Placeholder for new test - need a new app and need to determine what the behavior should be
    }

}
