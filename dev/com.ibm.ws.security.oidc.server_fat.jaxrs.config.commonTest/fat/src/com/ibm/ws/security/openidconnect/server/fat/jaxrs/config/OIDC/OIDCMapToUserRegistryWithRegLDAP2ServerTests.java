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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSSkipIfJWTToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.MapToUserRegistryWithRegLDAP2ServerTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;
import componenttest.topology.utils.LDAPUtils;

// See test description in CommonTests test MapToUserRegistryWithRegLDAP2ServerTests.

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCMapToUserRegistryWithRegLDAP2ServerTests extends MapToUserRegistryWithRegLDAP2ServerTests {

    private static final Class<?> thisClass = OIDCMapToUserRegistryWithRegLDAP2ServerTests.class;

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
        genericTestServer = commonSetUp(RSServerName, "server_orig_maptest_withLdap.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, Constants.OIDC_OP, true, true, tokenType, certType);

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        flowType = Constants.WEB_CLIENT_FLOW;
        //       goodActions = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS ;
        goodActions = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld");

    }

    /**
     * Test Purpose: Verify that when the subject is created based on email claim (oidcu1@ibm.com) and that user oidcu1@ibm.com
     * does not exist in the RS user registry that an error 403 Authentication failed results. Note: This test is not valid when a
     * JWT token
     * is used because the JWT token does not include the email claim.
     * <OL>
     * <LI>mapIdentityToRegistryUser True
     * <LI>userIdentityToCreateSubject - not specified
     * <LI>uniqueUserIdentifier="email"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>403 Authentication failed
     * <LI>Message in RP server messaages.log: CWWKS1106A: Authentication did not succeed for the user ID oidcu1@ibm.com. An
     * invalid user ID was specified
     * </OL>
     **/
    @AllowedFFDC("com.ibm.ws.security.registry.EntryNotFoundException")
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void MapToUserRegistryWithRegLDAP2ServerTests_EmailUserIdentifier_NotInRSRegistry_AuthenticationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid email");
        updatedTestSettings.setAdminUser("oidcu1");
        updatedTestSettings.setAdminPswd("security");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testEmailUserinfo");
        generalNegativeTest(updatedTestSettings, Constants.FORBIDDEN_STATUS, FAILED_TO_FIND_MESSAGE, CWWKS1106A_AUTHENTICATION_FAILED + ".*oidcu1@ibm.com");

    }

    /**
     * Test Purpose: Verify when userIdentifier is specified as a claim (address) which is not the correct type for creating the
     * subject
     * that an error 401 occurs. Note: This test is not valid when JWT token is used because it does not contain an address claim.
     * <OL>
     * <LI>mapToUserRegistry - true
     * <LI>userIdentifier - not specified
     * <LI>userIdentityToCreateSubject - address
     * <LI>others default
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>401 error
     * <LI>Message in RS messages.log: CWWKS1730E: The resource server failed the authentication request because the data type of
     * the [address] claim in the access token associated with the [userIdentityToCreateSubject] configuration attribute is not
     * valid.
     *
     * </OL>
     **/
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void MapToUserRegistryWithRegLDAP2ServerTests_AddressUserIdentifier_NotValidForSubjectCreation_AuthenticationFailed() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid address");
        updatedTestSettings.setAdminUser("oidcu1");
        updatedTestSettings.setAdminPswd("security");
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld_testUserAddress");
        if (Constants.SUPPORTED.equals(updatedTestSettings.getInboundProp())) {
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, null, null);
        } else {
            generalNegativeTest(updatedTestSettings, Constants.UNAUTHORIZED_STATUS, FAILED_TO_FIND_MESSAGE, MessageConstants.CWWKS1730E_UNABLE_TO_AUTHENTICATE_BADCLAIMTYPE + ".*\\[address\\]");
        }

    }
}
