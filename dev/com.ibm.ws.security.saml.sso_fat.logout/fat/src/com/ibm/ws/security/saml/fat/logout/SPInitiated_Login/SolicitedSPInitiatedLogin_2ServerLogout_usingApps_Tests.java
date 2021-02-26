/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.fat.logout.SPInitiated_Login;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.saml.fat.logout.common.TwoServerLogoutTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests extends TwoServerLogoutTests {

    private static final Class<?> thisClass = SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.SOLICITED_SP_INITIATED;
        // set cookie variables based on the types of cookies we'll be using (LTPA, SP Cookies, or Mixed (LTPA and SP Cookies)
        setCookieSettings(CookieType.SPCOOKIES);
        testUsers = new Testusers(UserType.SAME);

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_DEMO_APP);
        //        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        // the config filenames are the same, but their content is just a little different (and they live in different sub-directories)
        server1MasterConfig = "server_SPLogoutFalse" + cookieInfo.getCookieFileExtension() + "_multiApp.xml";
        server2MasterConfig = "server_SPLogoutFalse" + cookieInfo.getCookieFileExtension() + "_multiApp.xml";
        server1OtherConfig = "server_SPLogoutTrue" + cookieInfo.getCookieFileExtension() + "_multiApp.xml";
        server2OtherConfig = "server_SPLogoutTrue" + cookieInfo.getCookieFileExtension() + "_multiApp.xml";

        start2SPWithIDPServer("com.ibm.ws.security.saml.sso_fat.logout", server1MasterConfig, "com.ibm.ws.security.saml.sso_fat.logout.server2", server2MasterConfig, SAMLConstants.SAML_SERVER_TYPE, extraMsgs, extraApps, true, null, null);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        testSAMLServer2.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

        // update the context root to allow use of unique apps instead of just unique servlets
        testSettings = updateContextRoot(testSettings);

        // setting this will prevent the config restoration between tests
        // That means NONE or ALL Tests have to have a reconfig to make sure they're using the
        // config that is correct for them - we will reconfig in every test - it may save 2 or 3 reconfigs
        testSAMLServer.setRestoreServerBetweenTests(false);
        testSAMLServer2.setRestoreServerBetweenTests(false);

    }

    @Test
    public void SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests_ibmSecurityLogout_spLogoutFalse_sameUser() throws Exception {

        testUsers = new Testusers(UserType.SAME);
        reconfigServers(server1MasterConfig, server2MasterConfig);
        test_logout_with_multipleSPs_on_2Servers(SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.IBMSECURITYLOGOUT_INITIATED, LogoutStaysInSPOnly);

    }

    @Test
    public void SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests_ibmSecurityLogout_spLogoutFalse_differentUsers() throws Exception {

        testUsers = new Testusers(UserType.DIFFERENT);
        reconfigServers(server1MasterConfig, server2MasterConfig);
        test_logout_with_multipleSPs_on_2Servers(SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.IBMSECURITYLOGOUT_INITIATED, LogoutStaysInSPOnly);

    }

    @Test
    public void SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests_ibmSecurityLogout_spLogoutFalse_tryToUseSPServer2CookieAfterLogout() throws Exception {

        testUsers = new Testusers(UserType.SAME);
        reconfigServers(server1MasterConfig, server2MasterConfig);
        test_usingCookieAfterLogout(SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.IBMSECURITYLOGOUT_INITIATED, LogoutStaysInSPOnly);

    }

    @Test
    public void SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests_ibmSecurityLogout_spLogoutTrue_sameUser() throws Exception {

        reconfigServers(server1OtherConfig, server2OtherConfig);
        test_logout_with_multipleSPs_on_2Servers(SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.IBMSECURITYLOGOUT_INITIATED, LOGOUT_INVOLVES_IDP);

    }

    @Mode(TestMode.LITE)
    @Test
    public void SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests_ibmSecurityLogout_spLogoutTrue_differentUsers() throws Exception {

        testUsers = new Testusers(UserType.DIFFERENT);
        reconfigServers(server1OtherConfig, server2OtherConfig);
        test_logout_with_multipleSPs_on_2Servers(SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.IBMSECURITYLOGOUT_INITIATED, LOGOUT_INVOLVES_IDP);

    }

    @Test
    public void SolicitedSPInitiatedLogin_2ServerLogout_usingApps_Tests_ibmSecurityLogout_spLogoutTrue_tryToUseSPServer2CookieAfterLogout() throws Exception {

        reconfigServers(server1OtherConfig, server2OtherConfig);
        test_usingCookieAfterLogout(SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.IBMSECURITYLOGOUT_INITIATED, LOGOUT_INVOLVES_IDP);

    }

}
