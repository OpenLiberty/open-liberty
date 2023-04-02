/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.okdServiceLogin;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.social.fat.okdServiceLogin.RepeatActions.OKDServiceLoginRepeatActions;
import com.ibm.ws.security.social.fat.okdServiceLogin.commonTests.OKDServiceLogin_SSLTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * SSL targeted testing of OKD Service Account using a fake/stubbed OpenShift user validation endpoint
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OKDServiceLogin_SSLTests_StubbedServer extends OKDServiceLogin_SSLTests {

    public static Class<?> thisClass = OKDServiceLogin_SSLTests_StubbedServer.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(OKDServiceLoginRepeatActions.usingStub());

    @BeforeClass
    public static void setUp() throws Exception {

        stubbedTests = true;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps1 = new ArrayList<String>();
        extraApps1.add(SocialConstants.HELLOWORLD_SERVLET);
        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add(SocialConstants.STUBBED_USER_VALIDATION_API_SERVLET);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".stubbedOKDServiceLogin.op", "server_OP_SSLTests_usingStubs.xml", SocialConstants.OIDC_OP, extraApps2, SocialConstants.DO_NOT_USE_DERBY, null);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".OKDServiceLogin.social", "server_OKDServiceLogin_SSLTests_usingStubs.xml", SocialConstants.GENERIC_SERVER, extraApps1, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setActionsForProvider(SocialConstants.OPENSHIFT_PROVIDER, SocialConstants.OAUTH_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, "server_OKDServiceLogin_SSLTests_usingStubs");

        socialSettings = updateOkdServiceLoginSettings(socialSettings, genericTestServer, testOPServer);

        serviceAccountToken = stubbedServiceAccountToken;

        Log.info(thisClass, "setup", "service.account.token: " + serviceAccountToken);

        genericTestServer.addIgnoredServerException(SocialMessageConstants.CWWKG0033W_ATTRIBUTE_VALUE_NOT_FOUND);

    }

}
