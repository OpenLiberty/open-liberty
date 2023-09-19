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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
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
import com.ibm.ws.webcontainer.security.test.servlets.FormLoginClient.LogoutOption;
import com.ibm.ws.webcontainer.security.test.servlets.SSLHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ContextRootCookiePathTests {

    // Initialize needed strings for the tests
    protected static String METHODS = null;
    protected static final String PROGRAMMATIC_API_SERVLET = "ProgrammaticAPIServlet";
    protected static final String authTypeForm = "FORM";
    protected static final String authTypeBasic = "BASIC";
    protected static final String cookieName = "LtpaToken2";

    // Keys to help readability of the test
    protected static final boolean IS_MANAGER_ROLE = true;
    protected static final boolean NOT_MANAGER_ROLE = false;
    protected static final boolean IS_EMPLOYEE_ROLE = true;
    protected static final boolean NOT_EMPLOYEE_ROLE = false;

    // Initialize a liberty server for basic auth and form login
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.contextRootCookiePathTestServer");

    private final Class<?> thisClass = ContextRootCookiePathTests.class;

    // Initialize the user
    private static final String validUser = "user1";
    private static final String validPassword = "user1pwd";
    private static final String managerUser = "user2";
    private static final String managerPassword = "user2pwd";
    private static final String serverShutdownMessages = "CWWKG0083W";

    // Initialize the BasicAuth Clients
    private static final BasicAuthClient baClient1 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth1");
    private static final BasicAuthClient baClient2 = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, BasicAuthClient.DEFAULT_SERVLET_NAME, "/basicauth2");

    // Initialize the FormLogin Clients
    private static final FormLoginClient flClient1 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin1");
    private static final FormLoginClient flClient2 = new FormLoginClient(server, FormLoginClient.DEFAULT_SERVLET_NAME, "/formlogin2");

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

    /*
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true.
     * <LI>Attempt to access app1 (/formlogin1/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookie and path back from login.
     * <LI>Logout of app1 (/formlogin1/SimpleServlet).
     * <LI>Attempt to get the SSO cookie value and path back after the logout.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/formlogin1/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/formlogin1).
     * <LI>Successful logout of app1 (/formlogin1/SimpleServlet).
     * <LI>Retrieval of the SSO cookie value returns an empty string with path value of (/formlogin1).
     * </OL>
     */
    @Test
    public void testCookiePath_useContextRootForSSOCookiePath_true_formlogout() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "true");

        // Initial login to simple servlet for form login1
        String response = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie back from the login
        String cookie = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie);

        // Return the path value of the cookie
        String cookiePath = flClient1.getCookiePath();
        assertNotNull("SSO Cookie Path should not be null!", cookiePath);
        assertEquals("/formlogin1", cookiePath);

        // Logout of the servlet
        flClient1.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE);

        // Now try to get the SSO cookie value back after the logout
        cookie = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie);
        assertEquals("", cookie);

        // Return the path value of the cookie
        cookiePath = flClient1.getCookiePath();
        assertNotNull("SSO Cookie Path should not be null!", cookiePath);
        assertEquals("/formlogin1", cookiePath);
    }

    /*
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to false.
     * <LI>Attempt to access app1 (/formlogin1/SimpleServlet) with a valid userID and password.
     * <LI>Get the SSO cookie and path back from login.
     * <LI>Logout of app1 (/formlogin1/SimpleServlet).
     * <LI>Attempt to get the SSO cookie value and path back after the logout.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Successful authentication to app1 (/formlogin1/SimpleServlet).
     * <LI>Successful retrieval of the SSO cookie with path value of (/).
     * <LI>Successful logout of app1 (/formlogin1/SimpleServlet).
     * <LI>Retrieval of the SSO cookie value returns an empty string with path value of (/).
     * </OL>
     */
    @Test
    public void testCookiePath_useContextRootForSSOCookiePath_false_formlogout() throws Exception {
        // Set the useContextRootForSSOCookiePath to false
        setWebAppSecurityConfigElement(server, "false");

        // Initial login to simple servlet for form login1
        String response = flClient1.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_SIMPLE, validUser, validPassword);

        // Get the SSO cookie back from the login
        String cookie = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie);

        // Return the path value of the cookie
        String cookiePath = flClient1.getCookiePath();
        assertNotNull("SSO Cookie Path should not be null!", cookiePath);
        assertEquals("/", cookiePath);

        // Logout of the servlet
        flClient1.formLogout(LogoutOption.LOGOUT_DEFAULT_PAGE);

        // Now try to get the SSO cookie value back after the logout
        cookie = flClient1.getCookieFromLastLogin();
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie);
        assertEquals("", cookie);

        // Return the path value of the cookie
        cookiePath = flClient1.getCookiePath();
        assertNotNull("SSO Cookie Path should not be null!", cookiePath);
        assertEquals("/", cookiePath);
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to true.
     * <LI>Attempt to access a Programmatic API servlet configured for basic auth using a testMethod string: logout,authenticate
     * <LI>Get the SSO cookie and path back from authenticate.
     * <LI>Attempt another access a Programmatic API servlet configured for basic auth using a testMethod string: logout,login
     * <LI>Get the SSO cookie and path back from login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A valid user ID (user1) and password is permitted access to the protected servlet (/basicauth1/ProgrammaticAPIServlet).
     * <LI>Successful retrieval of the SSO cookie1 with path value of (/basicauth1).
     * <LI>1) logout() of user1 is called and should return null for the APIs
     * <LI>2) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI>Successful retrieval of the SSO cookie2 with path value of (/basicauth1).
     * <LI>1) logout() of user1 is called and should return null for the APIs
     * <LI>2) login() of user2 is called and should return the correct values for the passed-in user
     * <LI>Cookie1 and Cookie2 values should be different as the passed in users are different
     * </OL>
     */
    @Test
    public void testCookiePath_useContextRootForSSOCookiePath_true_ProgrammaticAPI() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "true");

        // First testing authenticate
        METHODS = "testMethod=logout,authenticate";
        String url1 = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth1/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                      + "&password=" + managerPassword;

        String[] response1 = authenticateWithValidAuthDataBA(validUser, validPassword, url1, PROGRAMMATIC_API_SERVLET);

        // Get the SSO cookie back from login
        String cookie1 = response1[1];
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie1);

        // Return the path value of the cookie
        String cookiePath1 = response1[2];
        assertNotNull("SSO Cookie Path should not be null!", cookiePath1);
        assertEquals("/basicauth1", cookiePath1);

        // Get servlet output to verify each test
        String test1 = response1[0].substring(response1[0].indexOf("STARTTEST1"), response1[0].indexOf("ENDTEST1"));
        String test2 = response1[0].substring(response1[0].indexOf("STARTTEST2"), response1[0].indexOf("ENDTEST2"));

        // TEST1 - check values after logout
        verifyNullValuesAfterLogout(validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after authenticate
        verifyProgrammaticAPIValues(authTypeBasic, validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);

        // Now testing login
        METHODS = "testMethod=logout,login";
        String url2 = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth1/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                      + "&password=" + managerPassword;

        String[] response2 = authenticateWithValidAuthDataBA(validUser, validPassword, url2, PROGRAMMATIC_API_SERVLET);

        // Get the SSO cookie back from login
        String cookie2 = response2[1];
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie2);

        // Return the path value of the cookie
        String cookiePath2 = response2[2];
        assertNotNull("SSO Cookie Path should not be null!", cookiePath2);
        assertEquals("/basicauth1", cookiePath2);

        // Get servlet output to verify each test
        test1 = response2[0].substring(response2[0].indexOf("STARTTEST1"), response2[0].indexOf("ENDTEST1"));
        test2 = response2[0].substring(response2[0].indexOf("STARTTEST2"), response2[0].indexOf("ENDTEST2"));

        // TEST1 - check values after logout
        verifyNullValuesAfterLogout(validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after login
        verifyProgrammaticAPIValues(authTypeBasic, managerUser, test2, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);

        //Compare cookie1 and cookie2
        assertFalse(cookie1.equals(cookie2));
        assertEquals(cookiePath1, cookiePath2);
    }

    /**
     * Test Setup:
     * <OL>
     * <LI>Set useContextRootForSSOCookiePath to false.
     * <LI>Attempt to access a Programmatic API servlet configured for basic auth using a testMethod string: logout,authenticate
     * <LI>Get the SSO cookie and path back from authenticate.
     * <LI>Attempt another access a Programmatic API servlet configured for basic auth using a testMethod string: logout,login
     * <LI>Get the SSO cookie and path back from login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A valid user ID (user1) and password is permitted access to the protected servlet (/basicauth1/ProgrammaticAPIServlet).
     * <LI>Successful retrieval of the SSO cookie1 with path value of (/).
     * <LI>1) logout() of user1 is called and should return null for the APIs
     * <LI>2) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI>Successful retrieval of the SSO cookie2 with path value of (/).
     * <LI>1) logout() of user1 is called and should return null for the APIs
     * <LI>2) login() of user2 is called and should return the correct values for the passed-in user
     * <LI>Cookie1 and Cookie2 values should be different as the passed in users are different
     * </OL>
     */
    @Test
    public void testCookiePath_useContextRootForSSOCookiePath_false_ProgrammaticAPI() throws Exception {
        // Set the useContextRootForSSOCookiePath to true
        setWebAppSecurityConfigElement(server, "false");

        // First testing authenticate
        METHODS = "testMethod=logout,authenticate";
        String url1 = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth1/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                      + "&password=" + managerPassword;

        String[] response1 = authenticateWithValidAuthDataBA(validUser, validPassword, url1, PROGRAMMATIC_API_SERVLET);

        // Get the SSO cookie back from login
        String cookie1 = response1[1];
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie1);

        // Return the path value of the cookie
        String cookiePath1 = response1[2];
        assertNotNull("SSO Cookie Path should not be null!", cookiePath1);
        assertEquals("/", cookiePath1);

        // Get servlet output to verify each test
        String test1 = response1[0].substring(response1[0].indexOf("STARTTEST1"), response1[0].indexOf("ENDTEST1"));
        String test2 = response1[0].substring(response1[0].indexOf("STARTTEST2"), response1[0].indexOf("ENDTEST2"));

        // TEST1 - check values after logout
        verifyNullValuesAfterLogout(validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after authenticate
        verifyProgrammaticAPIValues(authTypeBasic, validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);

        // Now testing login
        METHODS = "testMethod=logout,login";
        String url2 = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth1/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                      + "&password=" + managerPassword;

        String[] response2 = authenticateWithValidAuthDataBA(validUser, validPassword, url2, PROGRAMMATIC_API_SERVLET);

        // Get the SSO cookie back from login
        String cookie2 = response2[1];
        assertNotNull("Did not properly recieve the SSO Cookie.", cookie2);

        // Return the path value of the cookie
        String cookiePath2 = response2[2];
        assertNotNull("SSO Cookie Path should not be null!", cookiePath2);
        assertEquals("/", cookiePath2);

        // Get servlet output to verify each test
        test1 = response2[0].substring(response2[0].indexOf("STARTTEST1"), response2[0].indexOf("ENDTEST1"));
        test2 = response2[0].substring(response2[0].indexOf("STARTTEST2"), response2[0].indexOf("ENDTEST2"));

        // TEST1 - check values after logout
        verifyNullValuesAfterLogout(validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after login
        verifyProgrammaticAPIValues(authTypeBasic, managerUser, test2, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);

        //Compare cookie1 and cookie2
        assertFalse(cookie1.equals(cookie2));
        assertEquals(cookiePath1, cookiePath2);
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
            updateConfigDynamically(server, configuration);
            return waSecurity;
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(thisClass, "setWebAppSecurityConfigElement", "Failure getting server configuration");
        }
        return null;
    }

    // Function to update the server configuration dynamically
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());

        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        String logLine = server.waitForStringInLogUsingMark("CWWKG001[7-8]I");

        // Wait for feature update to be completed or LTPA configuration to get ready 
        Thread.sleep(200);
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful basic auth is expected when accessing the servlet
     */
    private String[] authenticateWithValidAuthDataBA(String user, String password, String url, String urlString) {
        String methodName = "authenticateWithValidAuthDataBA";
        String debugData = "URL:" + url + "; userId:[" + user + "]" + "; password:[" + password + "]";
        Log.info(thisClass, methodName, debugData);
        HttpResponse getMethod = null;
        String[] ssoCookieAndPath = null;
        String[] response = null;
        String responseText = null;
        try {
            getMethod = executeGetRequestWithAuthCreds(url, user, password);
            String authResult = getMethod.getStatusLine().toString();
            Log.info(thisClass, methodName, "BasicAuth result: " + authResult);
            HttpEntity entity = getMethod.getEntity();
            responseText = EntityUtils.toString(entity);
            Log.info(thisClass, methodName, "Servlet response: " + responseText);
            EntityUtils.consume(entity);
            Log.info(thisClass, methodName, "BasicAuth response: " + responseText);

            // Verify we get the correct response
            assertTrue("Expected output, " + urlString + ", not returned: " + responseText, responseText.contains(urlString));
            assertTrue("Expecting 200, got: " + authResult,
                       authResult.contains("200"));

            // Get cookie and path
            ssoCookieAndPath = getSSOCookieAndPath(getMethod, cookieName);
            if (ssoCookieAndPath[0] == null) {
                fail("LtpaToken2 not found in the cookie after login");
            }

        } catch (Exception e) {
            handleException(e, methodName, debugData);
        }

        response = new String[] { responseText, ssoCookieAndPath[0], ssoCookieAndPath[1] };
        return response;
    }

    /**
     * This is used for Basic Auth HttpGet with httpclient
     *
     * @throws IOException
     */
    HttpResponse executeGetRequestWithAuthCreds(String queryString, String username, String password) throws IOException {
        return executeGetRequestWithAuthCreds(queryString, username, password, null);
    }

    public HttpResponse executeGetRequestWithAuthCreds(String queryString, String username, String password, LibertyServer server) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        if (queryString.contains("https")) {
            SSLHelper.establishSSLContext(client, server.getHttpDefaultSecurePort(), server);
        }
        if (username != null) {
            client.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY),
                                                           new UsernamePasswordCredentials(username, password));
        }
        HttpGet getMethod = new HttpGet(queryString);
        HttpResponse response = client.execute(getMethod);

        return response;
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful basic auth is expected when accessing the servlet
     */
    public void verifyProgrammaticAPIValues(String authType, String loginUser, String test, boolean inMgrRole, boolean inEmpRole) {
        // Verify programmatic APIs
        assertTrue("Failed to find expected getAuthType: " + loginUser, test.contains("getAuthType: " + authType));
        assertTrue("Failed to find expected getRemoteUser: " + loginUser, test.contains("getRemoteUser: " + loginUser));
        assertTrue("Failed to find expected getUserPrincipal: " + loginUser, test.contains("getUserPrincipal: " + "WSPrincipal:" + loginUser));
        assertTrue("Failed to find expected callerSubject: " + loginUser, test.contains("callerSubject: Subject:"));
        assertTrue("Failed to find expected callerCredential: " + loginUser,
                   test.contains("callerCredential: com.ibm.ws.security.credentials.wscred.WSCredentialImpl"));
        assertTrue("Failed to find expected isUserInRole(Employee): " + inEmpRole, test.contains("isUserInRole(Employee): " + inEmpRole));
        assertTrue("Failed to find expected isUserInRole(Manager): " + inMgrRole, test.contains("isUserInRole(Manager): " + inMgrRole));
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful basic auth is expected when accessing the servlet
     */
    public void verifyNullValuesAfterLogout(String user, String test, boolean inMgrRole, boolean inEmpRole) {
        // Verify programmatic APIs return null
        assertTrue("getAuthType not null after logout: " + user, test.contains("getAuthType: null"));
        assertTrue("getRemoteUser not null after logout: " + user, test.contains("getRemoteUser: null"));
        assertTrue("getUserPrincipal not null after logout: " + user, test.contains("getUserPrincipal: null"));
        assertTrue("callerSubject not null after logout: " + user, test.contains("callerSubject: null"));
        assertTrue("callerCredential not null after logout: " + user, test.contains("callerCredential: null"));
        assertTrue("isUserInRole(Employee) not false after logout: " + inEmpRole, test.contains("isUserInRole(Employee): false"));
        assertTrue("isUserInRole(Manager) not false after logout: " + inMgrRole, test.contains("isUserInRole(Manager): false"));
    }

    /**
     * This is used for Basic Auth to handle exceptions with httpclient
     */
    public void handleException(Exception e, String methodName, String debugData) {
        Log.info(thisClass, methodName, "caught unexpected exception for: " + debugData + e.getMessage());
        fail("Caught unexpected exception: " + e);
    }

    /**
     * This method is used to get the cookie value and path.
     */
    protected String[] getSSOCookieAndPath(HttpMessage httpMessage, String cookieName) {
        String cookieValue = null;
        String cookiePath = null;

        Header[] setCookieHeaders = httpMessage.getHeaders("Set-Cookie");
        if (setCookieHeaders == null) {
            Log.info(thisClass, "getCookieValue", "setCookieHeaders was null and should not be");
        }
        for (Header header : setCookieHeaders) {
            Log.info(thisClass, "getCookieValue", "header: " + header);
            HeaderElement[] elements = header.getElements();
            for (HeaderElement element : elements) {
                if (element.getName().equals(cookieName)) {
                    NameValuePair[] parameters = element.getParameters();
                    for (NameValuePair parameter : parameters) {
                        if (parameter.getName().equalsIgnoreCase("path")) {
                            cookiePath = parameter.getValue();
                            break;
                        }
                    }
                    return new String[] { element.getValue(), cookiePath };
                }
            }
        }
        return new String[] { cookieValue, cookiePath };
    }
}
