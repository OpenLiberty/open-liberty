/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.claimPropagation.OIDC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.claimPropagation.CommonTests.BasicClaimPropagationTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCBasicClaimPropagationTestss extends BasicClaimPropagationTests {

    private static final Class<?> thisClass = OIDCBasicClaimPropagationTestss.class;
    static HashMap<String, String> bootstrapProps = new HashMap<String, String>();

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(WithRegistry_withUser))
            .andWith(new SecurityTestRepeatAction(WithRegistry_withoutUser))
            .andWith(new SecurityTestRepeatAction(WithoutRegistry));

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(Constants.APP_FORMLOGIN);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testExternalOPServer = commonSetUp(ExternalOPServerName, "server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        Log.info(thisClass, "setupBeforeTest", RepeatTestFilter.getRepeatActionsAsString());
        Log.info(thisClass, "setupBeforeTest", "Without: " + Boolean.toString(RepeatTestFilter.isRepeatActionActive(WithoutRegistry)));
        Log.info(thisClass, "setupBeforeTest", "With: " + Boolean.toString(RepeatTestFilter.isRepeatActionActive(WithoutRegistry)));
        if (RepeatTestFilter.isRepeatActionActive(WithoutRegistry)) {
            Log.info(thisClass, "setupBeforeTest", "Starting Intermediate OP without a registry");
            testOPServer = commonSetUp(OPServerName, "server_withoutRegistry.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        } else {
            if (RepeatTestFilter.isRepeatActionActive(WithRegistry_withUser)) {
                Log.info(thisClass, "setupBeforeTest", "Starting Intermediate OP with a registry that does have testuser");
                testOPServer = commonSetUp(OPServerName, "server_withRegistry_withTestUser.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
            } else {
                Log.info(thisClass, "setupBeforeTest", "Starting Intermediate OP with a registry that does NOT have testuser");
                testOPServer = commonSetUp(OPServerName, "server_withRegistry_withoutTestUser.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
            }
        }
        testRPServer = commonSetUp(RPServerName, "server_orig.xml", Constants.OIDC_RP, extraApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testSettings.setFlowType(Constants.RP_FLOW);

    }

}
