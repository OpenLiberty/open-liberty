/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
import org.junit.Rule;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.social.fat.commonTests.Social_EncryptionTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled;
import componenttest.rules.SkipJavaSemeruWithFipsEnabled.SkipJavaSemeruWithFipsEnabledRule;
import componenttest.topology.impl.LibertyServerWrapper;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class LibertyOP_Encryption_oidc_usingSocialConfig extends Social_EncryptionTests {

    public static Class<?> thisClass = LibertyOP_Encryption_oidc_usingSocialConfig.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @Rule
    public static final SkipJavaSemeruWithFipsEnabled skipJavaSemeruWithFipsEnabled = new SkipJavaSemeruWithFipsEnabled(SocialConstants.SERVER_NAME + ".LibertyOP.opWithStub");

    @BeforeClass
    public static void setUp() throws Exception {

        classOverrideValidationEndpointValue = Constants.USERINFO_ENDPOINT;

        isTestingOidc = true; // affect superclass behavior
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        List<String> opStartMsgs = new ArrayList<String>();
        opStartMsgs.add("CWWKS1631I.*");
        opStartMsgs.add("CWWKZ0001I.*" + Constants.TOKEN_ENDPOINT_SERVLET);

        List<String> opExtraApps = new ArrayList<String>();
        opExtraApps.add(SocialConstants.OP_SAMPLE_APP);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.opWithStub", "op_server_encrypt.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP, true, true);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".LibertyOP.social", "server_LibertyOP_withOpStub_encrypt.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        testOPServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);

        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, null);

        socialSettings = updateLibertyOPSettings(socialSettings);

        socialSettings.setTokenEndpt(socialSettings.getTokenEndpt().replace("oidc/endpoint/OidcConfigSample/token", "TokenEndpointServlet").replace("oidc/providers/OidcConfigSample/token", "TokenEndpointServlet") + "/saveToken");
        socialSettings.setScope("openid profile email");
        socialSettings.setRealm(testOPServer.getServerHttpsString());
        socialSettings.setUserName("testuser");

    }

}
