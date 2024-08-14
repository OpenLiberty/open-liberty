/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.LibertyOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.commonTests.Social_BasicConfigTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class LibertyOP_BasicConfigTests_oidc_usingSocialConfig extends Social_BasicConfigTests {

    public static Class<?> thisClass = LibertyOP_BasicConfigTests_oidc_usingSocialConfig.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {

        classOverrideValidationEndpointValue = Constants.USERINFO_ENDPOINT;

        isTestingOidc = true; // affect superclass behavior
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        // TODO fix
        List<String> opStartMsgs = new ArrayList<String>();
        //        opStartMsgs.add("CWWKS1600I.*" + SocialConstants.OIDCCONFIGMEDIATOR_APP);
        opStartMsgs.add("CWWKS1631I.*");

        // TODO fix
        List<String> opExtraApps = new ArrayList<String>();
        opExtraApps.add(SocialConstants.OP_SAMPLE_APP);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(SocialConstants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        // TODO - vary whether OP runs with access_token or jwt as access_token
        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.social", "server_LibertyOP_basicConfigTests_oidc_usingSocialConfig.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);
        // following added for hostnameverificationtest
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS6029E_NO_SIGNING_KEY);
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL); // 248970

        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, null);

        addServerExceptions();

        socialSettings = updateLibertyOPSettings(socialSettings);

    }

    /**
     * Test/config attribute is ONLY valid for oidcLogin (not google) - that's why it resides in this class - tests validtion of
     * the hostname in the cert
     * When including a JWK, having hostNameVerificationEnabledTrue set to true, the validation will fail as we have to use
     * localhost in our test automation.
     * All other JWK tests set hostNameVerificationEnabledTrue to false, so we're implicitly testing false.
     *
     * @throws Exception
     */
   
    @AllowedFFDC({"javax.net.ssl.SSLException", "javax.net.ssl.SSLPeerUnverifiedException",
        "com.ibm.websphere.security.jwt.InvalidTokenException",
        "com.ibm.websphere.security.jwt.InvalidClaimException", 
        "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void Social_BasicConfigTests_hostNameVerificationEnabledTrue() throws Exception {
        testOPServer.reconfigServer("op_server_hostNameVerify.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        genericTestServer.reconfigServer("server_LibertyOP_basicConfigTests_oidc_usingSocialConfig_hostNameVerify.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_hostNameVerificationEnabledTrue");

        List<validationData> expectations = set401ResponseBaseExpectations(updatedSocialTestSettings);
        // twitter marks the redirect as bad in the login page that it returns - you fill in your id/pw and the login request fails - throwing an exception
        //steps = inovke_social_login_actions;
        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.INVOKE_SOCIAL_RESOURCE_ONLY);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received a message indicating that there was a problem with the hostname verification", SocialMessageConstants.CWWKS1708E_UNABLE_TO_CONTACT_PROVIDER);
        //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received a message indicating that the signing key was not found", SocialMessageConstants.CWWKS6049E_JWK_NOT_RETURNED);
        /*
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received a message indicating that the signing key was not found", SocialMessageConstants.CWWKS6029E_NO_SIGNING_KEY);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received a message indicating that the signing key was not found", SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, perform_social_login, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received a message indicating that the signing key was not found", SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        */

        genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Verifies that when authzParameter is configured and fowardLoginParamter is configured to forward login_hint with a
     * value included on the protected resource URL,
     * that both the authzParameter and login_hint and value are forwarded to the Liberty OP.
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the login page from Social and access the HelloWorld application. The Liberty OP trace.log should contain an
     * entry showing
     * that the forwardLoginParamter (login_hint=bob@example.com) was received by the OP authorization endpoint as well as the
     * authzParameter (mq_authz1=mqa1234).
     * </OL>
     */
    @Test
    public void Social_BasicConfigTests_fowardLoginParameter() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_forwardLoginParameter?login_hint=bob@example.com");

        String lastStep = inovke_social_login_actions[inovke_social_login_actions.length - 1];

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null, jwtUserPrincipal);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.TRACE_LOG, SocialConstants.STRING_CONTAINS,
                "OP trace log did not contain entry indicating that the login_hint parameter was forwarded.", "name:.*response_type.*values:.*\\[code\\].*name:.*login_hint.*values:.*\\[bob@example.com\\]");
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login, SocialConstants.TRACE_LOG, SocialConstants.STRING_CONTAINS,
                "OP trace log did not contain entry indicating that the authzParameter was passed to the OP.", "name:.*response_type.*values:.*\\[code\\].*name:.*mq_authz1.*values:.*\\[mqa1234\\]");

        genericSocial(_testName, webClient, inovke_social_login_actions, updatedSocialTestSettings, expectations);

    }
}
