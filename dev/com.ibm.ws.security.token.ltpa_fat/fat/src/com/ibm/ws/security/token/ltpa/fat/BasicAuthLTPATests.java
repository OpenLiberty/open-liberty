/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package com.ibm.ws.security.token.ltpa.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

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

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BasicAuthLTPATests {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.basicauth");
    private final Class<?> thisClass = BasicAuthLTPATests.class;

    private final static String employeeUser = "user1";
    private final static String employeePassword = "user1pwd";

    private final static String managerUser = "user2";
    private final static String managerPassword = "user2pwd";

    private final static String invalidUser = "invalidUser";
    private final static String invalidPassword = "invalidPwd";

    private final static String METHODS = "testMethod=login,logout,login";

    // Keys to help readability of the test
    private final boolean IS_MANAGER_ROLE = true;
    private final boolean NOT_MANAGER_ROLE = false;
    private final boolean IS_EMPLOYEE_ROLE = true;
    private final boolean NOT_EMPLOYEE_ROLE = false;

    private static final BasicAuthClient baClient = new BasicAuthClient(server);

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "Entering test " + description.getMethodName());
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "Exiting test " + description.getMethodName());
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer(true);
/*
 * assertNotNull("FeatureManager did not report update was complete",
 * server.waitForStringInLog("CWWKF0008I"));
 * assertNotNull("Security service did not report it was ready",
 * server.waitForStringInLog("CWWKS0008I"));
 * assertNotNull("The application did not report is was started",
 * server.waitForStringInLog("CWWKZ0001I"));
 */
    }

    @After
    public void resetConnection() {
        baClient.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            baClient.releaseClient();
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a simple servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A different userId (user2) and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called but expects an exception
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should return the correct values for the passed-in user
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Mode(TestMode.LITE)
    @Test
    public void testLoginMethodBA_ValidUserIdPassword() throws Exception {

        // Test #1: Positive Scenarios
        // When useContextRootForSSOCookiePath has been set to true
        // 1. Successful authentication with LTPA, using . app1 ("/basicauth/SimpleServlet"), ltpatoken works,
        // then app2 ("/basicauth/ManagerRoleServlet") ltpatoken still works. (because "/basicauth" domain root is the same)

        // Set the useContextRootForSSOCookiePath to true

        String response = baClient.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
        assertNotNull(response);

        // Get the cookie back from the session
        String cookie = baClient.getCookieFromLastLogin();
        assertNotNull(cookie);

        // Return the path value of the cookie
        String cookiePath = baClient.getContextRoot();
        assertNotNull(cookiePath);

        // Print both values
        System.out.println("Cookie: " + cookie);
        System.out.println("Cookie Path: " + cookiePath);

        // Now try to access the servlet with the cookie
        response = baClient.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie);
        assertNotNull(response);

        resetConnection();

        // Now access the manager servlet ("/ManagerRoleServlet") with the cookie
        response = baClient.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_MANAGER_ROLE, cookie);
        assertNotNull(response);

        // Print both values
        System.out.println("Cookie: " + cookie);
        System.out.println("Cookie Path: " + cookiePath);

        // Test #2: Negative Scenarios
        /*
         * // TEST1 - check values after 1st login
         * // we expect a ServletException if already logged in
         * assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));
         *
         * // TEST2 - check values after logout
         * assertTrue(baClient.verifyUnauthenticatedResponse(test2));
         *
         * // TEST3 - check values after 2nd login
         * assertTrue(baClient.verifyResponse(test3, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
         */

        List<String> passwordsInTrace = server.findStringsInLogsAndTrace(employeePassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
        passwordsInTrace = server.findStringsInLogsAndTrace(managerPassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
    }

}
