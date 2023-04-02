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
import com.ibm.ws.security.social.fat.okdServiceLogin.commonTests.OKDServiceLogin_BasicTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * Basic OKD Service login tests using a fake/stubbed OpenShift user validation endpoint
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OKDServiceLogin_BasicTests_StubbedServer extends OKDServiceLogin_BasicTests {

    public static Class<?> thisClass = OKDServiceLogin_BasicTests_StubbedServer.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(OKDServiceLoginRepeatActions.basicTests_usingStub());

    @BeforeClass
    public static void setUp() throws Exception {

        stubbedTests = true;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET + "GoodGroup");
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET + "BadGroup");
        extraApps.add(SocialConstants.STUBBED_USER_VALIDATION_API_SERVLET);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".stubbedOKDServiceLogin.social", "server_OKDServiceLogin_basicTests_usingStubs.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setActionsForProvider(SocialConstants.OPENSHIFT_PROVIDER, SocialConstants.OAUTH_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, "server_OKDServiceLogin_basicTests_usingStubs");

        socialSettings = updateOkdServiceLoginSettings(socialSettings, genericTestServer);

        serviceAccountToken = stubbedServiceAccountToken;

        Log.info(thisClass, "setup", "service.account.token: " + serviceAccountToken);

    }

}
