/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oidc_social.backchannelLogout.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**********************************************************************************/
/****** TODO: We may need to add an OP for use with some of the sid tests ******/
/****** The validation should ensure that the sid is valid ******/
/****** We may also need to use update to use an OP for tests ******/
/**********************************************************************************/

/**
 * This test class contains tests that validate the proper behavior with varying client config settings (RP, RS, Social configs)
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
public class ClientConfigTests extends BackChannelLogoutCommonTests {

    // Repeat tests using the OIDC and Social endpoints
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC))
            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL));

    @BeforeClass
    public static void setUp() throws Exception {

        testSettings = new TestSettings();

        //        testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_httpMethod.xml", Constants.OIDC_RP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY,
        //                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
        //
        //        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
        //            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_httpMethod.xml", Constants.OIDC_RP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY,
        //                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
        //        } else {
        //            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_httpMethod.xml", Constants.GENERIC_SERVER, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY,
        //                    Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
        //
        //        }
        testSettings.setFlowType(Constants.RP_FLOW);
    }

    @Test
    public void ClientConfigTests_backchannelLogoutSupported_true_backchannelLogoutSessionRequired_false() throws Exception {

    }

    @Test
    public void ClientConfigTests_backchannelLogoutSupported_true_backchannelLogoutSessionRequired_true() throws Exception {

    }

    @Test
    public void ClientConfigTests_backchannelLogoutSupported_false_backchannelLogoutSessionRequired_false() throws Exception {

    }

    @Test
    public void ClientConfigTests_backchannelLogoutSupported_fakse_backchannelLogoutSessionRequired_true() throws Exception {

    }

}