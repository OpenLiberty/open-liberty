/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.jaspic11.fat;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies that Form Login is handled by the JASPI provider for both
 * positive and negative cases when the JASPI user feature is present in the server.xml and
 * the application web.xml contains <login-config> with <auth-method>FORM</auth-method>.
 *
 * The test access a protected servlet, verifies that the JASPI provider was invoked to
 * make the authentication decision and verifies that the servlet response contains the correct
 * values for getAuthType, getUserPrincipal and getRemoteUser after JASPI authentication.
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIFormLoginTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat");
    protected static Class<?> logClass = JASPIFormLoginTest.class;
    protected static String queryString = "/JASPIFormLoginServlet/JASPIForm";
    protected static String queryStringUnprotected = "/JASPIFormLoginServlet/JASPIUnprotected";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIFormLoginTest() {
        super(myServer, logClass);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war", "JASPIRegistrationTestServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_FORM_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JASPIFatUtils.uninstallJaspiUserFeature(myServer);
        }
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Rule
    public TestName name = new TestName();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password in the jaspi_form role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> ---JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Messages.log contains line to show validateRequest called submitting the form with valid user and password
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> NOTE: Product design does not allow for secureResponse to be called here so it is not checked.
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password where user is in a group that is in
     * <LI> jaspi_form role and verify that JASPI authentication occurs and establishes return values
     * <LI> for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> ---JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Messages.log contains line to show validateRequest called submitting the form with valid user and password
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> NOTE: Product design does not allow for secureResponse to be called here so it is not checked.
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     *
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserWhereGroupInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleGroupUser, jaspi_formRoleGroupPwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.

        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleGroupUser, getRemoteUserFound + jaspi_formRoleGroupUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login.
     * <LI> Login with a valid userId and password but the user is not in jaspi_form role
     * <LI> required to access the servlet and verify that JASPI authentication is processed but
     * <LI> authorization fails because the user is not in the required role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS9104A in messages.log indicating authorization failed for user and role.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserNoRole_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_noRoleUser, jaspi_noRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog("CWWKS9104A:.*" + jaspi_noRoleUser + ".*" + FormRole);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login.
     * <LI> Login with a valid userId in jaspi_form role, but bad password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Form Login Error page displays
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserBadPassword_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, "BadPassword");
        String response = executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedOnFormLoginError(response, DEFAULT_JASPI_PROVIDER);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + DEFAULT_FORM_LOGIN_PAGE);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login.
     * <LI> Login with a valid userId in jaspi_form role, but null password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Form Login Error page displays
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserNullPassword_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, "BadPassword");
        String response = executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedOnFormLoginError(response, DEFAULT_JASPI_PROVIDER);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + DEFAULT_FORM_LOGIN_PAGE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login.
     * <LI> Login with a an invalid user that is not in the registry.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Form Login Error page displays
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginInvalidUser_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_invalidUser, jaspi_invalidPwd);
        String response = executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedOnFormLoginError(response, DEFAULT_JASPI_PROVIDER);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + DEFAULT_FORM_LOGIN_PAGE);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a unprotected servlet without userid and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> The security runtime sets isMandatory=false to indicate that no authentication is required for unprotected.
     * <LI> The JASPI provider displays the login page to prompt the user for ID and password. When entered, the servlet is accessed.
     * </OL>
     */
    @Test
    public void testJaspiFormLogin_UnprotectedURL_TestProviderRequiresAuthn_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Send servlet query to unprotected URL and provider displays form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringUnprotected, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedUnprotectedInMessageLog();

        // Fill in userid/password Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
