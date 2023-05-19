/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.WebAppSecurity;
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

    // Initialize the BasicAuthClients
    private static final BasicAuthClient baClient1 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth1");
    private static final BasicAuthClient baClient2 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth2");

    private static final String serverShutdownMessages = "CWWKG0083W";

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

        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));

    }

    @After
    public void resetConnection() {
        baClient1.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer(serverShutdownMessages);
        } finally {
            baClient1.releaseClient();
        }
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true
     * <LI>Attempt to access a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
     * <LI>Get the cookie back from the session
     * <LI>Return the path value of the cookie
     * <LI>Attempt to access simple servlet using the cookie
     * <LI>Attempt to access a manager role servlet configured for basic auth1 using the previously obtained cookie
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful update to the server xml configuration
     * <LI>Successful authentication to simple servlet
     * <LI>Successful retrieval of the cookie
     * <LI>Successful retrieval of the path value of the cookie with the value of "/basicauth1"
     * <LI>Successful authentication to simple servlet using the cookie
     * <LI>Successful authentication to manager role servlet using the previous cookie of the same context root even after a connection reset
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Mode(TestMode.LITE)
    @Test
    public void testSuccessfulLoginWithCookieFromSameContextRoot() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "true");

        String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
        assertNotNull(response);

        // Get the cookie back from the session
        String cookie = baClient1.getCookieFromLastLogin();
        assertNotNull(cookie);

        // Return the path value of the cookie
        String cookiePath = baClient1.getCookiePath();
        assertNotNull(cookiePath);
        assertEquals("/basicauth1", cookiePath);

        // Print both values
        Log.info(thisClass, "testSuccessfulLoginWithCookieForSameContextRoot", "Cookie: " + cookie);
        Log.info(thisClass, "testSuccessfulLoginWithCookieForSameContextRoot", "Cookie Path: " + cookiePath);

        // Now try to access the servlet with the cookie
        response = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie);
        assertNotNull(response);

        // Reset the connection
        resetConnection();

        // Now access the manager servlet ("/ManagerRoleServlet") with the previous cookie
        response = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_MANAGER_ROLE, cookie);
        assertNotNull(response);

        // Print both values
        Log.info(thisClass, "testSuccessfulLoginWithCookieForSameContextRoot", "Cookie: " + cookie);
        Log.info(thisClass, "testSuccessfulLoginWithCookieForSameContextRoot", "Cookie Path: " + cookiePath);

    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true
     * <LI>Attempt to access a simple servlet configured for basic auth1 with a valid userId (mnagerUser) and password.
     * <LI>Get the cookie back from the session
     * <LI>Return the path value of the cookie
     * <LI>Attempt to access simple servlet using this cookie
     * <LI>Now attempt to access a simple servlet configured for basic auth2 with a valid userId (mnagerUser) and password.
     * <LI>Get the cookie back from the session
     * <LI>Return the path value of the cookie
     * <LI>Attempt to access simple servlet using this cookie
     * <LI>Make sure that cookie1 and cookie 2 are different
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful update to the server xml configuration
     * <LI>Successful authentication to simple servlet for basic auth1
     * <LI>Successful retrieval of the cookie
     * <LI>Successful retrieval of the path value of the cookie with the value of "/basicauth1"
     * <LI>Successful authentication to simple servlet using the cookie
     * <LI>Successful authentication to simple servlet for basic auth2
     * <LI>Successful retrieval of the cookie
     * <LI>Successful retrieval of the path value of the cookie with the value of "/basicauth2"
     * <LI>Successful authentication to simple servlet using the cookie
     * <LI>Cookie1 and Cookie2 must be different and they both must have different path values respective of their context root
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Mode(TestMode.LITE)
    @Test
    public void testUnsuccessfulLoginWithCookieFromDifferentContextRoot() throws Exception {
        // Set the useContextRootForSSOCookiePath to false
        setWebAppSecurityConfigElement(server, "true");

        String response1 = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
        assertNotNull(response1);

        // Get the cookie back from the session
        String cookie1 = baClient1.getCookieFromLastLogin();
        assertNotNull(cookie1);

        // Return the path value of the cookie
        String cookiePath1 = baClient1.getCookiePath();
        assertNotNull(cookiePath1);
        assertEquals("/basicauth1", cookiePath1);

        // Print both values
        Log.info(thisClass, "testUnsuccessfulLoginWithCookieFromDifferentContextRoot", "baClient1 Cookie: " + cookie1);
        Log.info(thisClass, "testUnsuccessfulLoginWithCookieFromDifferentContextRoot", "baClient1 Cookie Path: " + cookiePath1);

        // Now try to access the servlet with the cookie
        response1 = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie1);
        assertNotNull(response1);

        String response2 = baClient2.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, managerUser, managerPassword);
        assertNotNull(response2);

        // Get the cookie back from the session
        String cookie2 = baClient2.getCookieFromLastLogin();
        assertNotNull(cookie2);

        // Return the path value of the cookie
        String cookiePath2 = baClient2.getCookiePath();
        assertNotNull(cookiePath2);
        assertEquals("/basicauth2", cookiePath2);

        // Print both values
        Log.info(thisClass, "testUnsuccessfulLoginWithCookieFromDifferentContextRoot", "baClient2 Cookie: " + cookie2);
        Log.info(thisClass, "testUnsuccessfulLoginWithCookieFromDifferentContextRoot", "baClient2 Cookie Path: " + cookiePath2);

        // Now try to access the servlet with the cookie
        response2 = baClient2.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie2);
        assertNotNull(response2);

        // Assert that cookie1 and cookie2 are different
        assertFalse(cookie1.equals(cookiePath2));
        assertFalse(cookiePath1.equals(cookiePath2));
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to "badString"
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Configuration exception
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Mode(TestMode.LITE)
    @Test
    public void testUnsuccessfulUpdateToXMLForInvalidStringValue() throws Exception {
        // Set the useContextRootForSSOCookiePath to "badString"
        setWebAppSecurityConfigElement(server, "badString");
        assertNotNull("Configuration exception did not occur",
                      server.waitForStringInLog("The value badString for boolean attribute useContextRootForSSOCookiePath is invalid."));
    }

    // Function to set the useContextRootForSSOCookiePath to true or false
    public WebAppSecurity setWebAppSecurityConfigElement(LibertyServer server, String useContextRootForSSOCookiePath) {
        WebAppSecurity waSecurity;
        try {
            ServerConfiguration configuration = server.getServerConfiguration();
            waSecurity = configuration.getWebAppSecurity();
            waSecurity.useContextRootForSSOCookiePath = useContextRootForSSOCookiePath;
            updateConfigDynamically(server, configuration, true);
            return waSecurity;
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(thisClass, "setWebAppSecurityConfigElement", "Failure getting server configuration");
        }
        return null;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, boolean waitForAppToStart) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
        if (waitForAppToStart) {
            server.waitForStringInLogUsingMark("CWWKZ0003I"); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
        }
    }
}
