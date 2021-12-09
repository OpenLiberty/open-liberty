/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.LibertyOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.commonTests.Social_ErrorDiscoveryConfigTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test covers common error conditions and startup messages with the configuration of Social Login discovery.
 *
 * The social login discovery function differs from OIDC discovery in how the server reconfig is processed. Once an initial
 * discovery is performed
 * in the test setup, a reconfig with a new server config file does NOT immediately result in processing discovery again. Instead,
 * discovery using the new config is
 * processed after a resource access is attempted. Therefore, the tests check for error messages with a bad config on the attempt
 * to
 * display the login form (rather than on the reconfigServer step as is done with OIDC client discovery).
 *
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class LibertyOP_ErrorConfigTests_oidc_usingSocialDiscoveryConfig extends Social_ErrorDiscoveryConfigTests {

    public static Class<?> thisClass = LibertyOP_ErrorConfigTests_oidc_usingSocialDiscoveryConfig.class;
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    private static boolean executedOnce = false;

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
        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.op", "op_server_disc_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.socialDisc", "server_LibertyOP_errorDiscoveryTests_oidc_orig.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        testOPServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL); // 272108 seen intermittently on OP for client with bad SSL trust
        // following added for hostnameverificationtest
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS6029E_NO_SIGNING_KEY);
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS6031E_JWT_CONSUMER_CANNOT_PROCESS_STRING);
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKS5453E_PROBLEM_CREATING_JWT);
        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKO0801E_CANNOT_INIT_SSL); // 248970
        genericTestServer.addIgnoredServerException("CWWKS5500E"); // discovery completed
        genericTestServer.addIgnoredServerException("CWWKS5501E"); // bad authz endpoint with discovery
        genericTestServer.addIgnoredServerException("CWWKS6107W"); // discovered override configured
        genericTestServer.addIgnoredServerException("CWWKS6114E"); // no response from endpoint

        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, "server_LibertyOP_basicTests_oidc_usingSocialConfig");

        addServerExceptions();

        socialSettings = updateLibertyOPSettings(socialSettings);

        // Force initial discovery by attempting to access a protected resource so that the discovery service will be activated. This discovery results in 401. Then in the tests,
        // the reconfig will try to start the discovery service again and appropriate error messages can be checked in the test. No need to verify results here.

        Log.info(thisClass, "setupBeforeFirstTest", "Single time test setup - Accessing social resource to perform initial discovery.");
        WebClient webClient = TestHelpers.getWebClient(); // can't use the static method, so handle one off creation/deletion in this setup
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidcLogin1");
        List<validationData> expectations = null;
        invokeSocialResource(_testName, webClient, updatedSocialTestSettings, expectations);
        TestHelpers.destroyWebClient(webClient);
    }

}
