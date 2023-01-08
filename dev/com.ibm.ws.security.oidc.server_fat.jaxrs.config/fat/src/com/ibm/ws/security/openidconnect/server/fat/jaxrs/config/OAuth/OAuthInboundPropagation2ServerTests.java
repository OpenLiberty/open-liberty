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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.InboundPropagation2ServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.LDAPUtils;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OAuthInboundPropagation2ServerTests extends InboundPropagation2ServerTests {

    private static final Class<?> thisClass = OAuthInboundPropagation2ServerTests.class;

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
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OAUTH_OP);

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add(Constants.HELLOWORLD_SERVLET);

        //        TestServer.addTestApp(extraApps2, null, Constants.OP_TAI_APP, Constants.OAUTH_OP);

        testSettings = new TestSettings();
        testOPServer = commonSetUp(OPServerName, "server_orig.xml", Constants.OAUTH_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs);
        genericTestServer = commonSetUp(RSServerName, "server_inboundProp_tests.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OAUTH_OP);

        targetProvider = Constants.OAUTHCONFIGSAMPLE_APP;
        targetISSEndpoint = "localhost:" + testOPServer.getHttpDefaultPort().toString() + "/" + Constants.OIDC_ROOT + "/endpoint/" + targetProvider;
        goodActions = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS;
        flowType = Constants.WEB_CLIENT_FLOW;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + Constants.HELLOWORLD_PROTECTED_RESOURCE);

        setRealmForValidationType(testSettings);
    }

}
