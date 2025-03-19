/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginJSPClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLFormLoginClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/*
 * This class drive the form login testcases that will be run with Servlet 3.0
 * and the default HTTP protocol version
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FormLoginJSPTest extends CommonFormLoginJSPTest {
    private final static String DEFAULT_CONFIG_FILE = "formlogin.server.orig.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.formlogin");
    private static String LDAP_REALM_CONFIG_FILE = "formloginLdapRealm.xml";
    private static String URL_REALM_CONFIG_FILE = "formloginUrlRealm.xml";
    private static Class<?> myLogClass = FormLoginJSPTest.class;
    private static FormLoginClient myClient;
    private static SSLFormLoginClient mySSLClient;
    private static int updateCount = 1;

    /*
     * setUp() method put the servlet 3.0 server.xml file in place.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        myServer.setServerConfigurationFile(DEFAULT_CONFIG_FILE);
        myServer.addInstalledAppForValidation("formlogin");

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "formlogin.war");

        LDAPUtils.addLDAPVariables(myServer);
        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2851I"));

        myClient = new FormLoginJSPClient(myServer, FormLoginClient.DEFAULT_JSP_NAME, FormLoginClient.DEFAULT_JSP_CONTEXT_ROOT);
        mySSLClient = new SSLFormLoginClient(myServer, SSLFormLoginClient.DEFAULT_JSP_NAME, SSLFormLoginClient.DEFAULT_JSP_CONTEXT_ROOT);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
    }

    public FormLoginJSPTest() {
        super(myServer, myLogClass, myClient, mySSLClient);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Log in with a valid complex userId with special characters and password that is a member of the Employee role.
     * <LI>Update the server configuration to test different realm formats.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logout with logout form.
     * <LI> Attempt to go to the servlet again after logging out.
     * <LI> User will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Override
    @Test
    public void testMultipleRealms_ComplexUserSpecialCharsAuthorized() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        myClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, complexUserSpecialChars, complexUserPassword);

        setServerConfiguration(myServer, LDAP_REALM_CONFIG_FILE);
        myClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, complexUserSpecialChars, complexUserPassword);

        setServerConfiguration(myServer, URL_REALM_CONFIG_FILE);
        myClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, complexUserSpecialChars, complexUserPassword);

        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        myClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, complexUserSpecialChars, complexUserPassword);
    }

}
