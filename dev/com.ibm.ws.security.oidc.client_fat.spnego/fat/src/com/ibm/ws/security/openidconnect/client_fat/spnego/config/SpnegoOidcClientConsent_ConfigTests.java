/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.client_fat.spnego.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCConstants;

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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
public class SpnegoOidcClientConsent_ConfigTests extends SpnegoGenericOidcClientConsent_ConfigTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = SpnegoOidcClientConsent_ConfigTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(SpnegoOIDCConstants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Start the OIDC OP server
        //SPECIFY if OP or RP is going to be created.
        testOPServer = commonSetUpOPServer("com.ibm.ws.security.openidconnect.client-1.0_fat.spnego.op", "op_server_orig_no_spnego_config.xml",
                                           SpnegoOIDCConstants.OIDC_OP,
                                           SpnegoOIDCConstants.NO_EXTRA_APPS,
                                           SpnegoOIDCConstants.DO_NOT_USE_DERBY, SpnegoOIDCConstants.NO_EXTRA_MSGS);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUpRPServer("com.ibm.ws.security.openidconnect.client-1.0_fat.spnego.rp", "rp_server_orig.xml",
                                           SpnegoOIDCConstants.OIDC_RP, apps,
                                           SpnegoOIDCConstants.DO_NOT_USE_DERBY,
                                           SpnegoOIDCConstants.NO_EXTRA_MSGS, SpnegoOIDCConstants.OPENID_APP, SpnegoOIDCConstants.IBMOIDC_TYPE);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript
        testOPServer.addIgnoredServerException("CWWKS4312E");
        testOPServer.addIgnoredServerException("CWWKS4313E");
        test_GOOD_LOGIN_ACTIONS = SpnegoOIDCConstants.GOOD_OIDC_LOGIN_ACTIONS_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = SpnegoOIDCConstants.GOOD_OIDC_POST_LOGIN_ACTIONS_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = SpnegoOIDCConstants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS_CONSENT;
        test_FinalAction = SpnegoOIDCConstants.GET_RP_CONSENT;
        testSettings.setFlowType(SpnegoOIDCConstants.RP_FLOW);
    }

}
