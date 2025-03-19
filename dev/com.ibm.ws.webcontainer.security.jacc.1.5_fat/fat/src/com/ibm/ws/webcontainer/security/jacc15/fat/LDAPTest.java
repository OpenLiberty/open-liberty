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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class LDAPTest {
    // Keys to help readability of the test
    protected final boolean IS_MANAGER_ROLE = true;
    protected final boolean NOT_MANAGER_ROLE = false;
    protected final boolean IS_EMPLOYEE_ROLE = true;
    protected final boolean NOT_EMPLOYEE_ROLE = false;

    private final String employeeUser = "testuser";
    private final String employeePassword = "testuserpwd";
    private final String employeeInGroupWithAccessId = "LDAPUser1";
    private final String employeeInGroupWithAccessIdPassword = "security";
    private final String employeeInGroupNoAccessId = "LDAPUser2";
    private final String employeeInGroupNoAccessIdPassword = "security";
    private final String unauthorizedUser = "LDAPUser5";
    private final String unauthorizedUserPassword = "security";

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.basicauth.ldap");
    protected static Class<?> logClass = LDAPTest.class;
    protected static BasicAuthClient myClient;

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(logClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(logClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        LDAPUtils.addLDAPVariables(myServer);
        JACCFatUtils.transformApps(myServer, "basicauth.war");

        myServer.addInstalledAppForValidation("basicauth");
        myServer.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      myServer.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      myServer.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      myServer.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", myServer.waitForStringInLog("CWWKS2200I")); //Hiroko-Kristen
        assertNotNull("JACC feature did not report it was ready", myServer.waitForStringInLog("CWWKS2201I")); //Hiroko-Kristen

        myClient = new BasicAuthClient(myServer);
        myClient.setJaccValidation(true);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
    }

    @After
    public void resetConnection() {
        myClient.resetClientState();
    }

    /**
     * Access the protected servlet as a valid LDAP user, and check that
     * getRemoteUser and getUserPrincipal do not return the full DN, but
     * rather the name used to log in.
     */
    @Test
    public void testSimpleEmployeeAuthorizedForUser() throws Exception {
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeUser, employeePassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Access the protected servlet as a valid LDAP user, and check that
     * getRemoteUser and getUserPrincipal do not return the full DN, but
     * rather the name used to log in.
     * Only the group is authorized, and the access-id is specified in the
     * server.xml.
     */
    @Test
    public void testSimpleEmployeeAuthorizedForGroupWithAccessId() throws Exception {
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeInGroupWithAccessId,
                                                                                   employeeInGroupWithAccessIdPassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, employeeInGroupWithAccessId, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Access the protected servlet as a valid LDAP user who is not authorized,
     * and validate a 403 is returned for the unauthorized user.
     * Only the group is authorized, and the access-id is specified in the
     * server.xml.
     */
    @Test
    public void testSimpleEmployeeNotAuthorizedForGroupWithAccessId() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, unauthorizedUser, unauthorizedUserPassword));
    }

    /**
     * Access the protected servlet as a valid LDAP user, and check that
     * getRemoteUser and getUserPrincipal do not return the full DN, but
     * rather the name used to log in.
     * Only the group is authorized, and the access-id is NOT specified in
     * the server.xml.
     */
    @Test
    public void testSimpleEmployeeAuthorizedForGroupNoAccessId() throws Exception {
        String response = myClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, employeeInGroupNoAccessId,
                                                                                   employeeInGroupNoAccessIdPassword);
        assertTrue("Verification of programmatic APIs failed",
                   myClient.verifyResponse(response, employeeInGroupNoAccessId, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Access the protected servlet as a valid LDAP user who is not authorized,
     * and validate a 403 is returned for the unauthorized user.
     * Only the group is authorized, and the access-id is NOT specified in
     * the server.xml.
     */
    @Test
    public void testSimpleEmployeeNotAuthorizedForGroupNoAccessId() throws Exception {
        assertTrue("Expected access to be denied, but it was granted",
                   myClient.accessProtectedServletWithUnauthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, unauthorizedUser, unauthorizedUserPassword));
    }
}
