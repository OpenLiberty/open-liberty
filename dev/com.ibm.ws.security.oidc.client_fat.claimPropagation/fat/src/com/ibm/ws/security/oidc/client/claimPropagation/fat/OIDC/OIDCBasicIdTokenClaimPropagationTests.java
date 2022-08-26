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
package com.ibm.ws.security.oidc.client.claimPropagation.fat.OIDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.common.claimPropagation.fat.CommonTests.BasicIdTokenClaimPropagationTests;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCBasicIdTokenClaimPropagationTests extends BasicIdTokenClaimPropagationTests {

    private static final Class<?> thisClass = OIDCBasicIdTokenClaimPropagationTests.class;
    static HashMap<String, String> bootstrapProps = new HashMap<String, String>();

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(WithRegistry_withUser + "_rp"))
            .andWith(new SecurityTestRepeatAction(WithRegistry_withoutUser + "_rp"))
            .andWith(new SecurityTestRepeatAction(WithoutRegistry + "_rp"))
            .andWith(new SecurityTestRepeatAction(WithRegistry_withUser_implicit + "_rp"))
            .andWith(new SecurityTestRepeatAction(WithRegistry_withoutUser_implicit + "_rp"))
            .andWith(new SecurityTestRepeatAction(WithoutRegistry_implicit + "_rp"));

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(Constants.APP_FORMLOGIN);

        Log.info(thisClass, "setupBeforeTest", "actions: " + RepeatTestFilter.getRepeatActionsAsString());
        repeatAction = RepeatTestFilter.getRepeatActionsAsString(); // only really returns the current action
        if (repeatAction.contains(Constants.IMPLICIT_GRANT_TYPE)) {
            bootstrapProps.put("testGrantType", Constants.IMPLICIT_GRANT_TYPE);
        } else {
            bootstrapProps.put("testGrantType", Constants.AUTH_CODE_GRANT_TYPE);
        }
        setMiscBootstrapParms(bootstrapProps);

        testSettings = new TestSettings();
        testExternalOPServer = commonSetUp(ExternalOPServerName, "server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true);
        Log.info(thisClass, "setupBeforeTest", RepeatTestFilter.getRepeatActionsAsString());
        Log.info(thisClass, "setupBeforeTest", "Without: " + Boolean.toString(RepeatTestFilter.isRepeatActionActive(WithoutRegistry)));
        Log.info(thisClass, "setupBeforeTest", "With: " + Boolean.toString(RepeatTestFilter.isRepeatActionActive(WithoutRegistry)));

        if (repeatAction.contains(WithoutRegistry)) {
            Log.info(thisClass, "setupBeforeTest", "Starting Intermediate OP without a registry");
            testOPServer = commonSetUp(OPServerName, "server_withoutRegistry.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true);
        } else {
            if (repeatAction.contains(WithRegistry_withUser) || RepeatTestFilter.isRepeatActionActive(WithRegistry_withUser_implicit)) {
                Log.info(thisClass, "setupBeforeTest", "Starting Intermediate OP with a registry that does have testuser");
                testOPServer = commonSetUp(OPServerName, "server_withRegistry_withTestUser.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true);
            } else {
                Log.info(thisClass, "setupBeforeTest", "Starting Intermediate OP with a registry that does NOT have testuser");
                testOPServer = commonSetUp(OPServerName, "server_withRegistry_withoutTestUser.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true);
            }
        }

        testRPServer = commonSetUp(RPServerName, "server_orig.xml", Constants.OIDC_RP, extraApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true);

        testSettings.setFlowType(Constants.RP_FLOW);

        testSettings.setUserName("LDAPUser1");
        testSettings.setUserPassword("security");

        steps = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        loginStep = Constants.LOGIN_USER;

    }

}
