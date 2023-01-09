/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.MapToUserRegistryNoRegUserinfo2ServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCMapToUserRegistryNoRegUserinfo2ServerTests extends MapToUserRegistryNoRegUserinfo2ServerTests {

    private static final Class<?> thisClass = OIDCMapToUserRegistryNoRegUserinfo2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OIDC_OP);

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add(Constants.HELLOWORLD_SERVLET);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "Initialized tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testOPServer = commonSetUp(OPServerName, "server_orig_maptest_basic.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(RSServerName, "server_orig_maptest_noregistry_userinfo.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OIDC_OP, true, true, tokenType, certType);

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        targetISSEndpoint = "localhost:" + testOPServer.getHttpDefaultPort().toString() + "/" + Constants.OIDC_ROOT + "/endpoint/" + targetProvider + "/";
        flowType = Constants.WEB_CLIENT_FLOW;
        //       goodActions = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS ;
        goodActions = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld");
        defaultISSRealm = testOPServer.getHttpString() + "/" + Constants.OIDC_ROOT + "/endpoint/" + Constants.OIDCCONFIGSAMPLE_APP;

    }

}
