/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSSkipIfJWTToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.MapToUserRegistryWithRegMismatch2ServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.LDAPUtils;

// See test description in CommonTests test MapToUserRegistryWithRegMismatch2ServerTests.

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCMapToUserRegistryWithRegMismatch2ServerTests extends MapToUserRegistryWithRegMismatch2ServerTests {

    private static final Class<?> thisClass = OIDCMapToUserRegistryWithRegMismatch2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        /*
         * These tests have not been configured to run with the local LDAP server.
         */
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OIDC_OP);

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add(Constants.HELLOWORLD_SERVLET);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "Initialized tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testOPServer = commonSetUp(OPServerName, "server_orig_maptest_ldap.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(RSServerName, "server_orig_maptest_mismatchregistry.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OIDC_OP, true, true, tokenType, certType);

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;
        //       goodActions = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS ;
        goodActions = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld");
        // Initial user settings for default user. Individual tests override as needed.
        testSettings.setAdminUser("oidcu1");
        testSettings.setAdminPswd("security");
        testSettings.setGroupIds("RSGroup");
    }

    /**
     * Test Purpose: Verify that when the subject is created for a user testuser, which is not in any group on the OP, but is in
     * the
     * required Manager group in the RS user registry, that access to the RS protected resource is allowed and the subject is
     * created
     * based on RS registry with testuser (in group RSGroup). The user testuser is in the Manager role in the appliation-bnd.
     * Note: This test does not apply for JWT token because the JWT token does not contain the given_name claim.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject ""
     * <LI>userIdentifier - given_name
     * </OL>
     * <P>
     * Expected Results: The subject is created with the given_name claim (profile scope) with the realm and groups from the RS
     * user registry.
     * <OL>
     * WSCredential SecurityName=oidcu1First WSCredential RealmName=RSTestRealm WSCredential
     * RealmSecurityName=RSTestRealm/oidcu1First WSCredential UniqueSecurityName=oidcu1First WSCredential groupIds=[] WSCredential
     * accessId=user:RSTestRealm/oidcu1First
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void MapToUserRegistryWithRegMismatch2ServerTest_given_name_Userinfo_AccessRSResource() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile email");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testClaimUserinfo");
        generalPositiveTest(updatedTestSettings, defaultBasicRPRealm, defaultMismatchGivenNameUser, defaultEmptyGroup, null, null);

    }
}
