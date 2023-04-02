/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.spnego.app.token;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RevokeAppTokensWithAccessTokenUsingLocalStoreTests extends RevokeAppTokensTests {

    private static final Class<?> thisClass = RevokeAppTokensWithAccessTokenUsingLocalStoreTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add(MessageConstants.CWWKS1631I_OIDC_ENDPOINT_SERVICE_ACTIVATED);

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);

        testOPServer = commonSetUpOPServer(AppTokensOPServerName, "server_orig.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs);

        testOPServer.addIgnoredServerExceptions("SRVE8115W", "SRVE8094W", "SRVE8114W", "SESN0066E", "CWWKS1497E");

        flowType = Constants.WEB_CLIENT_FLOW;

        // override some of the default OIDC test settings (these tests use different clients, apps, ...)
        setAppTokensDefaultsForTests();
    }

}
