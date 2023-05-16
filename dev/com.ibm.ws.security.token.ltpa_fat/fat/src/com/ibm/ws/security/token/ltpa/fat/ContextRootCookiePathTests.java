/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

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
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ContextRootCookiePathTests {

    // Initialize two liberty servers for basic auth and form login
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.contextRootCookiePathTestServer");

    private final Class<?> thisClass = ContextRootCookiePathTests.class;

    private final static String validUser = "user1";
    private final static String validPassword = "user1pwd";

    // Initialize the BasicAuth Clients
    private static final BasicAuthClient baClient1 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth1");
    private static final BasicAuthClient baClient2 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth2");

    // Initialize the FormLogin Clients
    private static final FormLoginClient flClient1 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
    private static final FormLoginClient flClient2 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

    private static final String serverShutdownMessages = "CWWKG0083W";

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        // Function to make it easier to see when each test starts and ends
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer(true);

        assertNotNull("Featurevalid did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));

    }

    @After
    public void resetConnection() {
        baClient1.resetClientState();
        baClient2.resetClientState();
        flClient1.resetClientState();
        flClient2.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer(serverShutdownMessages);
        } finally {
            baClient1.releaseClient();
            baClient2.releaseClient();
            flClient1.releaseClient();
            flClient2.releaseClient();
        }
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookie and path back from last login.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) using the cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/basicauth1/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/basicauth1).
     * <LI>Successful authentication to app1 (/basicauth1/SimpleServlet) using the cookie.
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Test
    public void testCookiePath_useContextRootForSSOCookiePath_true() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "true");

        String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie back from last login
        String cookie = baClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie);

        // Return the path value of the cookie
        String cookiePath = baClient1.getCookiePath();
        assertNotNull("SSO Cookie Path should not be null!", cookiePath);
        assertEquals("/basicauth1", cookiePath);

        // Now try to access the servlet with the cookie
        response = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie);
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to false.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookie and path back from last login.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) using the cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/basicauth1/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/).
     * <LI>Successful authentication to app1 (/basicauth1/SimpleServlet) using the cookie.
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Test
    public void testCookiePath_useContextRootForSSOCookiePath_false() throws Exception {
        // Set the useContextRootForSSOCookiePath to false
        setWebAppSecurityConfigElement(server, "false");

        String response = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie back from last login
        String cookie = baClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie);

        // Return the path value of the cookie
        String cookiePath = baClient1.getCookiePath();
        assertNotNull("SSO Cookie Path should not be null!", cookiePath);
        assertEquals("/", cookiePath);

        // Now try to access the servlet with the cookie
        response = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie);
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) and app2 (/basicauth2/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookies and paths back from both logins.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) and app2 (/basicauth2/SimpleServlet) with the SSO cookies.
     * <LI>Compare the paths of the SSO cookies.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/basicauth1/SimpleServlet) and app2 (/basicauth2/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/basicauth1) and (/basicauth2).
     * <LI>Successful authentication to app1 and app2. Both cookies have different paths.
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Mode(TestMode.LITE)
    @Test
    public void testCookiePath_2apps_useContextRootForSSOCookiePath_true_basicauth() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "true");

        // Initial login to simple servlet for basic auth1 and basic auth2
        String response1 = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);
        String response2 = baClient2.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = baClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 1.", cookie1);

        String cookie2 = baClient2.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 2.", cookie2);

        // Return the path values of the cookies
        String cookiePath1 = baClient1.getCookiePath();
        String cookiePath2 = baClient2.getCookiePath();

        // Now try to access both servlets with their respective cookies
        response1 = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie1);
        response2 = baClient2.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie2);

        // Assert that cookie1 and cookie2 have different paths
        assertFalse("The cookie paths from app1 and app2 did match! CookiePath1 = " + cookiePath1 + ". CookiePath2 = " + cookiePath2 + ".",
                    cookiePath1.equals(cookiePath2));
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to false.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) and app2 (/basicauth2/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookies and paths back from both logins.
     * <LI>Attempt to access app1 (/basicauth1/SimpleServlet) and app2 (/basicauth2/SimpleServlet) with the SSO cookies.
     * <LI>Compare the paths of the SSO cookies.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/basicauth1/SimpleServlet) and app2 (/basicauth2/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/) and (/).
     * <LI>Successful authentication to app1 and app2. Both cookies have same paths (/).
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Test
    public void testCookiePath_2apps_useContextRootForSSOCookiePath_false_basicauth() throws Exception {
        // Set the useContextRootForSSOCookiePath to false
        setWebAppSecurityConfigElement(server, "false");

        // Initial login to simple servlet for basic auth1 and basic auth2
        String response1 = baClient1.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);
        String response2 = baClient2.accessProtectedServletWithAuthorizedCredentials(BasicAuthClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = baClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 1.", cookie1);

        String cookie2 = baClient2.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 2.", cookie2);

        // Return the path values of the cookies
        String cookiePath1 = baClient1.getCookiePath();
        String cookiePath2 = baClient2.getCookiePath();

        // Now try to access both servlets with their respective cookies
        response1 = baClient1.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie1);
        response2 = baClient2.accessProtectedServletWithAuthorizedCookie(BasicAuthClient.PROTECTED_SIMPLE, cookie2);

        // Assert that cookie1 and cookie2 have the same path
        assertTrue("The cookie paths from app1 and app2 did not match! CookiePath1 = " + cookiePath1 + ". CookiePath2 = " + cookiePath2 + ".",
                   cookiePath1.equals(cookiePath2));
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true.
     * <LI>Attempt to access app1 (/formlogin1/SimpleServlet) and app2 (/formlogin2/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookies and paths back from both logins.
     * <LI>Attempt to access app1 (/formlogin1/SimpleServlet) and app2 (/formlogin2/SimpleServlet) with the SSO cookies.
     * <LI>Compare the paths of the SSO cookies.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/formlogin1/SimpleServlet) and app2 (/formlogin2/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/formlogin1) and (/formlogin2).
     * <LI>Successful authentication to app1 and app2. Both cookies have different paths.
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Mode(TestMode.LITE)
    @Test
    public void testCookiePath_2apps_useContextRootForSSOCookiePath_true_formlogin() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "true");

        // Initial login to simple servlet for form login1 and form login2
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String response2 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 1.", cookie1);

        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 2.", cookie2);

        // Return the path values of the cookies
        String cookiePath1 = flClient1.getCookiePath();
        String cookiePath2 = flClient2.getCookiePath();

        // Now try to access both servlets with their respective cookies
        response1 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
        response2 = flClient2.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie2);

        // Assert that cookie1 and cookie2 have different paths
        assertFalse("The cookie paths from app1 and app2 did match! CookiePath1 = " + cookiePath1 + ". CookiePath2 = " + cookiePath2 + ".",
                    cookiePath1.equals(cookiePath2));
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to false.
     * <LI>Attempt to access app1 (/formlogin1/SimpleServlet) and app2 (/formlogin2/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookies and paths back from both logins.
     * <LI>Attempt to access app1 (/formlogin1/SimpleServlet) and app2 (/formlogin2/SimpleServlet) with the SSO cookies.
     * <LI>Compare the paths of the SSO cookies.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/formlogin1/SimpleServlet) and app2 (/formloginh2/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/) and (/).
     * <LI>Successful authentication to app1 and app2. Both cookies have same paths (/).
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Test
    public void testCookiePath_2apps_useContextRootForSSOCookiePath_false_formlogin() throws Exception {
        // Set the useContextRootForSSOCookiePath to false
        setWebAppSecurityConfigElement(server, "false");

        // Initial login to simple servlet for form login1 and form login2
        String response1 = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);
        String response2 = flClient2.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookies back from each login
        String cookie1 = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 1.", cookie1);

        String cookie2 = flClient2.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie 2.", cookie2);

        // Return the path values of the cookies
        String cookiePath1 = flClient1.getCookiePath();
        String cookiePath2 = flClient2.getCookiePath();

        // Now try to access both servlets with their respective cookies
        response1 = flClient1.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie1);
        response2 = flClient2.accessProtectedServletWithAuthorizedCookie(FormLoginClient.PROTECTED_SIMPLE, cookie2);

        // Assert that cookie1 and cookie2 have the same path
        assertTrue("The cookie paths from app1 and app2 did not match! CookiePath1 = " + cookiePath1 + ". CookiePath2 = " + cookiePath2 + ".",
                   cookiePath1.equals(cookiePath2));
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to "badString".
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Configuration exception: CWWKG0081E: The value badString for boolean attribute useContextRootForSSOCookiePath is invalid.
     * <LI>Valid values are "true" and "false". The default value of false will be used.
     * </OL>
     */
    @SuppressWarnings("restriction")
    @Test
    public void testServerConfiguration_useContextRootForSSOCookiePath_badValue() throws Exception {
        // Set the useContextRootForSSOCookiePath to "badString"
        setWebAppSecurityConfigElement(server, "badString");
        assertNotNull("Configuration exception did not occur",
                      server.waitForStringInLog("CWWKG0081E"));
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
        String logLine = server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
        if (waitForAppToStart && !logLine.contains("CWWKG0018I")) {
            server.waitForStringInLogUsingMark("CWWKZ0003I", 10000); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
        }
    }
}
