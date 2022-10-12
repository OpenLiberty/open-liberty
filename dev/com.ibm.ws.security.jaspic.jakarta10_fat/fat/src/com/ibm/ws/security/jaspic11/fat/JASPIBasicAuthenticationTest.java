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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies that Basic Authentication is handled by the JASPI provider for both
 * positive and negative cases when the JASPI user feature is present in the server.xml.
 * the application web.xml contains <login-config> with <auth-method>BASIC</auth-method>.
 *
 * The test access a protected servlet, verifies that the JASPI provider was invoked to
 * make the authentication decision and verifies that the servlet response contains the correct
 * values for getAuthType, getUserPrincipal and getRemoteUser after JASPI authentication.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class JASPIBasicAuthenticationTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat");
    protected static Class<?> logClass = JASPIBasicAuthenticationTest.class;
    protected String queryString = "/JASPIBasicAuthServlet/JASPIBasic";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIBasicAuthenticationTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war", "JASPIRegistrationTestServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_APP);
        verifyServerStartedWithJaspiFeature(myServer);
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

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password in the jaspi_basic role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuthValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password where the user is in a group in jaspi_basic role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthValidGroupInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleGroupUser, jaspi_basicRoleGroupPwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleGroupUser, getRemoteUserFound + jaspi_basicRoleGroupUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access an unprotected servlet with no user and password with JASPI activated and
     * <LI> verify that JASPI authentication is processed. When Basic authentication is used, the JASPI test provider will check the value of
     * <LI> javax.security.auth.message.MessagePolicy.isMandatory=false and will return Authn.SUCCESS (200) without
     * <LI> authenticating the user.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200 and JASPI authentication provider is invoked and returns without requiring user authentication
     * <LI> Servlet prints values for getUserPrincipal and getRemoteUser show null.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_UnprotectedServletUserNoAuthCreds_ProviderDoesNotRequireAuthn_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String queryStringUnprotected = "/JASPIBasicAuthServlet/JASPIUnprotected";
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + queryStringUnprotected, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedUnprotectedInMessageLog();
        verifyUserResponse(response, getUserPrincipalNull, getRemoteUserNull);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and password but not in the required jaspi_basic role and verify
     * <LI> that JASPI authentication is processed and authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS9104A in messages.log indicating authorization failed for user and role.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthValidUserNoRole_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_noRoleUser, jaspi_noRolePwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_AUTHORIZATION_FAILED + jaspi_noRoleUser + ".*" + BasicRole);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication first with a user in the correct role
     * <LI> followed by a user that is not in the correct role when JASPI authentication is activated. Verify that
     * <LI> authorization succeeds after JASPI authentication for user in required role, but fails when user is not in required role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> First Servlet response with valid user in role contains lines to show that JASPI authentication was processed and user can access the servlet.
     * <LI> Second Servlet response with valid user not in the jaspi_basic role, shows 403 error with
     * <LI> Message CWWKS9104A in messages.log indicating authorization failed for user and role.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthAccessWithValidRoleFollowedByInvalidRole_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Attempt access first with user in role that is allowed to access servlet
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);

        // Attempt access with user in role that is not authorized to access servlet
        httpclient = new DefaultHttpClient();
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_noRoleUser, jaspi_noRolePwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_AUTHORIZATION_FAILED + jaspi_noRoleUser + ".*" + BasicRole);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and bad password in the jaspi_basic role and verify that JASPI
     * <LI> authentication is processed and authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthBadUserPassword_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + queryString);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with a valid userId and null password in the jaspi_basic role and verify that JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthNullPassword_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, null, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + queryString);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login with an invalid userId and invalid password and verify that JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message CWWKS1652A in messages.log indicating authentication failed for user with userID.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthBadUser_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_invalidUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED + queryString);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login without credentials and verify that JASPI authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 Challenge response
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthProtectedNoAuthCreds_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestNoAuthCreds(httpclient, urlBase + queryString, HttpServletResponse.SC_UNAUTHORIZED);
        verifyJaspiAuthenticationProcessedInMessageLog();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
