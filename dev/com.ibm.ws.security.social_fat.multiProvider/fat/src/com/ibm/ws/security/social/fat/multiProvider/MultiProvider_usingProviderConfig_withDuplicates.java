/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.multiProvider;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.social.fat.multiProvider.commonTests.Social_MultiProvider_BasicTests;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 *
 **/
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class MultiProvider_usingProviderConfig_withDuplicates extends Social_MultiProvider_BasicTests {

    public static Class<?> thisClass = MultiProvider_usingProviderConfig_withDuplicates.class;

    @BeforeClass
    public static void setUp() throws Exception {
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.social", "server_multiProvider_usingProviderConfig_withDups.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setGenericVSSpeicificProviderFlags(ProviderConfig);

        //        runConfigType = ProviderConfig;
        // using provider specific configs - the redirect will use the generic provider login name (no extension necessary)
        nameExtender = "2";
    }

}
