/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests.TokenMappingResolverGenericTest;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCTokenMappingResolverGenericTest extends TokenMappingResolverGenericTest {

    private static final Class<?> thisClass = OIDCTokenMappingResolverGenericTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_SERVLET);
            }
        };

        testSettings = new TestSettings();

        // Start the Generic/App Server
        //genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rs", "rs_server_orig.xml", Constants.GENERIC_SERVER, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        // Start the OIDC OP server

        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.op", "op_server_for_user_feature.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS);
        userFeatureInstaller = new com.ibm.ws.security.openidconnect.client.fat.jaxrs.utils.OAuth20TokenMapperFeatureInstaller();
        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rp", "rp_server_with_user_feature.xml", Constants.OIDC_RP, apps,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
        //testSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");
        testSettings.setTestURL(testRPServer.getHttpsString() + "/helloworld/rest/helloworld");

    }

}
