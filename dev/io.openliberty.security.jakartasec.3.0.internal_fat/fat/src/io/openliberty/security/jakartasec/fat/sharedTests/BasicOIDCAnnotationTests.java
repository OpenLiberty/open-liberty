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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.sharedTests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class BasicOIDCAnnotationTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = BasicOIDCAnnotationTests.class;

    protected static ShrinkWrapHelpers swh = null;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    @Server("jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    public static LibertyServer rpServer;

    public static void baseSetup(LibertyServer jwt_rp, LibertyServer opaque_rp) throws Exception {
        Log.info(thisClass, "setUp", "starting setup");

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, jwt_rp, opaque_rp);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        if (RepeatTestFilter.getRepeatActionsAsString().contains(withOidcClientConfig)) {
            rpServer.startServerUsingExpandedConfiguration("server_withOidcClientConfig.xml", waitForMsgs);
        } else {
            rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        }
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps();

        // rspValues used to validate the app output will be initialized before each test - any unique values (other than the
        //  app need to be updated by the test case - the app is updated by the invokeApp* methods)

        // Wait to ensure LTPA configuration has completed before starting tests.
        opServer.waitForStringInLog("CWWKS4105I");
        rpServer.waitForStringInLog("CWWKS4105I");
    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        Map<String, Object> redirectSetting;
        if (RepeatTestFilter.getRepeatActionsAsString().contains(useRedirectToOriginalResource)) {
            redirectSetting = TestConfigMaps.getRedirectToOriginalResourceExpressionTrue();
        } else {
            redirectSetting = TestConfigMaps.getRedirectToOriginalResourceExpressionFalse();
        }
        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.defaultDropinApp(rpServer, "SimpleServlet.war", "oidc.client.simple.*", "oidc.client.base.utils");
        swh.defaultDropinApp(rpServer, "SimplestAnnotated.war", "oidc.client.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithEL.war", "SimplestAnnotatedWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithEL", null, redirectSetting), "oidc.client.withEL.servlets",
                                       "oidc.client.base.*");
        // duplicate app (just with a different name) - use to test access after authenticating
        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithEL2.war", "SimplestAnnotatedWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithEL2", null, redirectSetting), "oidc.client.withEL.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithELAltOP.war", "SimplestAnnotatedWithELAltOP.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithELAltOP", null, redirectSetting), "oidc.client.withELAltOP.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithELAltOPAndRole.war", "SimplestAnnotatedWithELAltOPAndRole.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithELAltOPAndRole", null, redirectSetting),
                                       "oidc.client.withELAltOPAndRole.servlets",
                                       "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithELAltRole.war", "SimplestAnnotatedWithELAltRole.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithELAltRole", null, redirectSetting), "oidc.client.withELAltRole.servlets",
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
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfUseRedirectToOriginalResource.class)
    public void BasicOIDCAnnotationTests_unprotected() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "SimpleServlet";
        String url = rpHttpsBase + "/SimpleServlet/" + app;

        //show that we get to the test app without having to log in
        Expectations expectations = getGotToTheAppExpectations(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, app, url);

        Page response = actions.invokeUrl(_testName, webClient, url);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test with values hard coded in the annotation (instead of using EL to resolve values)
     * This test is skipped since we would need a unique app with useRedirectToOriginalResource set to false and another
     * with it set to true - we'll cover testing that with the config tests
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfUseRedirectToOriginalResource.class)
    public void BasicOIDCAnnotationTests_withoutEL() throws Exception {

        // the test app has the OP & RP secure ports hard coded (since it doesn't use expression language vars
        // if we end up using a different port, we'll need to skip this test

        if (opServer.getBvtSecurePort() != 8920) {
            Log.info(thisClass, _testName,
                     "Skipping test since the OP was assigned the non-default secure port of 8920 - the app used by this test has that value hard coded in the @OpenIdAuthenticationMechanismDefinition annotation");
            return;
        }
        if (rpServer.getBvtSecurePort() != 8940) {
            Log.info(thisClass, _testName,
                     "Skipping test since the RP was assigned the non-default secure port of 8940 - the app used by this test has that value hard coded in the @OpenIdAuthenticationMechanismDefinition annotation");
            return;
        }
        runGoodEndToEndTest("SimplestAnnotated", "OidcAnnotatedServlet");

    }

    @Test
    public void BasicOIDCAnnotationTests_WithEL() throws Exception {

        runGoodEndToEndTest("SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL");

    }

    @Test
    public void BasicOIDCAnnotationTests_useSameWebClientMultipleRequests() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/OidcAnnotatedServletWithEL";

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD, "OidcAnnotatedServletWithEL");

        // invoke the app again with the same context
        Page response2 = actions.invokeUrl(_testName, webClient, url);

        Expectations secondLoginExpectations = getProcessLoginExpectations("OidcAnnotatedServletWithEL");

        validationUtils.validateResult(response2, secondLoginExpectations);

    }

    // @Test
    public void BasicOIDCAnnotationTests_useSameWebClient_MakeRequestsOfMultipleServlets_sameOPsSameRoles() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/OidcAnnotatedServletWithEL";

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD, "OidcAnnotatedServletWithEL");

        String url2 = rpHttpsBase + "/SimplestAnnotatedWithEL2/OidcAnnotatedServletWithEL";

        // invoke the app again with the same context
        Page response2 = actions.invokeUrl(_testName, webClient, url2);

        Expectations secondLoginExpectations = getProcessLoginExpectations("OidcAnnotatedServletWithEL");

        validationUtils.validateResult(response2, secondLoginExpectations);

    }

    // @Test
    public void BasicOIDCAnnotationTests_useSameWebClient_MakeRequestsOfMultipleServlets_diffOPsSameRoles() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/OidcAnnotatedServletWithEL";

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD, "OidcAnnotatedServletWithEL");

        String url2 = rpHttpsBase + "/SimplestAnnotatedWithELAltOP/OidcAnnotatedServletWithEL";

        // invoke the app again with the same context
        Page response2 = actions.invokeUrl(_testName, webClient, url2);

        Expectations secondLoginExpectations = getProcessLoginExpectations("OidcAnnotatedServletWithEL");

        validationUtils.validateResult(response2, secondLoginExpectations);

    }

    // @Test
    public void BasicOIDCAnnotationTests_useSameWebClient_MakeRequestsOfMultipleServlets_sameOPsDiffRoles() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/OidcAnnotatedServletWithEL";

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD, "OidcAnnotatedServletWithEL");

        String url2 = rpHttpsBase + "/SimplestAnnotatedWithELAltRole/OidcAnnotatedServletWithEL";

        // invoke the app again with the same context
        Page response2 = actions.invokeUrl(_testName, webClient, url2);

        Expectations secondLoginExpectations = getProcessLoginExpectations("OidcAnnotatedServletWithEL");

        validationUtils.validateResult(response2, secondLoginExpectations);

    }

    // @Test
    public void BasicOIDCAnnotationTests_useSameWebClient_MakeRequestsOfMultipleServlets_diffOPsdiffRoles() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/OidcAnnotatedServletWithEL";

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD, "OidcAnnotatedServletWithEL");

        String url2 = rpHttpsBase + "/SimplestAnnotatedWithELAltOPAndRole/OidcAnnotatedServletWithEL";

        // invoke the app again with the same context
        Page response2 = actions.invokeUrl(_testName, webClient, url2);

        Expectations secondLoginExpectations = getProcessLoginExpectations("OidcAnnotatedServletWithEL");

        validationUtils.validateResult(response2, secondLoginExpectations);

    }

    public void BasicOIDCAnnotationTests_ServletUsesCallbackFromAnotherApp() throws Exception {

        // TODO Placeholder for new test - need a new app and need to determine what the behavior should be
    }

    /**
     * Use the same app with different users and different sessions - should use different contexts and show the proper users for
     * each instance
     *
     * @throws Exception
     */
    @Test
    public void BasicOIDCAnnotationTests_multipleDifferentUsers() throws Exception {

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
    public void BasicOIDCAnnotationTests_multipleSameUser() throws Exception {

        WebClient webClient1 = getAndSaveWebClient();
        Page response1 = runGoodEndToEndTest(webClient1, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

        WebClient webClient2 = getAndSaveWebClient(); // need a new webClient
        Page response2 = runGoodEndToEndTest(webClient2, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

        validateNotTheSame("Callback", response1, response2);

    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicOIDCAnnotationTests_passExtraHeadersToServlet() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put("testHeaderName1", "testHeaderValue1");
        headers.put("testHeaderName2", "testHeaderValue2");
        rspValues.setHeaders(headers);

        runGoodEndToEndTest(webClient, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicOIDCAnnotationTests_passParmsToServlet() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        List<NameValuePair> parms = new ArrayList<NameValuePair>();
        parms.add(new NameValuePair("testParmName1", "testParmValue2"));
        parms.add(new NameValuePair("testParmName2", "testParmValue2"));
        rspValues.setParms(parms);

        runGoodEndToEndTest(webClient, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicOIDCAnnotationTests_passExtraCookiesToServlet() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Cookie testCookie1 = new Cookie("", "testCookieName1", "testCookieValue1");
        Cookie testCookie2 = new Cookie("", "testCookieName2", "testCookieValue2");
        rspValues.setCookies(testCookie1, testCookie2);

        runGoodEndToEndTest(webClient, "SimplestAnnotatedWithEL", "OidcAnnotatedServletWithEL", Constants.TESTUSER, Constants.TESTUSERPWD);

    }

    /**
     * Test deploying an app that has multiple annotations embedded - we should see a failure trying to deploy it.
     *
     * @throws Exception
     */
    // TODO - enable once the runtime issues message CWWKS1925E @Test
    public void BasicOIDCAnnotationTests_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar_similar() throws Exception {

        swh.defaultDropinApp(rpServer, "MultipleServletsSimilarAnnotations" + ".war", "oidc.client.similarAnnotations.servlets", "oidc.client.base.*");

        Expectations deployExpectations = new Expectations();
        deployExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS1925E_MULTIPLE_ANNOTATIONS, "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

    // TODO - enable once the runtime issues message CWWKS1925E @Test
    public void BasicOIDCAnnotationTests_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar_diffProvider() throws Exception {

        swh.defaultDropinApp(rpServer, "MultipleServletsDifferentProviders" + ".war", "oidc.client.differentProviders.servlets", "oidc.client.base.*");

        Expectations deployExpectations = new Expectations();
        deployExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS1925E_MULTIPLE_ANNOTATIONS, "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

    // TODO - enable once the runtime issues message CWWKS1925E @Test
    public void BasicOIDCAnnotationTests_multiple_OpenIdAuthenticationMechanismDefinition_annotations_inTheSameWar_diffRo1e() throws Exception {

        swh.defaultDropinApp(rpServer, "MultipleServletsDifferentRoles" + ".war", "oidc.client.differentRoles.servlets", "oidc.client.base.*");

        Expectations deployExpectations = new Expectations();
        deployExpectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS1925E_MULTIPLE_ANNOTATIONS, "Message log did not contain an error indicating that the War contained multiple \"@OpenIdAuthenticationMechanismDefinition\" annotations."));
        validationUtils.validateResult(deployExpectations);

    }

}
