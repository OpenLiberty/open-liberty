/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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

/**
 * Note:
 * 1. User registry:
 * At this time, the test uses users, passwords, roles, and groups predefined in server.xml as
 * test user registry.
 *
 * TODO:  use different user registry
 *
 * 2. The constraints (which servlets can be accessed by which user/group/role) are defined in web.xml
 *
 * 3. Note on *Overlap* test:
 * When there are more than one constraints applied to the same servlet, the least constraint will win,
 * e.g.,
 *   <auth-constraint id="AuthConstraint_5">
 <role-name>Employee</role-name>
 </auth-constraint>

 and

 <security-constraint id="SecurityConstraint_5">
 <web-resource-collection id="WebResourceCollection_5">
 <web-resource-name>Protected with overlapping * and Employee roles</web-resource-name>
 <url-pattern>/OverlapNoConstraintServlet</url-pattern>
 <http-method>GET</http-method>
 <http-method>POST</http-method>
 </web-resource-collection>
 <auth-constraint id="AuthConstraint_5">
 <role-name>*</role-name>
 </auth-constraint>
 </security-constraint>

 servlet OverlapNoConstraintServlet will allow access to all roles since
 the role = * (any role) and role =  Employee are combined and * will win.

 */

import static org.junit.Assert.assertNotNull;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.SSLFormLoginClient;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.CheckpointRule;
import componenttest.rules.repeater.CheckpointRule.ServerMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@CheckpointTest(alwaysRun = true)
public class FormLoginTest extends CommonServletTestScenarios {
    private static LibertyServer myServer;
    private static Class<?> myLogClass = FormLoginTest.class;
    private static FormLoginClient myClient;
    private static SSLFormLoginClient mySSLClient;

    @ClassRule
    public static CheckpointRule checkpointRule = new CheckpointRule()
                                                      .setConsoleLogName(FormLoginTest.class.getSimpleName() + ".log")
                                                      .setServerSetup(FormLoginTest::serverSetUp)
                                                      .setServerStart(FormLoginTest::serverStart)
                                                      .setServerTearDown(FormLoginTest::serverTearDown);

    public static LibertyServer serverSetUp(ServerMode mode) throws Exception {
        myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.formlogin");
        myServer.addInstalledAppForValidation("formlogin");

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "formlogin.war");

        LDAPUtils.addLDAPVariables(myServer);

        myClient = new FormLoginClient(myServer);
        mySSLClient = new SSLFormLoginClient(myServer);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
        return myServer;
    }

    public static void serverStart(ServerMode mode, LibertyServer server) throws Exception {
        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));

        if (myServer.getValidateApps() && !CheckpointRule.isActive()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(myServer);
        }
    }

    public static void serverTearDown(ServerMode mode, LibertyServer server) throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(myServer);
        }
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    public FormLoginTest() {
        super(myServer, myLogClass, myClient, mySSLClient);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password
     * <LI>Issue a logout and validate the cookie is cleared
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form.
     * <LI> Attempt to go to the servlet again after logging out,
     * <LI> user will have to log in again before accessing the protected servlet.
     * </OL>
     */
    @Test
    public void testFormLogin_LoginThenLogout() throws Exception {
        myClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, employeeUser, employeePassword);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access the form login servlet with a valid userId and password, then logout, which redirects to login page
     * <LI>Attempt to access the servlet again
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A form login when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> No challenge or login required to access the protected resource.
     * <LI> Successful logged out with logout form, which redirects to login page
     * <LI>
     * <LI>Note: This test uses httpunit since httpclient does not support form logout with 2 logout forms
     * </OL>
     */
    @Test
    public void testFormLogin_LoginThenLogoutRedirect() throws Exception {
        myClient.formLogout(LogoutOption.LOGOUT_TO_LOGIN_PAGE, employeeUser, employeePassword);
    }

    /**
     * Test the form login with a servlet executing logout method to validate the Servlet 3.0 spec
     * requirement that after logging out, access to a protected web resource requires re-authentication
     * and the getUserPrincipal, getRemoteUser and getAuthType methods return null.
     *
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> The servlet calls logout() and returns back to user
     * <LI> Response from the servlet with data from before and after the servlet logs out are correct
     * <LI> - The caller subject and public credential returns null after the logout
     * <LI> - LtpaToken2 in the cookie is empty
     * </OL>
     */
    @Test
    public void testFormLogOutMethod_ValidUserIdPassword() throws Exception {
        myClient.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE, employeeUser, employeePassword);
    }

}
