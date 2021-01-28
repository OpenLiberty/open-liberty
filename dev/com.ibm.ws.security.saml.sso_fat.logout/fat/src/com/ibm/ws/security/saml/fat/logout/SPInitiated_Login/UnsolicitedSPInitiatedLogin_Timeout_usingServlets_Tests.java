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
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ShibbolethHelpers;
import com.ibm.ws.security.saml.fat.logout.common.TimeoutTests;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;

import componenttest.annotation.ExpectedFFDC;
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
public class UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests extends TimeoutTests {

    private static final Class<?> thisClass = UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        flowType = SAMLConstants.UNSOLICITED_SP_INITIATED;

        setCookieSettings(CookieType.SPCOOKIES);
        testUsers = new Testusers(chooseUsers()); // randomly chooses either the same or different users to use for this instance

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = getDefaultSAMLStartMsgs();

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SAMLConstants.SAML_DEMO_APP);
        extraApps.add(SAMLConstants.SAML_CLIENT_APP);

        HashMap<String, String> varMap = new HashMap<String, String>();
        varMap.put("xxx_IdpSessionTimeout_xxx", "PT30S");

        ShibbolethHelpers.ShibbolethServerVars[] shibUpdateVars = { shibbolethHelpers.new ShibbolethServerVars("conf", "idp.properties", varMap) };

        // the config filenames are the same for server 1 and 2, but their content is just a little different (and they live in different sub-directories)
        serverMasterConfig = "server_SPLogoutFalse" + cookieInfo.getCookieFileExtension() + "_unsolicited.xml";
        serverOtherConfig = "server_SPLogoutTrue" + cookieInfo.getCookieFileExtension() + "_unsolicited.xml";

        startSPWithIDPServer("com.ibm.ws.security.saml.sso_fat.logout", serverMasterConfig, SAMLConstants.SAML_SERVER_TYPE, extraMsgs, extraApps, true, null, null, shibUpdateVars);

        testSAMLServer.addIgnoredServerException(SAMLMessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);

        // setting this will prevent the config restoration between tests
        // That means NONE or ALL Tests have to have a reconfig to make sure they're using the
        // config that is correct for them - we will reconfig in every test - it may save 2 or 3 reconfigs
        testSAMLServer.setRestoreServerBetweenTests(false);

    }

    @Test
    public void UnsolicitedSPInitiatedLogin_Timeout_usingServlets_TestsIDPInitiated_LogoutUrl_IDPSessionTimeout() throws Exception {

        testSAMLServer.reconfigServer(serverMasterConfig, _testName, null, SAMLConstants.JUNIT_REPORTING);
        test_logoutAfterIDPSessionExpires(SAMLConstants.UNSOLICITED_SP_INITIATED, SAMLConstants.IDP_INITIATED, LOGOUT_INVOLVES_IDP, null);
    }

    @Test
    public void UnsolicitedSPInitiatedLogin_Timeout_usingServlets_TestsIDPInitiated_LogoutUrl_SPSessionTimeout() throws Exception {

        testSAMLServer.reconfigServer(serverMasterConfig, _testName, null, SAMLConstants.JUNIT_REPORTING);
        test_logoutAfterSAMLTokenExpires(SAMLConstants.UNSOLICITED_SP_INITIATED, SAMLConstants.IDP_INITIATED, LOGOUT_INVOLVES_IDP, null);
    }

    @Test
    public void UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests_servletRequestLogout_spLogoutFalse_IDPSessionTimeout() throws Exception {

        testSAMLServer.reconfigServer(serverMasterConfig, _testName, null, SAMLConstants.JUNIT_REPORTING);
        test_logoutAfterIDPSessionExpires(SAMLConstants.UNSOLICITED_SP_INITIATED, SAMLConstants.HTTPSERVLET_INITIATED, LogoutStaysInSPOnly, null);
    }

    @Test
    public void UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests_servletRequestLogout_spLogoutFalse_SPSessionTimeout() throws Exception {

        testSAMLServer.reconfigServer(serverMasterConfig, _testName, null, SAMLConstants.JUNIT_REPORTING);
        test_logoutAfterSAMLTokenExpires(SAMLConstants.UNSOLICITED_SP_INITIATED, SAMLConstants.HTTPSERVLET_INITIATED, LogoutStaysInSPOnly, null);
    }

    @Mode(TestMode.LITE)
    @Test
    public void UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests_servletRequestLogout_spLogoutTrue_IDPSessionTimeout() throws Exception {

        String[] flowOverride = { SAMLConstants.PERFORM_SP_LOGOUT, SAMLConstants.PROCESS_LOGOUT_REQUEST, SAMLConstants.PROCESS_LOGOUT_CONTINUE, SAMLConstants.PROCESS_LOGOUT_CONTINUE2 };
        testSAMLServer.reconfigServer(serverOtherConfig, _testName, null, SAMLConstants.JUNIT_REPORTING);
        test_logoutAfterIDPSessionExpires(SAMLConstants.UNSOLICITED_SP_INITIATED, SAMLConstants.HTTPSERVLET_INITIATED, LOGOUT_INVOLVES_IDP, flowOverride);
    }

    @ExpectedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void UnsolicitedSPInitiatedLogin_Timeout_usingServlets_Tests_servletRequestLogout_spLogoutTrue_SPSessionTimeout() throws Exception {

        String[] flowOverride = { SAMLConstants.PERFORM_SP_LOGOUT };
        testSAMLServer.reconfigServer(serverOtherConfig, _testName, null, SAMLConstants.JUNIT_REPORTING);
        test_logoutAfterSAMLTokenExpires(SAMLConstants.UNSOLICITED_SP_INITIATED, SAMLConstants.HTTPSERVLET_INITIATED, LOGOUT_INVOLVES_IDP, flowOverride);
    }

}
