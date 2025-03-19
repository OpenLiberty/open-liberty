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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.TestConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BasicAuthDenyTest {

    public static final String PROTECTED_SIMPLE = "/SimpleServlet";
    public static final String PROTECTED_EMPLOYEE_ROLE = "/EmployeeRoleServlet";
    public static final String PROTECTED_ACCESS_PRECLUDED = "/EmptyConstraintServlet";
    public static final String OMISSION_BASIC = "/OmissionBasic";
    public static final String OVERLAP_CUSTOM_METHOD_SERVLET = "/OverlapCustomMethodServlet";
    public static final String CUSTOM_METHOD_SERVLET = "/CustomMethodServlet";

    // Keys to help readability of the test
    public static final boolean IS_MANAGER_ROLE = true;
    public static final boolean NOT_MANAGER_ROLE = false;
    public static final boolean IS_EMPLOYEE_ROLE = true;
    public static final boolean NOT_EMPLOYEE_ROLE = false;

    // Users defined by role
    protected final static String realm = "BasicRealm";
    protected final static String invalidUser = "invalidUser";
    protected final static String invalidPassword = "invalidPwd";
    protected final static String employeeUser = "user1";
    protected final static String employeePassword = "user1pwd";
    protected final static String managerUser = "user2";
    protected final static String managerPassword = "user2pwd";
    protected final static String managerGroupUser = "user6";
    protected final static String managerGroupPassword = "user6pwd";

    private static String DEFAULT_CONFIG_FILE = "basicauthdeny.server.orig.xml";
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauthdeny");
    private static Class<?> myLogClass = BasicAuthDenyTest.class;
    private static BasicAuthClient client;
    private static SSLBasicAuthClient mySSLClient;
    private static String appName = "basicauthdeny";

    // TestName should be an instance level Rule, not a ClassRule, but static references are made to it,
    // so we will create it as a static field, then keep a reference to it from a non-static field which
    // is annotated with @Rule - junit can make the test method name changes to that field, which should
    // (hopefully) be reflected in the static references as well.
    private static TestName _name = new TestName();

    @Rule
    public TestName name = _name;

    private static final TestConfiguration testConfig = new TestConfiguration(server, myLogClass, _name, appName);

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation(appName);

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "basicauthdeny.war");

        testConfig.startServerClean(DEFAULT_CONFIG_FILE);

        client = new BasicAuthClient(server);
        mySSLClient = new SSLBasicAuthClient(server);
        client.setJaccValidation(true);
        mySSLClient.setJaccValidation(true);

        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I")); //Hiroko-Kristen

        server.updateLogOffset(server.getDefaultLogFile().getAbsolutePath(), (long) 0);

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

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password. Call isUserInRole with a
     * <LI>rolename of "*". The http method is covered in this scenario.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> isUserInRole(*) is false
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarRole_Employee_CoveredMethod() throws Exception {
        String specifiedRole = "*";
        String response = client.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE + "?role=" + specifiedRole, employeeUser,
                                                                                 employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   client.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE, "*", false));

        server.updateLogOffset(server.getDefaultLogFile().getAbsolutePath(), (long) 0);

    }

    /**
     * Verify the following:
     * <LI>After the application is deployed, validate that we see messages for any uncovered http methods
     * <LI>
     */
    @Test
    public void testUncoveredHttpMethodsMessages() throws Exception {

        assertNotNull("CWWKS9123I:  For URL /SimpleServlet in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: PUT DELETE HEAD OPTIONS TRACE",
                      server.waitForStringInLogUsingMark("CWWKS9123I:  For URL /SimpleServlet in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: PUT DELETE HEAD OPTIONS TRACE"));

        server.updateLogOffset(server.getDefaultLogFile().getAbsolutePath(), (long) 0);

    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password. Call isUserInRole with a
     * <LI>rolename of "*". The http method is uncovered in this scenario.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password will still not permit access to the protected servlet as
     * <LI> the GET method is uncovered and <deny-uncovered-http-methods/> is set in the web.xml
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testIsUserInRoleStarRole_Employee_UncoveredMethod() throws Exception {
        String specifiedRole = "*";

        assertTrue("Expected access to be denied, but it was granted",
                   client.accessDeniedHttpMethodServlet(BasicAuthClient.PROTECTED_EMPLOYEE_ROLE + "?role=" + specifiedRole, employeeUser,
                                                        employeePassword));

        assertNotNull("CWWKS9123I:  For URL /EmployeeRoleServlet in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: GET PUT DELETE HEAD OPTIONS TRACE",
                      server.waitForStringInLogUsingMark("CWWKS9123I:  For URL /EmployeeRoleServlet in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: GET PUT DELETE HEAD OPTIONS TRACE"));
        server.updateLogOffset(server.getDefaultLogFile().getAbsolutePath(), (long) 0);

    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password. Call isUserInRole with a
     * <LI>rolename of "*". The http method is uncovered in this scenario.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password will still not permit access to the protected servlet as
     * <LI> the GET method is uncovered and <deny-uncovered-http-methods/> is set in the web.xml
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarRole_Employee_CustomUncoveredMethod() throws Exception {
        String specifiedRole = "*";

        assertTrue("Expected access to be denied, but it was granted",
                   client.accessDeniedHttpMethodServlet(BasicAuthClient.CUSTOM_METHOD_SERVLET + "?role=" + specifiedRole, employeeUser,
                                                        employeePassword));

        assertNotNull("CWWKS9123I:  For URL /CustomMethodServlet in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: GET",
                      server.waitForStringInLogUsingMark("CWWKS9123I:  For URL /CustomMethodServlet in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: GET"));
        server.updateLogOffset(server.getDefaultLogFile().getAbsolutePath(), (long) 0);

    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic authentication.
     * <LI>The role name MappedToEmployee is mapped to Employee
     * <LI>Login with a employee userId and password. Call isUserInRole with a
     * <LI>rolename of "*". The http method is uncovered in this scenario.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A 401 Challenge when accessing the protected page for the first time.
     * <LI> A valid userId and password will still not permit access to the protected servlet as
     * <LI> the GET method is uncovered by http-method-omission and <deny-uncovered-http-methods/>
     * <LI> is set in the web.xml
     * </OL>
     */
    @Test
    public void testIsUserInRoleStarRole_Employee_UncoveredMethodByHttpOmission() throws Exception {
        String specifiedRole = "*";

        assertTrue("Expected access to be denied, but it was granted",
                   client.accessDeniedHttpMethodServlet(BasicAuthClient.OMISSION_BASIC + "?role=" + specifiedRole, employeeUser,
                                                        employeePassword));

        assertNotNull("CWWKS9123I:  For URL /OmissionBasic in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: GET",
                      server.waitForStringInLogUsingMark("CWWKS9123I:  For URL /OmissionBasic in application basicauthdeny, the following HTTP methods are uncovered, and not accessible: GET"));

        server.updateLogOffset(server.getDefaultLogFile().getAbsolutePath(), (long) 0);

    }

}
