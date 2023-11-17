/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.security.social;

import static io.openliberty.checkpoint.fat.security.common.FATSuite.getTestMethod;
import static io.openliberty.checkpoint.fat.security.common.FATSuite.updateVariableConfig;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerWrapper;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@CheckpointTest
public class LibertyOP_BasicTests_oauth_usingSocialConfig extends SocialCommonTest {

    public static Class<?> thisClass = LibertyOP_BasicTests_oauth_usingSocialConfig.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    public static String UserApiEndpoint = Constants.USERINFO_ENDPOINT;

    public static boolean isTestingOidc = false;

    public TestMethod testMethod;

    public static LibertyServer opServer;

    public static LibertyServer genericServer;

    private static final String OP_SERVER = SocialConstants.SERVER_NAME + ".LibertyOP.op";

    private static final String GENERIC_SERVER = SocialConstants.SERVER_NAME + ".LibertyOP.social";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(OP_SERVER, GENERIC_SERVER).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(OP_SERVER, GENERIC_SERVER).fullFATOnly());

    @BeforeClass
    public static void setUpServers() throws Exception {
        classOverrideValidationEndpointValue = UserApiEndpoint;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        List<String> opStartMsgs = new ArrayList<String>();

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(SocialConstants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        skipServerStart = true;
        testOPServer = commonSetUp(OP_SERVER, "server.xml",
                                   SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null,
                                   SocialConstants.OIDC_OP, true, true, tokenType, certType);

        opServer = testOPServer.getServer();
        skipServerStart = true;
        genericTestServer = commonSetUp(GENERIC_SERVER,
                                        "server.xml", SocialConstants.GENERIC_SERVER, extraApps,
                                        SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        genericServer = genericTestServer.getServer();
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        opServer.startServer(testMethod + ".log");

        genericServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        genericServer.startServer(testMethod + ".log");
        configureBeforeRestore();

        genericServer.checkpointRestore();

        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP);

        socialSettings = updateLibertyOPSettings(socialSettings);
    }

    private void configureBeforeRestore() {
        try {
            opServer.saveServerConfiguration();
            genericServer.saveServerConfiguration();

            Log.info(getClass(), testName.getMethodName(), "Configuring: " + testMethod);
            switch (testMethod) {
                case testSocialBasicMainPathWithUpdatedConfig:
                    Log.info(getClass(), testName.getMethodName(), "UPDATING: " + testMethod);
                    // Update the client secret in the server.xml
                    updateVariableConfig(opServer, "clientsecret", "secret2");
                    updateVariableConfig(genericServer, "clientsecret", "secret2");
                    break;
                default:
                    Log.info(getClass(), testName.getMethodName(), "No configuration required: " + testMethod);
                    break;
            }

        } catch (Exception e) {
            throw new AssertionError("Unexpected error configuring test.", e);
        }

    }

    @Test
    public void testSocialBasicMainPath() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String lastStep = inovke_social_login_actions[inovke_social_login_actions.length - 1];

        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_FULL,
                                            SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null,
                                            jwtUserPrincipal);

        genericSocial(_testName, webClient, inovke_social_login_actions, socialSettings, expectations);

        if (isTestingOidc) {
            testUserInfo(webClient);
        }
    }

    /**
     * Test that userinfo is retrieved and available from an API call. If userinfo url is defined and enabled in
     * metadata, then upon authentication the userinfo JSON from the OP, if available, is to be stored in the subject as
     * a string and made accessible through the UserProfile API. Since we invoked the protected resource, we should
     * already be authenticated. This calls a jsp that invokes the UserProfile.getUserInfo() API to check the userinfo.
     */
    void testUserInfo(WebClient webClient) throws Exception {
        String endpoint = socialSettings.getProtectedResource();
        endpoint = endpoint.replace("rest/helloworld", "userProfileUserInfoApiTest.jsp");
        WebRequest req = new WebRequest(new URL(endpoint));
        HtmlPage wr = webClient.getPage(req);
        String response = wr.asText();
        Log.info(thisClass, _testName, "Got JSP response: " + response);

        String testAction = "testUserInfo";
        String expectedUser = socialSettings.getAdminUser();
        Expectations expectations = new Expectations();
        expectations
                        .addExpectation(Expectation.createResponseExpectation(testAction, "\"sub\":\"" + expectedUser + "\"",
                                                                              "Did not find expected \"sub\" claim and value in the JSP response."));
        expectations
                        .addExpectation(Expectation.createResponseExpectation(testAction, "\"name\":\"" + expectedUser + "\"",
                                                                              "Did not find expected \"name\" claim and value in the JSP response."));
        expectations.addExpectation(new ResponseFullExpectation(testAction, SocialConstants.STRING_MATCHES, "\"iss\":\"http[^\"]+/OidcConfigSample\"", "Did not find expected \"iss\" claim and value in the JSP response."));
        List<validationData> convertedExpectations = ValidationDataToExpectationConverter
                        .convertExpectations(expectations);
        validationTools.validateResult(wr, testAction, convertedExpectations, socialSettings);
    }

    /**
     * Test purpose: Update the client secret in the server.xml after checkpoint in both testOPServer and genericTestServer
     *
     * Expected Results: Should get the login page from Social. After entering a valid id/pwd, we should receive access to the
     * helloworld app
     *
     * @throws Exception
     */
    @Test
    public void testSocialBasicMainPathWithUpdatedConfig() throws Exception {
        WebClient webClient = getAndSaveWebClient();

        String lastStep = inovke_social_login_actions[inovke_social_login_actions.length - 1];

        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_FULL,
                                            SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null,
                                            jwtUserPrincipal);

        genericSocial(_testName, webClient, inovke_social_login_actions, socialSettings, expectations);
    }

    /**
     * Test Purpose:Invoke Helloworld
     *
     * Expected Results: Should get the login page from Social. After entering a valid id/pw, we should receive access to the
     * helloworld app
     *
     * @throws Exception
     */
    @Test
    public void testSocialBasicMainPathWithJwtSsoFeature() throws Exception {

        genericTestServer.reconfigServer("server_withJwtSsoFeature.xml", _testName, true, null);

        WebClient webClient = getAndSaveWebClient();

        String lastStep = inovke_social_login_actions[inovke_social_login_actions.length - 1];

        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        // Ensure that the subject principals include a JWT
        String issClaim = "\"iss\":\"https?://[^/]+/jwt(sso)?/defaultJwtSso\"";
        String jwtUserPrincipal = "Principal: \\{.+" + issClaim;
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_FULL,
                                            SocialConstants.STRING_MATCHES,
                                            "Did not find the expected JWT principal in the app response but should have.", null, jwtUserPrincipal);

        genericSocial(_testName, webClient, inovke_social_login_actions, socialSettings, expectations);
    }

    @After
    public void stopServers() throws Exception {
        try {
            opServer.stopServer();
            genericServer.stopServer();
        } finally {
            Log.info(getClass(), "stopServers", "RESTORING ");
            opServer.restoreServerConfiguration();
            genericServer.restoreServerConfiguration();

        }

    }

    static enum TestMethod {
        testSocialBasicMainPath,
        testSocialBasicMainPathWithUpdatedConfig,
        testSocialBasicMainPathWithJwtSsoFeature,
        testSocialBasicInvalidUserPassword,
        unknown
    }
}
