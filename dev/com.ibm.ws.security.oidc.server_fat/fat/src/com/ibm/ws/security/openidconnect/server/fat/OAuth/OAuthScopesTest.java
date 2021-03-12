/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.OAuth;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.CommonTests.GenericScopesTests;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OAuthScopesTest extends GenericScopesTests {

    private static final Class<?> thisClass = OAuthScopesTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for 
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        //extraMsgs.add("CWWKS1402I.*" + Constants.OAUTHCONFIGMEDIATOR_APP);

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OAUTH_OP);
        // not using Derby even thought it may be in the server.xml
        //TestServer.addTestApp(null, extraMsgs, Constants.OP_MEDIATOR_APP, Constants.OAUTH_OP);

        testSettings = new TestSettings();
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.server-1.0_fat", "server_scopes.xml", Constants.OAUTH_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs);

        targetProvider = Constants.OAUTHCONFIGSAMPLE_APP;
        goodActions = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;
        flowType = Constants.WEB_CLIENT_FLOW;

    }

}
