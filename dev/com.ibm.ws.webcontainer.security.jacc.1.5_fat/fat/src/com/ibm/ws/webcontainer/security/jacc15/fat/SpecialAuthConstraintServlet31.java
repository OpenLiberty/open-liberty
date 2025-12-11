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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLServletClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

/*
 * Testcase for Servlet 3.1 special authorization constraint "**" (all authenticated users).
 *
 * Some info from the Servlet 3.1 spec
 *      - Any call to isUserInRole with "*" must return false.
 *      - If the role-name of the security-role to be tested is “**”, isUserInRole must only return true
 *        if the user has been authenticated.
 *      - When the special role name "**" appears in an authorization constraint, it indicates that any
 *        authenticated user, independent of role, is authorized to perform the constrained requests.
 *      -
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SpecialAuthConstraintServlet31 extends CommonServletTestScenarios {
    private static String DEFAULT_CONFIG_FILE = "basicauthServlet31.server.orig.xml";
    private static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauthServlet31");
    private static Class<?> myLogClass = SpecialAuthConstraintServlet31.class;
    private static BasicAuthClient myClient;
    private static SSLBasicAuthClient mySSLClient;
    private static String appName = "basicauth";
    private static String serverConfigurationFile = DEFAULT_CONFIG_FILE;
    private static String CONFIG_WITH_ALL_AUTH = "basicauthServlet31-AllAuth.server.xml";

    private static CommonTestHelper testHelper = new CommonTestHelper();
    private static String authTypeBasic = "BASIC";
    private final static String OMISSION_BASIC_SERVLET = "/basicauth/StarStarOmissionBasic";
    private final static String OMISSION_COMPLEX_SERVLET = "/basicauth/StarStarOmissionComplex";
    private final static String ANY_ROLE = "*";
    private final static String ALL_AUTH_ROLE = "**";

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        /*
         * These tests have not been configured to run with the local LDAP server.
         */
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);

        myServer.setServerConfigurationFile(CONFIG_WITH_ALL_AUTH);

        JACCFatUtils.installJaccUserFeature(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war");

        myServer.addInstalledAppForValidation(appName);
        myServer.startServer(true);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaccFeature(myServer);
        }

        myClient = new BasicAuthClient(myServer);
        mySSLClient = new SSLBasicAuthClient(myServer);
        myClient.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);
    }

    protected static void verifyServerStartedWithJaccFeature(LibertyServer server) {
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen
    }

    public SpecialAuthConstraintServlet31() {
        super(myServer, myLogClass, myClient, mySSLClient);
    }

    /**
     * Pass-through constructor so ServerXMLOverrides* and XM*Bindings
     * tests can sub-class this class.
     *
     * @param server
     * @param logClass
     * @param client
     * @param sslClient
     */
    @SuppressWarnings("static-access")
    protected SpecialAuthConstraintServlet31(LibertyServer server, Class<?> logClass,
                                             ServletClient client,
                                             SSLServletClient sslClient,
                                             String appName) {
        super(server, logClass, client, sslClient);
        this.appName = appName;
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
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "*" false must be returned.
     * <LI> Login with a valid user id and password call the isUserInRole api
     * <LI> isUserInRole returns false.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStar_withCredentials() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SPECIAL_ALL_AUTH + "?role=" + ANY_ROLE, employeeUser, employeePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, ANY_ROLE, false));
    }

    /**
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "*" false must be returned.
     * <LI> Login to a servlet that has special role "*" auth constraint
     * <LI> isUserInRole api returns false.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStar_withNoRoles() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SPECIAL_ANY_ROLE_AUTH + "?role=" + ANY_ROLE, noRoleUser, noRolePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, ANY_ROLE, false));
    }

    /**
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "*" false must be returned.
     * <LI> Access an unprotected servlet isUserInRole api returns false.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStar_UnprotectedServlet() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT + "?role=" + ANY_ROLE);
        assertTrue("The response did not contain the expected isUserInRole(" + ANY_ROLE + ")",
                   response.contains("isUserInRole(" + "*" + "): " + "false"));
    }

    /**
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "**" true must be returned.
     * <LI> Login with a valid id and password for a user that is a member of a role
     * <LI> the isUserInRole api returns true.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarStar_withCredentials() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SPECIAL_ALL_AUTH + "?role=" + ALL_AUTH_ROLE, employeeUser,
                                                                                 employeePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, ALL_AUTH_ROLE, true));
    }

    /**
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "**" true must be returned.
     * <LI> Login with a valid id and password for a user that is not a member of any role the
     * <LI> isUserInRole api returns true.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarStar_withNoRole() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SPECIAL_ALL_AUTH + "?role=" + ALL_AUTH_ROLE, noRoleUser, noRolePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, ALL_AUTH_ROLE, true));
    }

    /**
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "**" true must be returned.
     * <LI> Login to a servlet that does not have "**" configured, the isUserInRole
     * <LI> api returns true.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarStar_ServletWithoutStarStar() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + ALL_AUTH_ROLE, employeeUser, employeePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, ALL_AUTH_ROLE, true));
    }

    /**
     * Verify the following Servlet 3.1 isUserInRole:
     * <OL>
     * <LI> When calling isUserInRole with role name of "**" true must be returned if the user is authenticated.
     * <LI> Access an unprotected servlet the isUserInRole api returns false because the user is unauthenticated.
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarStar_UnprotectedServlet() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessUnprotectedServlet(BasicAuthClient.UNPROTECTED_NO_SECURITY_CONSTRAINT + "?role=" + ALL_AUTH_ROLE);
        assertTrue("The response did not contain the expected isUserInRole(" + ALL_AUTH_ROLE + ")",
                   response.contains("isUserInRole(" + ALL_AUTH_ROLE + "): " + "false"));
    }

    /**
     * Verify the following Servlet 3.1 authorization constraint:
     * <OL>
     * <LI> Attempt to access a servlet protected with an authorization
     * constraint of ** and configured for basic authentication. Access should
     * be allowed to a user in a role that is authenticated.
     * <LI> Login with an valid userId and password.
     * <LI> Access is granted
     * </OL>
     */
    @Test
    public void testStarStarAuthzConstraintValidUser() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SPECIAL_ALL_AUTH, employeeUser, employeePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following Servlet 3.1 authorization constraint:
     * <OL>
     * <LI> Attempt to access a servlet protected with an authorization
     * constraint of ** and configured for basic authentication. Access should
     * be allowed to a user not in a role that is authenticated.
     * <LI> Login with an valid userId and password.
     * <LI> Access is granted
     * </OL>
     */
    @Test
    public void testStarStarAuthzConstraintValidUserNoRole() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SPECIAL_ALL_AUTH, noRoleUser, noRolePassword);
        assertTrue("Expected authorization to be granted",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following Servlet 3.1 authorization constraint:
     * <OL>
     * <LI> Attempt to access a servlet protected with an authorization
     * constraint of ** and configured for basic authentication. Access should
     * not be allowed to any user that is not authenticated.
     * <LI> Login with an invalid userId and password.
     * <LI> Access is denied.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> An invalid userId and password is not permitted.
     * </OL>
     */
    @Test
    public void testStarStarAuthzConstraintInvalidUserNotAuthorized() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        assertTrue("Expected access denied, but it was granted",
                   client.accessProtectedServletWithInvalidCredentials(BasicAuthClient.PROTECTED_SPECIAL_ALL_AUTH, invalidUser, invalidPassword));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToManager is mapped to Manager
     * <LI>Login with a user not mapped to any roles. Call isUserInRole with a
     * <LI>rolename of "*".
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(*) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarRole_NoRolesUser() throws Exception {
        String specifiedRole = "*";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE + "?role=" + specifiedRole, noRoleUser, noRolePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, specifiedRole, false));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password. Call isUserInRole with a
     * <LI>rolename of "**".
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(**) is true
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testIsUserInRoleStarStarRole_Employee() throws Exception {
        String specifiedRole = "**";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, "**", true));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToManager is mapped to Manager
     * <LI>Login with a user not mapped to any roles. Call isUserInRole with a
     * <LI>rolename of "**".
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(**) is true
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarStarRole_NoRolesUser() throws Exception {
        String specifiedRole = "**";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_ALL_ROLE + "?role=" + specifiedRole, noRoleUser, noRolePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, noRoleUser, NOT_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, specifiedRole, true));
    }

    /**
     * Verify the following Servlet 3.1:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /StarStarOmissionBasic, where all methods are protected except POST
     * <LI>Expected result: GET, CUSTOM - protected with AllAuthenticated role
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionBasic_GetWithEmployee() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_BASIC_SERVLET;
        String response = testHelper.accessGetProtectedServletWithAuthorizedCredentials(url, employeeUser, employeePassword, url, server);
        testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
    }

    /**
     * Verify the following Servlet 3.1:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /StarStarOmissionBasic, where all methods are protected except POST
     * <LI>Expected result: GET, CUSTOM - protected with AllAuthenticated role
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionBasic_CustomWithEmployee() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String methodName = "testHttpMethodOmissionBasic_CustomWithEmployee";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_BASIC_SERVLET;
        try {
            String response = testHelper.httpCustomMethodResponse(url, "CUSTOM", true, server.getHttpDefaultPort(), employeeUser, employeePassword);
            Log.info(logClass, methodName, "response: " + response);
            testHelper.verifyProgrammaticAPIValues(employeeUser, response, authTypeBasic);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed to access the URL " + url);
        }
    }

    /**
     * Verify the following Servlet 3.1:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /StarStarOmissionBasic, where all methods are protected except POST
     * <LI>Expected result: POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionBasic_PostUnprotected() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_BASIC_SERVLET;
        testHelper.accessPostUnprotectedServlet(url);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /StarStarOmissionComplex, where all methods are denied access except POST
     * <LI>Expected result: GET, CUSTOM - denied access
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionComplex_GetWithEmployee() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_COMPLEX_SERVLET;
        testHelper.accessGetProtectedServletWithInvalidCredentials(url, employeeUser, employeePassword, url, server);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /StarStarOmissionComplex, where all methods are denied access except POST
     * <LI>Expected result: GET, CUSTOM - denied access
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionComplex_CustomWithEmployee() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String methodName = "testHttpMethodOmissionComplex_CustomWithEmployee";

        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_COMPLEX_SERVLET;
        try {
            String response = testHelper.httpCustomMethodResponse(url, "CUSTOM", true, server.getHttpDefaultPort(), employeeUser, employeePassword);
            Log.info(logClass, methodName, "response: " + response);
            assertTrue("Expected 403 Forbidden not found", response.contains("403 Forbidden"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed to access the URL " + url);
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>http-method-omission specified in web.xml for /StarStarOmissionComplex, where all methods are denied access except POST
     * <LI>Expected result: POST - unprotected
     * </OL>
     */
    @Test
    public void testHttpMethodOmissionComplex_PostUnprotected() throws Exception {
        setServerConfiguration(myServer, DEFAULT_CONFIG_FILE);
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + OMISSION_COMPLEX_SERVLET;
        testHelper.accessPostUnprotectedServlet(url);
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
            myServer.waitForStringInLogUsingMark("CWWKG0017I: The server configuration was successfully updated");
            serverConfigurationFile = serverXML;
        }
    }

}
