/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.OpenShift;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;

import com.ibm.ws.security.social.fat.commonTests.Social_BasicConfigTests_NoServerSSL;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 *
 **/
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OpenShift_BasicConfigTests_usingSocialConfig_noServerSSL extends Social_BasicConfigTests_NoServerSSL {

    public static Class<?> thisClass = OpenShift_BasicConfigTests_usingSocialConfig_noServerSSL.class;

    @BeforeClass
    public static void setUp() throws Exception {
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".OpenShift.social", "server_OpenShift_basicConfigTests_usingSocialConfig_noServerSSL.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setActionsForProvider(SocialConstants.OPENSHIFT_PROVIDER, SocialConstants.OAUTH_OP);

        setGenericVSSpeicificProviderFlags(GenericConfig, null);

        socialSettings = updateOpenShiftSettings(socialSettings);

        helpers.testSleep(10);

    }

}
