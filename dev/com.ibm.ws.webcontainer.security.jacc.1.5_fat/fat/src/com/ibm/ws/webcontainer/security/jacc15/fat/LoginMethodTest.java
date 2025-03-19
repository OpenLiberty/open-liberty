/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
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
public class LoginMethodTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.webcontainer.security.fat.loginmethod");
    private final Class<?> thisClass = LoginMethodTest.class;

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

    private static final BasicAuthClient baClient = new BasicAuthClient(server, BasicAuthClient.DEFAULT_REALM, "ProgrammaticAPIServlet", BasicAuthClient.DEFAULT_CONTEXT_ROOT);
    private static final FormLoginClient flClient = new FormLoginClient(server, "ProgrammaticAPIServlet", FormLoginClient.DEFAULT_CONTEXT_ROOT);

    static {
        baClient.setJaccValidation(true);
        flClient.setJaccValidation(true);
    }

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

        JACCFatUtils.installJaccUserFeature(server);
        JACCFatUtils.transformApps(server, "loginmethod.ear");

        server.addInstalledAppForValidation("loginmethod");
        server.startServer(true);
        assertNotNull("FeatureManager did not report update was complete",
                      server.waitForStringInLog("CWWKF0008I"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));
        assertNotNull("JACC feature did not report it was starting", server.waitForStringInLog("CWWKS2850I"));
        assertNotNull("JACC feature did not report it was ready", server.waitForStringInLog("CWWKS2851I"));

    }

    @After
    public void resetConnection() {
        baClient.resetClientState();
        flClient.resetClientState();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            server.stopServer();
        } finally {
            JACCFatUtils.uninstallJaccUserFeature(server);
            baClient.releaseClient();
            flClient.releaseClient();
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
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
    @Mode(TestMode.LITE)
    @Test
    public void testLoginMethodBA_ValidUserIdPassword() throws Exception {
        String queryString = BasicAuthClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + managerUser + "&password=" + managerPassword;
        String response = baClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

        // TEST2 - check values after logout
        assertTrue(baClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        assertTrue(baClient.verifyResponse(test3, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        List<String> passwordsInTrace = server.findStringsInLogsAndTrace(employeePassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
        passwordsInTrace = server.findStringsInLogsAndTrace(managerPassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The same userId (user1) and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called but expects an exception
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodBA_SameUserIdPassword() throws Exception {
        String queryString = BasicAuthClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + employeeUser + "&password=" + employeePassword;
        String response = baClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

        // TEST2 - check values after logout
        assertTrue(baClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        assertTrue(baClient.verifyResponse(test3, employeeUser, IS_EMPLOYEE_ROLE, NOT_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> An invalid userId and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called but expects an exception
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should fail for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodBA_IncorrectUserId() throws Exception {
        String methodName = "testLoginMethodBA_IncorrectUserId";

        // Do not use BasicAuthClient for this since it will try to
        // validate a cookie was returned and since the last login
        // was invalid, it will not be.
        String queryString = BasicAuthClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + invalidUser + "&password=" + invalidPassword;
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                     BasicAuthClient.DEFAULT_CONTEXT_ROOT + queryString;
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            client.getCredentialsProvider().setCredentials(new AuthScope(server.getHostname(), server.getHttpDefaultPort()),
                                                           new UsernamePasswordCredentials(employeeUser, employeePassword));
            HttpGet getMethod = new HttpGet(url);
            HttpResponse response = client.execute(getMethod);
            HttpEntity entity = response.getEntity();
            String content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            assertNotNull(content);

            Log.info(thisClass, methodName, "Servlet response: " + content);

            assertTrue("Content should contain start of test1", content.indexOf("STARTTEST1") > 0);
            assertTrue("Content should contain end of test1", content.indexOf("ENDTEST1") > 0);
            assertTrue("Content should contain start of test2", content.indexOf("STARTTEST2") > 0);
            assertTrue("Content should contain end of test2", content.indexOf("ENDTEST2") > 0);
            assertTrue("Content should contain start of test3", content.indexOf("STARTTEST3") > 0);
            assertTrue("Content should contain end of test3", content.indexOf("ENDTEST3") > 0);

            // Get servlet output to verify each test
            String test1 = content.substring(content.indexOf("STARTTEST1"), content.indexOf("ENDTEST1"));
            String test2 = content.substring(content.indexOf("STARTTEST2"), content.indexOf("ENDTEST2"));
            String test3 = content.substring(content.indexOf("STARTTEST3"), content.indexOf("ENDTEST3"));

            // TEST1 - check values after 1st login
            // we expect a ServletException if already logged in
            assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

            // TEST2 - check values after logout
            assertTrue(baClient.verifyUnauthenticatedResponse(test2));

            // TEST3 - check values after 2nd login
            // we expect a ServletException
            assertTrue("Failed to find after 2nd login: ServletException", test3.contains("ServletException"));

            // Validate we did not get a cookie back (due to final login being bad)
            baClient.validateNoSSOCookie(response);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> A valid userId (user2) and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called and should return the correct values for the passed-in user
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodBA_UnprotectedServlet() throws Exception {
        String queryString = BasicAuthClient.UNPROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + managerUser + "&password=" + managerPassword;
        String response = baClient.accessUnprotectedServlet(queryString);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue(baClient.verifyResponse(test1, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        // TEST2 - check values after logout
        assertTrue(baClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        assertTrue(baClient.verifyResponse(test3, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> An invalid userId and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called and should fail for the passed-in user
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should fail for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodBA_UnprotectedServletIncorrectUserId() throws Exception {
        String queryString = BasicAuthClient.UNPROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + invalidUser + "&password=" + invalidPassword;
        String response = baClient.accessUnprotectedServlet(queryString);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

        // TEST2 - check values after logout
        assertTrue(baClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        // we expect a ServletException
        assertTrue("Failed to find after 2nd login: ServletException", test3.contains("ServletException"));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
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
    @Test
    public void testLoginMethodFL_ValidUserIdPassword() throws Exception {
        String queryString = FormLoginClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + managerUser + "&password=" + managerPassword;
        String response = flClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

        // TEST2 - check values after logout
        assertTrue(flClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        assertTrue(flClient.verifyResponse(test3, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        List<String> passwordsInTrace = server.findStringsInLogsAndTrace(employeePassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
        // N.B. In this case we are not searching for the manager password
        //      because it is being passed in via the request URL. Since the
        //      FormLogin flow will manipulate the request URL for a redirect
        //      and trace that manipulation, we are going to see the manager
        //      password. This is not viewed as a security issue at this time.
    }

    /**
     * Verify the following:
     * <OL>
     * <LI>Attempt to access a protected servlet configured for form login
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A different, invalid userId and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called but expects an exception
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should fail for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodFL_IncorrectUserId() throws Exception {
        String queryString = FormLoginClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + invalidUser + "&password=" + invalidPassword;
        String response = flClient.accessProtectedServletWithAuthorizedCredentials(queryString, employeeUser, employeePassword);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

        // TEST2 - check values after logout
        assertTrue(flClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        // we expect a ServletException
        assertTrue("Failed to find after 2nd login: ServletException", test3.contains("ServletException"));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet configured for form login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user2) and password is passed in to the servlet for the login() method
     * <LI> 1) login() is called and should return the correct values for the passed-in user
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodFL_UnprotectedServlet() throws Exception {
        String queryString = FormLoginClient.UNPROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + managerUser + "&password=" + managerPassword;
        String response = flClient.accessUnprotectedServlet(queryString);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue(flClient.verifyResponse(test1, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));

        // TEST2 - check values after logout
        assertTrue(flClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        assertTrue(flClient.verifyResponse(test3, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected form login servlet, then login() with a valid userId (user2) and store the cookie.
     * <LI>Attempt to access a protected servlet in a new session, passing in the cookie
     * </OL>
     * <P>Expected Results:
     * <LI> The cookie allows access to the 2nd servlet, and login() is called with a valid userId (user2) and password
     * <LI> 1) login() is called but expects an exception
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testLoginMethodFL_LoginWithValidCookie() throws Exception {
        String response = flClient.accessProtectedServletWithAuthorizedCredentials(FormLoginClient.PROTECTED_PROGRAMMATIC_API_SERVLET, employeeUser, employeePassword);
        assertNotNull(response);
        String ssoCookie = flClient.getCookieFromLastLogin();

        flClient.resetClientState();

        String queryString = FormLoginClient.PROTECTED_PROGRAMMATIC_API_SERVLET + "?" +
                             METHODS + "&user=" + managerUser + "&password=" + managerPassword;
        response = flClient.accessProtectedServletWithAuthorizedCookie(queryString, ssoCookie);
        assertNotNull(response);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st login
        // we expect a ServletException if already logged in
        assertTrue("Failed to find after 1st login: ServletException", test1.contains("ServletException"));

        // TEST2 - check values after logout
        assertTrue(flClient.verifyUnauthenticatedResponse(test2));

        // TEST3 - check values after 2nd login
        assertTrue(flClient.verifyResponse(test3, managerUser, NOT_EMPLOYEE_ROLE, IS_MANAGER_ROLE));
    }

}
