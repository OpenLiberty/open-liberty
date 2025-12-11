/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests.genericWebClientCredentialTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OAuthWebClientCredentialTest extends genericWebClientCredentialTest {

    private static final Class<?> thisClass = OAuthWebClientCredentialTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for 
        // we should wait for any providers that this test requires

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, Constants.NO_EXTRA_MSGS, Constants.OP_SAMPLE_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, Constants.NO_EXTRA_MSGS, Constants.OP_CLIENT_APP, Constants.OAUTH_OP);
        TestServer.addTestApp(extraApps, Constants.NO_EXTRA_MSGS, Constants.OP_TAI_APP, Constants.OAUTH_OP);

        testSettings = new TestSettings();
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.server-1.0_fat", "server_clientcred_oauth.xml", Constants.OAUTH_OP, extraApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS);

        testSettings.setAdminUser(null);
        testSettings.setAdminPswd(null);
        testSettings.setClientID("client01");
        testSettings.setClientSecret("secret1234");
        testSettings.setScope("openid");
        testSettings.setFirstClientURL(testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/clientcred.jsp");
        testSettings.setFirstClientUrlSSL(testOPServer.getHttpsString() + "/" + Constants.OAUTHCLIENT_APP + "/clientcred.jsp");

    }
}
