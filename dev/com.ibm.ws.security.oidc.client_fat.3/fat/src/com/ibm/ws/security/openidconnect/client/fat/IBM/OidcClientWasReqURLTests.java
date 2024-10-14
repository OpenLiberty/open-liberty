/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.ws.security.SSO.clientTests.WasReqUrl.ClientWasReqURLTests;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 * This is the test class that will validate the behavior with various settings of
 * <webAppSecurity wasReqURLRedirectDomainNames= /> and the value of the WasReqURLOidc cookie.
 * Tests added for Issue 14692
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcClientWasReqURLTests extends ClientWasReqURLTests {

    public static Class<?> thisClass = OidcClientWasReqURLTests.class;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    public enum ExpectedResult {
        SUCCESS, INVALID_COOKIE, EXCEPTION, MISSING_COOKIE
    }

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op.wasReqUrl", opServerConfig, Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp.wasReqUrl", "rp_server_wasReqUrl_notSet.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // speed up the tests by not restoring the config between tests - each test will config the server that it needs (cut the time in half)
        testRPServer.setRestoreServerBetweenTests(false);

        testSettings.setFlowType(Constants.RP_FLOW);
        clientNameRoot = "rp_";
    }

}
