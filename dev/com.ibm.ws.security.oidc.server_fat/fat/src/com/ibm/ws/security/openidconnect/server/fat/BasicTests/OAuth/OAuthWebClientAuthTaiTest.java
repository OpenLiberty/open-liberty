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

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests.genericWebClientAuthTaiTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.LDAPUtils;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OAuthWebClientAuthTaiTest extends genericWebClientAuthTaiTest {

    private static final Class<?> thisClass = OAuthWebClientAuthTaiTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
    	/*
    	 * These tests have not been configured to run with the local LDAP server.
    	 */
    	Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for 
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1600I.*" + Constants.OIDCCONFIGSAMPLE_APP);
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();
        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OAUTH_OP);

        testSettings = new TestSettings();
        testSettings.setProviderType(Constants.OAUTH_OP);
        copyTaiFiles("com.ibm.ws.security.openidconnect.server-1.0_fat.tai");
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.server-1.0_fat.tai", "server_tai.xml", Constants.OAUTH_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs);
    }

}
