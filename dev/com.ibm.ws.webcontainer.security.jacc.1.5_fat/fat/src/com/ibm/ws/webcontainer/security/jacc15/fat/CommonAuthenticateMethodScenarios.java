/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@Ignore("This is not a test")
@RunWith(FATRunner.class)
public class CommonAuthenticateMethodScenarios {

    private static final String validRealm = "BasicRealm";
    private static final String FORMLOGOUTPAGE = "Form Logout Page";
    private static final String SUCCESSFULLOGOUT = "Successful Logout";

    protected static final String PROGRAMMATIC_API_SERVLET = "ProgrammaticAPIServlet";
    protected static final String FORMLOGINJSP = "Form Login Page";
    protected static final String validUser = "user1";
    protected static final String validPassword = "user1pwd";
    protected static final String managerUser = "user2";
    protected static final String managerPassword = "user2pwd";
    protected static final String authTypeForm = "FORM";
    protected static final String authTypeBasic = "BASIC";
    protected static final String cookieName = "LtpaToken2";

    // Keys to help readability of the test
    protected static final boolean IS_MANAGER_ROLE = true;
    protected static final boolean NOT_MANAGER_ROLE = false;
    protected static final boolean IS_EMPLOYEE_ROLE = true;
    protected static final boolean NOT_EMPLOYEE_ROLE = false;

    protected static String METHODS = null;
    protected static String REDIRECT_PAGE = null;
    protected static String REDIRECT_METHODS = null;

    protected LibertyServer server;
    protected Class<?> logClass;
    protected CommonTestHelper testHelper;

    protected CommonAuthenticateMethodScenarios(LibertyServer server, Class<?> logClass, CommonTestHelper testHelper) {
        this.server = server;
        this.logClass = logClass;
        this.testHelper = testHelper;
    }

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

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is permitted access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_ProtectedServlet1() throws Exception {
        String methodName = "testAuthenticateMethodBA_ProtectedServlet1";
        METHODS = "testMethod=authenticate,logout,login";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);

