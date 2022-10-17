/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests appSecurity-5.0
 */
@RunWith(FATRunner.class)
public class SimplestAnnotatedTest extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = SimplestAnnotatedTest.class;

    protected static ShrinkWrapHelpers swh = null;

    @Server("jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.opaque.rp")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(Constants.JWT_TOKEN_FORMAT)).andWith(new SecurityTestRepeatAction(Constants.OPAQUE_TOKEN_FORMAT));
//    public static RepeatTests r = RepeatTests.with(new SecurityTestRepeatAction(Constants.JWT_TOKEN_FORMAT));

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(thisClass, "setUp", "starting setup");

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "https://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "https://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps();

        // rspValues used to validate the app output will be initialized before each test - any unique values (other than the
        //  app need to be updated by the test case - the app is updated by the invokeApp* methods)
    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.defaultDropinApp(rpServer, "SimpleServlet.war", "oidc.client.simple.*", "oidc.client.base.utils");
//        swh.defaultDropinApp(rpServer, "OnlyProviderInAnnotation.war", "oidc.simple.client.onlyProvider.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "SimplestAnnotated.war", "oidc.client.servlets", "oidc.client.base.*");
        swh.defaultDropinApp(rpServer, "SimplestAnnotatedWithEL.war", "oidc.client.withEL.servlets", "oidc.client.base.*");

        // deploy the apps that will be updated at runtime (now) (such as deploying the same app runtime with different embedded configs)
        swh.deployConfigurableTestApps(rpServer, "newApp2.war", "GenericOIDCAuthMechanism.war", TestConfigMaps.getTest1(), "oidc.client.generic.servlets",
                                       "oidc.client.base.*");

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

    @Test
    public void testSimplestAnnotatedServlet_useSameWebClientMultipleRequests() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/OidcAnnotatedServletWithEL";

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD, "OidcAnnotatedServletWithEL");

        // invoke the app again with the same context
        Page response2 = actions.invokeUrl(_testName, webClient, url);

        Expectations secondLoginExpectations = getProcessLoginExpectations("OidcAnnotatedServletWithEL");

        validationUtils.validateResult(response2, secondLoginExpectations);

    }

    public void testSimplestAnnotatedServlet_ServletUsesCallbackFromAnotherApp() throws Exception {

        // TODO Placeholder for new test - need a new app and need to determine what the behavior should be
    }

    /**
     * Use the same app with different users and different sessions - should use different contexts and show the proper users for
     * each instance
     *
     * @throws Exception
     */
    @Test
    public void testSimplestAnnotatedServlet_multipleDifferentUsers() throws Exception {

        // the test app has the OP secure port hard coded (since it doesn't use expression language vars
        // if we end up using a different port, we'll need to skip this test

        WebClient webClient1 = getAndSaveWebClient();
        Page response1 = runGoodEndToEndTest(webClient1, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

        rspValues.setSubject("user1");
        WebClient webClient2 = getAndSaveWebClient(); // need a new webClient
        Page response2 = runGoodEndToEndTest(webClient2, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", "user1", "user1pwd");

        validateNotTheSame("Callback", response1, response2);

    }

    /**
     * Use the same app with different users and different sessions - should use different contexts and show the proper users for
     * each instance
     *
     * @throws Exception
     */
    @Test
    public void testSimplestAnnotatedServlet_multipleSameUser() throws Exception {

        // the test app has the OP secure port hard coded (since it doesn't use expression language vars
        // if we end up using a different port, we'll need to skip this test

        WebClient webClient1 = getAndSaveWebClient();
        Page response1 = runGoodEndToEndTest(webClient1, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

        WebClient webClient2 = getAndSaveWebClient(); // need a new webClient
        Page response2 = runGoodEndToEndTest(webClient2, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

        validateNotTheSame("Callback", response1, response2);

    }

    @Test
    public void testSimplestAnnotatedServlet_passParmsToServlet() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("testHeaderName", "testHeaderValue");

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair("testParmName", "testParmValue"));
        runGoodEndToEndTest(webClient, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD, headers, parms);

    }

    /**
     * Test deploying an app that has multiple annotations embedded - we should see a failure trying to deploy it.
     *
     * @throws Exception
     */
    // TODO - enable once the runtime issues message CWWKS1925E @Test
    public void testSimplestAnnotatedServlet_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar_similar() throws Exception {

        swh.defaultDropinApp(rpServer, "MultipleServletsSimilarAnnotations" + ".war", "oidc.client.similarAnnotations.servlets", "oidc.client.base.*");

        Expectations deployExpectations = new Expectations();
        deployExpectations.addExpectation(new ServerMessageExpectation(rpServer, "someMessage", "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

    // TODO - enable once the runtime issues message CWWKS1925E @Test
    public void testSimplestAnnotatedServlet_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar_diffProvider() throws Exception {

        swh.defaultDropinApp(rpServer, "MultipleServletsDifferentProviders" + ".war", "oidc.client.differentProviders.servlets", "oidc.client.base.*");

        Expectations deployExpectations = new Expectations();
        deployExpectations.addExpectation(new ServerMessageExpectation(rpServer, "someMessage", "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

    // TODO - enable once the runtime issues message CWWKS1925E @Test
    public void testSimplestAnnotatedServlet_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar_diffRoke() throws Exception {

        swh.defaultDropinApp(rpServer, "MultipleServletsDifferentRoles" + ".war", "oidc.client.differentRoles.servlets", "oidc.client.base.*");

        Expectations deployExpectations = new Expectations();
        deployExpectations.addExpectation(new ServerMessageExpectation(rpServer, "someMessage", "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

}
