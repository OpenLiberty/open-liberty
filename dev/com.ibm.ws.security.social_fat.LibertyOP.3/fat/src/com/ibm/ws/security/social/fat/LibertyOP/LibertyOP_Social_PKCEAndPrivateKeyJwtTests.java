/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

// TODO - Client tests
package com.ibm.ws.security.social.fat.LibertyOP;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.SSO.clientTests.PKCEAndPrivateKeyJwt.PKCEAndPrivateKeyJwtClientTests;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run Private Key Jwt client tests using oidcLogin social client and an OP to provide
 * authorization functionality. PKCE and Private Key Jwt function are independent - just testing to prove that.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class LibertyOP_Social_PKCEAndPrivateKeyJwtTests extends PKCEAndPrivateKeyJwtClientTests {

    public static Class<?> thisClass = LibertyOP_Social_PKCEAndPrivateKeyJwtTests.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        useLdap = false;

        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.social_fat.LibertyOP.op.pkceAndPrivateKeyJwt", "op_server_PKCEAndPrivateKeyJwt.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // Start the Social client server and setup default values
        clientServer = commonSetUp("com.ibm.ws.security.social_fat.LibertyOP.social.pkceAndPrivateKeyJwt", "server_LibertyOP_PKCEAndPrivateKeyJwt.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testSettings.setFlowType(SocialConstants.SOCIAL);
        testSettings.setTokenEndpt(clientServer.getHttpsString() + "/PrivateKeyJwtTokenEndpoint/token");

    }

}