        assertNoPasswordsInServerLogFiles(methodName);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is permitted access to the protected servlet.
     * <LI> 1) login() of user2 is called but expects an exception because authentication is already established
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) authenticate() is called, returns true, and user1 is shown from API calls since it is found in the http header
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_ProtectedServlet2() throws Exception {
        METHODS = "testMethod=login,logout,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after login
        assertTrue("Failed to find after login: ServletException", test1.contains("ServletException"));
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test3, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId (user1) and password is permitted access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * <LI> 4) authenticate() is called, returns true, and user2 is shown from API calls
     * <LI> 5) logout() of user2 is called and should return null for the APIs
     * <LI> 6) authenticate() is called and returns original request user1 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_ProtectedServlet3() throws Exception {
        METHODS = "testMethod=authenticate,logout,login,authenticate,logout,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));
        String test4 = response.substring(response.indexOf("STARTTEST4"), response.indexOf("ENDTEST4"));
        String test5 = response.substring(response.indexOf("STARTTEST5"), response.indexOf("ENDTEST5"));
        String test6 = response.substring(response.indexOf("STARTTEST6"), response.indexOf("ENDTEST6"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST4 - check values after 2nd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test4, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST5 - check values after logout
        testHelper.verifyNullValuesAfterLogout(managerUser, test5, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST6 - check values after 3rd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test6, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> 1) authenticate() is called which prompts for login, log in with user1 and returns user2 info
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_UnprotectedServlet1() throws Exception {
        METHODS = "testMethod=authenticate,logout,login";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/UnprotectedProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test1, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> 1) authenticate() is called which prompts for login, log in with user1 and returns user1 info
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * <LI> 4) authenticate() is called, returns true, and user2 is shown from API calls
     * <LI> 5) logout() of user2 is called and should return null for the APIs
     * <LI> 6) authenticate() is called and returns original request user1 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_UnprotectedServlet3() throws Exception {
        METHODS = "testMethod=authenticate,logout,login,authenticate,logout,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/UnprotectedProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));
        String test4 = response.substring(response.indexOf("STARTTEST4"), response.indexOf("ENDTEST4"));
        String test5 = response.substring(response.indexOf("STARTTEST5"), response.indexOf("ENDTEST5"));
        String test6 = response.substring(response.indexOf("STARTTEST6"), response.indexOf("ENDTEST6"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST4 - check values after 2nd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test4, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST5 - check values after logout
        testHelper.verifyNullValuesAfterLogout(managerUser, test5, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST6 - check values after 3rd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test6, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) redirected to protected page, but not prompted again since already authenticated
     * <LI> 3) logout() of user1 is called and should return null for the APIs
     * <LI> 4) login() of user2 is call and should return user2 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_ProtectedRedirectToProtected() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=ProgrammaticAPIServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/AuthenticateRedirectServlet?" + METHODS + "&" + REDIRECT_METHODS
                     + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("Start initial values"), response.indexOf("STARTTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test3 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));

        // TEST1 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for basic auth.
     * <LI>Login with a valid userId (user2) and password, which has Manager access
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A valid userId and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user2 is shown from API calls
     * <LI> 2) redirected to protected page, but not prompted again since already authenticated
     * <LI> 3) however, since redirect to page is protected with Employee role which user2 does NOT have, then 403 is returned
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_ProtectedRedirectToProtectedWithRole() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=ProgrammaticAPIEmployeeRoleServlet2";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/AuthenticateRedirectServlet?" + METHODS + "&" + REDIRECT_METHODS
                     + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        authenticateWithInvalidAuthDataBA(validRealm, managerUser, managerPassword, url, PROGRAMMATIC_API_SERVLET);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet configured for basic auth.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The user is permitted access to the unprotected servlet.
     * <LI> 1) authenticate() is called, which prompts for user/pwd
     * <LI> 2) redirected to protected page, but not prompted again since already authenticated
     * <LI> 3) logout() of user1 is called and should return null for the APIs
     * <LI> 4) login() of user2 is call and should return user2 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodBA_UnprotectedRedirectToProtected() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=ProgrammaticAPIServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/basicauth/UnprotectedAuthenticateRedirectServlet?" + METHODS + "&"
                     + REDIRECT_METHODS + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        String response = authenticateWithValidAuthDataBA(validUser, validPassword, url, PROGRAMMATIC_API_SERVLET);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("Start initial values"), response.indexOf("STARTTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test3 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));

        // TEST1 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeBasic, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId (user1) and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_ProtectedServlet1() throws Exception {
        METHODS = "testMethod=authenticate,logout,login";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFL(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);

        List<String> passwordsInTrace = server.findStringsInLogsAndTrace(validPassword);
        assertEquals("Should not find password we used to initially log in with in the log file",
                     Collections.emptyList(), passwordsInTrace);
        // N.B. In this case we are not searching for the manager password
        //      because it is being passed in via the request URL. Since the
        //      FormLogin flow will manipulate the request URL for a redirect
        //      and trace that manipulation, we are going to see the manager
        //      password. This is not viewed as a security issue at this time.
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId (user1) and password permit access to the protected servlet.
     * <LI> 1) login() of user2 is called but expects an exception because authentication is already established
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) authenticate() is called, returns true, and user1 is shown from API calls since it is found in the http header
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_ProtectedServlet2() throws Exception {
        METHODS = "testMethod=login,logout_once,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFLTwice(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        // Skip test 2 because logout_once will not run logout on the 2nd pass
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after login
        assertTrue("Failed to find after login: ServletException", test1.contains("ServletException"));
        // Skip test 2 because logout_once will not run logout on the 2nd pass
        // TEST3 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test3, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId (user1) and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * <LI> 4) authenticate() is called, returns true, and user2 is shown from API calls
     * <LI> 5) logout() of user2 is called and should return null for the APIs
     * <LI> 6) authenticate() is called, returns true, and original request user1 is shown from API calls
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_ProtectedServlet3() throws Exception {
        METHODS = "testMethod=authenticate,logout,login,authenticate,logout_once,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFLTwice(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));
        String test4 = response.substring(response.indexOf("STARTTEST4"), response.indexOf("ENDTEST4"));
        // Skip test 5 because logout_once will not run logout on the 2nd pass
        String test6 = response.substring(response.indexOf("STARTTEST6"), response.indexOf("ENDTEST6"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST4 - check values after 2nd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test4, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // Skip test 5 because logout_once will not run logout on the 2nd pass
        // TEST6 - check values after 3rd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test6, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet configured for form login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> 1) authenticate() is called which prompts for login, log in with user1 and returns user1 info
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_UnprotectedServlet1() throws Exception {
        METHODS = "testMethod=authenticate,logout,login";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/UnprotectedProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFL(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet configured for form login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> No challenge or login required to access the unprotected resource
     * <LI> 1) authenticate() is called which prompts for login, log in with user1 and returns user1 info
     * <LI> 2) logout() of user1 is called and should return null for the APIs
     * <LI> 3) login() of user2 is called and should return the correct values for the passed-in user
     * <LI> 4) authenticate() is called, returns true, and user2 is shown from API calls
     * <LI> 5) logout() of user2 is called and should return null for the APIs
     * <LI> 6) authenticate() is called, returns true, and the original request user1 is shown from API calls
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_UnprotectedServlet3() throws Exception {
        METHODS = "testMethod=authenticate,logout,login,authenticate,logout_once,authenticate";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/UnprotectedProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
                     + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFLTwice(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));
        String test4 = response.substring(response.indexOf("STARTTEST4"), response.indexOf("ENDTEST4"));
        // Skip test 5 because logout_once will not run logout on the 2nd pass
        String test6 = response.substring(response.indexOf("STARTTEST6"), response.indexOf("ENDTEST6"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // TEST4 - check values after 2nd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test4, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
        // Skip test 5 because logout_once will not run logout on the 2nd pass
        // TEST6 - check values after 3rd authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test6, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId (user1) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId (user1) and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) redirected to protected page, but not prompted again since already authenticated
     * <LI> 3) logout() of user1 is called and should return null for the APIs
     * <LI> 4) login() of user2 is call and should return user2 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_ProtectedRedirectToProtected() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=ProgrammaticAPIServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/AuthenticateRedirectServlet?" + METHODS + "&" + REDIRECT_METHODS
                     + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFL(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("Start initial values"), response.indexOf("STARTTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test3 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));

        // TEST1 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected servlet configured for form login.
     * <LI>Login with a valid userId (user2) and password.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> A jsp page with login form when trying to access the protected servlet for the first time.
     * <LI> A valid userId (user2) and password permit access to the protected servlet.
     * <LI> 1) authenticate() is called, returns true, and user2 is shown from API calls
     * <LI> 2) redirected to protected page, but not prompted again since already authenticated
     * <LI> 3) however, since redirect to page is protected with Employee role which user2 does NOT have, then 403 is returned
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_ProtectedRedirectToProtectedWithRole() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=ProgrammaticAPIEmployeeRoleServlet1";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/AuthenticateRedirectServlet?" + METHODS + "&" + REDIRECT_METHODS
                     + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        authenticateWithInvalidAuthDataFL(validRealm, managerUser, managerPassword, url);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access an unprotected servlet configured for form login.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI> The user is permitted access to the unprotected page
     * <LI> 1) authenticate() is called, which prompts for user/pwd
     * <LI> 2) redirected to protected page, but not prompted again since already authenticated
     * <LI> 3) logout() of user1 is called and should return null for the APIs
     * <LI> 4) login() of user2 is call and should return user2 info
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_UnprotectedRedirectToProtected() throws Exception {
        METHODS = "testMethod=authenticate";
        REDIRECT_METHODS = "redirectMethod=logout,login";
        REDIRECT_PAGE = "redirectPage=ProgrammaticAPIServlet";
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/UnprotectedAuthenticateRedirectServlet?" + METHODS + "&"
                     + REDIRECT_METHODS + "&" + REDIRECT_PAGE + "&user=" + managerUser + "&password=" + managerPassword;
        HttpClient client = new DefaultHttpClient();
        String response = authenticateWithValidAuthDataFL(client, validUser, validPassword, url);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("Start initial values"), response.indexOf("STARTTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test3 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));

        // TEST1 - check values after authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    /**
     * Verify the following:
     * <LI>Attempt to access a protected form login servlet with a valid userId (user1) and store the cookie.
     * <LI>Attempt to access a protected servlet in a new session, passing in the cookie
     * </OL>
     * <P>Expected Results:
     * <LI> The cookie allows access to the 2nd servlet
     * <LI> 1) authenticate() is called, returns true, and user1 is shown from API calls
     * <LI> 2) logout() is called and should return null for the APIs
     * <LI> 3) login() is called and should return the correct values for the passed-in user (user2)
     * </OL>
     */
    @Test
    public void testAuthenticateMethodFL_LoginWithValidCookie() throws Exception {
        // log in to the protected servlet
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/ProgrammaticAPIServlet";
        HttpClient client1 = new DefaultHttpClient();
        String cookieReturned = authenticateWithValidAuthDataCookieFL(client1, validUser, validPassword, url);

        // log out
        String logoutUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/logout.html";
        formLogout(client1, logoutUrl, SUCCESSFULLOGOUT, "ibm_security_logout", cookieReturned);

        // attempt to go to the servlet again after logging out
        // same user can access the servlet again without logging in
        METHODS = "testMethod=authenticate,logout,login";
        url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/ProgrammaticAPIServlet?" + METHODS + "&user=" + managerUser
              + "&password=" + managerPassword;
        HttpClient client2 = new DefaultHttpClient();
        String response = authenticateWithValidCookie(client2, url, cookieReturned);

        // Get servlet output to verify each test
        String test1 = response.substring(response.indexOf("STARTTEST1"), response.indexOf("ENDTEST1"));
        String test2 = response.substring(response.indexOf("STARTTEST2"), response.indexOf("ENDTEST2"));
        String test3 = response.substring(response.indexOf("STARTTEST3"), response.indexOf("ENDTEST3"));

        // TEST1 - check values after 1st authenticate
        testHelper.verifyProgrammaticAPIValues(authTypeForm, validUser, test1, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST2 - check values after logout
        testHelper.verifyNullValuesAfterLogout(validUser, test2, NOT_MANAGER_ROLE, IS_EMPLOYEE_ROLE);
        // TEST3 - check values after 1st login
        testHelper.verifyProgrammaticAPIValues(authTypeForm, managerUser, test3, IS_MANAGER_ROLE, NOT_EMPLOYEE_ROLE);
    }

    //----------------------------------
    // utility methods
    //----------------------------------

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful basic auth is expected when accessing the servlet
     */
    private String authenticateWithValidAuthDataBA(String user, String password, String url, String urlString) {
        String methodName = "authenticateWithValidAuthDataBA";
        String debugData = "URL:" + url + "; userId:[" + user + "]" + "; password:[" + password + "]";
        Log.info(logClass, methodName, debugData);
        HttpResponse getMethod = null;
        String response = null;
        try {
            getMethod = testHelper.executeGetRequestWithAuthCreds(url, user, password);
            String authResult = getMethod.getStatusLine().toString();
            Log.info(logClass, methodName, "BasicAuth result: " + authResult);
            HttpEntity entity = getMethod.getEntity();
            response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "BasicAuth response: " + response);

            // Verify we get the correct response
            assertTrue("Expected output, " + urlString + ", not returned: " + response, response.contains(urlString));
            assertTrue("Expecting 200, got: " + authResult,
                       authResult.contains("200"));

        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData);
        }
        return response;
    }

    private void authenticateWithInvalidAuthDataBA(String realm, String user, String password, String url, String urlString) {
        String methodName = "authenticateWithInvalidAuthDataBA";
        String debugData = "URL:" + url + "; userId:[" + user + "]" + "; password:[" + password + "]";
        Log.info(logClass, methodName, debugData);
        HttpResponse getMethod = null;
        try {
            getMethod = testHelper.executeGetRequestWithAuthCreds(url, user, password);
            String authResult = getMethod.getStatusLine().toString();
            Log.info(logClass, methodName, "BasicAuth result: " + authResult);
            assertTrue("Expecting 403, got: " + authResult,
                       authResult.contains("403"));
        } catch (Exception e) {
            testHelper.handleException(e, methodName, debugData);
        }
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful form login is expected when accessing the servlet
     */
    private String authenticateWithValidAuthDataFL(HttpClient client, String user, String password, String url) {
        String methodName = "authenticateWithValidAuthDataFL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Log.info(logClass, methodName, "Verifying URL: " + debugData);
        String getResponseRedirect = null;
        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + url + ") result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Expecting form login page. Get response: " + response);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));

            // Post method to login
            HttpPost postMethod = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting form login getStatusCode 302", postResponse.getStatusLine().getStatusCode() == 302);

            // Get cookie
            // Todo: cookie name can change in future
            String ssoCookie = testHelper.getCookieValue(postResponse, cookieName);
            if (ssoCookie == null) {
                fail("LtpaToken2 not found in the cookie after login");
            }

            // Verify redirect to servlet
            Header header = postResponse.getFirstHeader("Location");
            String location = header.getValue();
            Log.info(logClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            HttpGet getMethodRedirect = new HttpGet(location);
            HttpResponse redirectResponse = client.execute(getMethodRedirect);
            authResult = redirectResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + location + ") result: " + authResult);
            entity = redirectResponse.getEntity();
            getResponseRedirect = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "Get getResponseRedirect: " + getResponseRedirect);
            assertTrue("Did not hit expected servlet: " + PROGRAMMATIC_API_SERVLET, getResponseRedirect.contains(PROGRAMMATIC_API_SERVLET));

        } catch (Exception e) {
            Log.info(logClass, methodName, "Caught unexpected Exception instead of successful form login: " + e.getMessage());
            fail("Caught unexpected Exception: " + e);
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        return getResponseRedirect;
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful form login is expected when accessing the servlet
     */
    private String authenticateWithValidAuthDataFLTwice(HttpClient client, String user, String password, String url) {
        String methodName = "authenticateWithValidAuthDataFL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Log.info(logClass, methodName, "Verifying URL: " + debugData);
        String getResponseRedirect = null;
        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + url + ")");
            Log.info(logClass, methodName, "HttpGet result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            EntityUtils.consume(entity);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Expecting form login page. Get response: " + response);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));

            // Post method to login
            String loginUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/j_security_check";
            Log.info(logClass, methodName, "Servlet loginUrl: " + loginUrl);
            HttpPost postMethod = new HttpPost(loginUrl);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting status code 302 from form login, got: " + postResponse.getStatusLine().getStatusCode(),
                       postResponse.getStatusLine().getStatusCode() == 302);

            // Get cookie
            // Todo: cookie name can change in future
            String ssoCookie = testHelper.getCookieValue(postResponse, cookieName);
            if (ssoCookie == null) {
                fail("LtpaToken2 not found in the cookie after login");
            }

            // Redirect to servlet
            Header header = postResponse.getFirstHeader("Location");
            String location = header.getValue();
            Log.info(logClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            HttpGet getMethodRedirect = new HttpGet(location);
            HttpResponse redirectResponse = client.execute(getMethodRedirect);
            authResult = redirectResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "Redirect result: " + authResult);
            entity = redirectResponse.getEntity();
            getResponseRedirect = EntityUtils.toString(entity);
            EntityUtils.consume(entity);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + redirectResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Expecting form login page. Get response: " + getResponseRedirect);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, getResponseRedirect.contains(FORMLOGINJSP));

            // Post method to login #2
            postMethod = new HttpPost(loginUrl);
            Log.info(logClass, methodName, "Servlet loginUrl: " + loginUrl);
            nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            postResponse = client.execute(postMethod);
            assertTrue("Expecting status code 302 from form login, got: " + postResponse.getStatusLine().getStatusCode(),
                       postResponse.getStatusLine().getStatusCode() == 302);

            // Get cookie
            // Todo: cookie name can change in future
            ssoCookie = testHelper.getCookieValue(postResponse, cookieName);
            if (ssoCookie == null) {
                fail("LtpaToken2 not found in the cookie after login");
            }

            // Redirect to servlet
            header = postResponse.getFirstHeader("Location");
            location = header.getValue();
            Log.info(logClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            getMethodRedirect = new HttpGet(location);
            redirectResponse = client.execute(getMethodRedirect);
            authResult = redirectResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "Redirect result: " + authResult);
            entity = redirectResponse.getEntity();
            getResponseRedirect = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "Redirect response: " + getResponseRedirect);

            assertTrue("Did not hit expected servlet: " + PROGRAMMATIC_API_SERVLET, getResponseRedirect.contains(PROGRAMMATIC_API_SERVLET));

        } catch (Exception e) {
            Log.info(logClass, methodName, "Caught unexpected Exception instead of successful form login: " + e.getMessage());
            fail("Caught unexpected Exception: " + e);
            e.printStackTrace();
        } finally {
            client.getConnectionManager().shutdown();
        }
        return getResponseRedirect;
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful form login is expected when accessing the servlet
     */
    private String authenticateWithValidAuthDataCookieFL(HttpClient client, String user, String password, String url) {
        String methodName = "authenticateWithValidAuthDataCookieFL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Log.info(logClass, methodName, "Verifying URL: " + debugData);
        String ssoCookie = null;
        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + url + ") result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Expecting form login page. Get response: " + response);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));

            // Post method to login
            HttpPost postMethod = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting form login getStatusCode 302", postResponse.getStatusLine().getStatusCode() == 302);

            // Get cookie
            // Todo: cookie name can change in future
            ssoCookie = testHelper.getCookieValue(postResponse, cookieName);
            if (ssoCookie == null) {
                fail("LtpaToken2 not found in the cookie after login");
            }

            // Verify redirect to servlet
            Header header = postResponse.getFirstHeader("Location");
            String location = header.getValue();
            Log.info(logClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            HttpGet getMethodRedirect = new HttpGet(location);
            HttpResponse redirectResponse = client.execute(getMethodRedirect);
            authResult = redirectResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + location + ") result: " + authResult);
            entity = redirectResponse.getEntity();
            String getResponseRedirect = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "Get getResponseRedirect: " + getResponseRedirect);
            assertTrue("Did not hit expected servlet: " + PROGRAMMATIC_API_SERVLET, getResponseRedirect.contains(PROGRAMMATIC_API_SERVLET));
        } catch (Exception e) {
            Log.info(logClass, methodName, "Caught unexpected Exception instead of successful form login: " + e.getMessage());
            fail("Caught unexpected Exception: " + e);
            e.printStackTrace();
        }
        return ssoCookie;
    }

    /**
     * This is an internal method used by the test methods that test the
     * unauthorized users. The users are valid but don't have access to the protected resource.
     * They all basically do the same thing and all expect a 403 exception.
     *
     * @return
     */
    private void authenticateWithInvalidAuthDataFL(String realm, String user, String password, String url) {
        String methodName = "authenticateWithInvalidAuthDataFL";
        String debugData = "\nURL:" + url +
                           "\nuserId:[" + user + "]" +
                           "\npassword:[" + password + "]\n";
        Log.info(logClass, "authenticateWithInvalidAuthData", "Verifying URL: " + debugData);
        HttpClient client = new DefaultHttpClient();
        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + url + ") result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Expecting form login page. Get response: " + response);
            // Verify we get the form login JSP
            assertTrue("Did not find expected form login page: " + FORMLOGINJSP, response.contains(FORMLOGINJSP));

            // Post method to login
            HttpPost postMethod = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/j_security_check");
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", user));
            nvps.add(new BasicNameValuePair("j_password", password));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            assertTrue("Expecting form login getStatusCode 302", postResponse.getStatusLine().getStatusCode() == 302);

            // Verify redirect to servlet
            Header header = postResponse.getFirstHeader("Location");
            String location = header.getValue();
            Log.info(logClass, methodName, "Redirect location: " + location);
            EntityUtils.consume(postResponse.getEntity());

            HttpGet getMethodRedirect = new HttpGet(location);
            HttpResponse redirectResponse = client.execute(getMethodRedirect);
            int getRedirectStatusCode = redirectResponse.getStatusLine().getStatusCode();
            Log.info(logClass, "authenticateWithInvalidAuthData", "Get getRedirectStatusCode: " + getRedirectStatusCode);
            assertTrue("Expecting 403, got: " + getRedirectStatusCode,
                       getRedirectStatusCode == 403);

        } catch (Exception e) {
            Log.info(logClass, "authenticateWithInvalidAuthData", "Caught unexpected Exception instead of form login error 403: " + e.getMessage());
            fail("Caught unexpected Exception: " + e);
            e.printStackTrace();
        }
    }

    /**
     * This is an internal method used by the test methods that test the
     * security constraints, where a successful form login is expected when accessing the servlet
     * with a cookie
     */
    private String authenticateWithValidCookie(HttpClient client, String url, String loginCookie) {
        String methodName = "authenticateWithValidCookie";
        String debugData = "\nURL:" + url;
        Log.info(logClass, "authenticateWithValidCookie", "Verifying URL: " + debugData);
        String response = null;
        try {
            // Get method on form login page
            HttpGet getMethod = new HttpGet(url);
            getMethod.setHeader("Cookie", "LtpaToken2=" + loginCookie);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet(" + url + ") result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Get response: " + response);
            // Verify we get the servlet and not the form login page
            assertTrue("Did not hit Form Login servlet: " + PROGRAMMATIC_API_SERVLET + ", authenticate with cookie failed", response.contains(PROGRAMMATIC_API_SERVLET));

            // Access servlet again, should not get prompted
            getResponse = client.execute(getMethod);
            entity = getResponse.getEntity();
            response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "Get response, 2nd time: " + response);
            // Verify we get the servlet and not the form login page
            assertTrue("Did not hit Form Login servlet 2nd time, authenticate with cookie failed", response.contains(PROGRAMMATIC_API_SERVLET));

        } catch (Exception e) {
            Log.info(logClass, methodName, "Caught unexpected Exception instead of a successful form login with cookie: " + e.getMessage());
            fail("Caught unexpected Exception: " + e);
            e.printStackTrace();
        }
        return response;
    }

    /**
     * This is an internal method used by the test methods to call the form logout
     * page and logout of the servlet
     */
    private void formLogout(HttpClient client, String theURL, String theURLId, String formURL, String loginCookie) {
        String methodName = "formLogout";
        String debugData = "\nURL:" + theURL;
        Log.info(logClass, methodName, "Verifying URL: " + debugData);
        try {
            // Get method on form logout page
            HttpGet getMethod = new HttpGet(theURL);
            HttpResponse getResponse = client.execute(getMethod);
            String authResult = getResponse.getStatusLine().toString();
            Log.info(logClass, methodName, "HttpGet result: " + authResult);
            HttpEntity entity = getResponse.getEntity();
            String response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "BasicAuth response: " + response);

            Log.info(logClass, methodName, "Expecting getStatusCode 200. getMethod.getStatusCode(): " + getResponse.getStatusLine().getStatusCode());
            Log.info(logClass, methodName, "Get response: " + response);
            // Verify we get the form logout page
            assertTrue("Form logout page not found: " + FORMLOGOUTPAGE, response.contains(FORMLOGOUTPAGE));

            // Post method to logout
            HttpPost postMethod = new HttpPost("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/" + formURL);
            Log.info(logClass, methodName, "logout URL: http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/formlogin/" + formURL);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("logout", "Logout2"));
            postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse postResponse = client.execute(postMethod);
            entity = postResponse.getEntity();
            response = EntityUtils.toString(entity);
            Log.info(logClass, methodName, "Servlet response: " + response);
            EntityUtils.consume(entity);
            Log.info(logClass, methodName, "Form logout getResponseBodyAsString: " + response);
            assertTrue("Form logout page output not found: " + theURLId, response.contains(theURLId));

            // Get cookie
            String ssoCookie = testHelper.getCookieValue(postResponse, cookieName);
            assertTrue("Expect LtpaToken2 to be empty value but got: " + ssoCookie, ssoCookie.isEmpty());
        } catch (Exception e) {
            Log.info(logClass, methodName, "Caught unexpected Exception instead of a successful form logout: " + e.getMessage());
            e.printStackTrace();
            fail("Caught unexpected Exception: " + e);
        }
    }

    protected void assertNoPasswordsInServerLogFiles(String methodName) throws Exception {
        List<String> passwordsInTrace = server.findStringsInLogsAndTrace(validPassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
        passwordsInTrace = server.findStringsInLogsAndTrace(managerPassword);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
    }

}
