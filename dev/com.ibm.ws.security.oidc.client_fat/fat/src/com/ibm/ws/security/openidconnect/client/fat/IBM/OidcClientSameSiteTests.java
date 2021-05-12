/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SameSiteTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.CommonTests.SameSiteTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run basic OpenID Connect RP tests.
 * This test class extends GenericRPTests.
 * GenericRPTests contains common code for all RP tests.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcClientSameSiteTests extends SameSiteTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcClientSameSiteTests.class;

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
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_samesite.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_samesite.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testSettings.setFlowType(Constants.RP_FLOW);

        testRPServer.setRestoreServerBetweenTests(false);
        testOPServer.setRestoreServerBetweenTests(false);

        testOPServer.addIgnoredServerExceptions(new String[] { MessageConstants.CWWKG0011W_CONFIG_VALIDATION_FAILURE, MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE, MessageConstants.CWWKG0081W_CONFIG_VALIDATION_FAILURE, MessageConstants.CWWKG0083W_CONFIG_VALIDATION_FAILURE });
        testRPServer.addIgnoredServerExceptions(new String[] { MessageConstants.CWWKG0011W_CONFIG_VALIDATION_FAILURE, MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE, MessageConstants.CWWKG0081W_CONFIG_VALIDATION_FAILURE, MessageConstants.CWWKG0083W_CONFIG_VALIDATION_FAILURE });

        samesiteTestTools = new SameSiteTestTools(testOPServer, testRPServer, null, serverRefList);

    }

}
