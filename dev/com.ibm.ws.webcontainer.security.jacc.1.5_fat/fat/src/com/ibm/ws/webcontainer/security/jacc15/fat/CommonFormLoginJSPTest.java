/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

 This class is the location of the all the form login testcases.  The testcases
 may be setup differently.  Test may be run for:
 1. Servlet Spec 3.0 with HTTP version 1.1
 2. Servlet Spec 3.1 with HTTP version 1.1
 3. Servlet Spec 3.1 with HTTP version 1.0

 */

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.SSLFormLoginClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CommonFormLoginJSPTest extends CommonServletTestScenarios {
    private final static String DEFAULT_CONFIG_FILE = "formlogin.server.orig.xml";

    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static String LDAP_REALM_CONFIG_FILE = "formloginLdapRealm.xml";
    private static String URL_REALM_CONFIG_FILE = "formloginUrlRealm.xml";
    private static Class<?> myLogClass = CommonFormLoginJSPTest.class;
    private static LibertyServer myServer;
    private static FormLoginClient myClient;
    private static SSLFormLoginClient mySSLClient;
    private static int updateCount = 1;

    public CommonFormLoginJSPTest(LibertyServer server, Class<?> logClass,
                                  FormLoginClient client,
                                  SSLFormLoginClient sslClient) {
        super(server, myLogClass, client, sslClient);
        myServer = server;
        myClient = client;
        mySSLClient = sslClient;
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
     * <LI> Successful logout with logout form.
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
     * <LI> Successful logout with logout form, which redirects to login page
     * <LI>
     * <LI>Note: This test uses httpunit since httpclient does not support form logout with 2 logout forms
     * </OL>
     */
    @Mode(TestMode.LITE)
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
     * <LI>Log in with a valid userId and password.
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

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for form login with a user that does not have permission to access this protected servlet.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testSimpleNoRole_ComplexUserUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, noRoleComplexUser, complexUserPassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected with only the Employee role and configured for form login.
     * <LI> Log in with a valid complex userId and password that is a member of the Employee role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testEmployeeAuthzConstraints_EmployeeComplexUserAuthorized() throws Exception {
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, complexEmployeeUser, complexUserPassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, complexEmployeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet protected with only the Employee role and configured for form login.
     * <LI> Log in with a valid complex userId and password that is NOT a member of the Employee role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testEmployeeAuthzConstraints_ManagerComplexUserUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE, complexManagerUser, complexUserPassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Log in with a valid complex userId and password that belongs to a group associated with an authorized role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testSimpleEmployeeRoleByGroup_ComplexUserAuthorized() throws Exception {
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, complexEmployeeGroupUser, complexUserPassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, complexEmployeeGroupUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Log in with a valid complex userId and password that belongs to a group associated with a role NOT authorized for this servlet.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> Authorization denied, 403, to the protected resource.
     * </OL>
     */
    @Test
    public void testSimpleUnauthorizedRoleByGroup_ComplexUserUnauthorized() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   client.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, complexUnauthorizedGroupUser, complexUserPassword));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Log in with a valid complex userId with special characters and password that is a member of the Employee role.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * </OL>
     */
    @Test
    public void testSimpleEmployeeRole_ComplexUserSpecialCharsAuthorized() throws Exception {
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, complexUserSpecialChars, complexUserPassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, complexUserSpecialChars, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
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

    /**
     * This method is used to set the server.xml
     */
    public static void setServerConfiguration(LibertyServer server,
                                              String serverXML) throws Exception {
        if (!serverConfigurationFile.equals(serverXML)) {
            System.out.println("setServerConfigurationFile to : " + serverXML);
            // Update server.xml
            Log.info(myLogClass, "setServerConfiguration", "setServerConfigurationFile to : " + serverXML);
            server.setMarkToEndOfLog();
            server.setServerConfigurationFile(serverXML);
            server.waitForMultipleStringsInLog(updateCount++, "CWWKG0017I: The server configuration was successfully updated");
            serverConfigurationFile = serverXML;
        }
    }
}
