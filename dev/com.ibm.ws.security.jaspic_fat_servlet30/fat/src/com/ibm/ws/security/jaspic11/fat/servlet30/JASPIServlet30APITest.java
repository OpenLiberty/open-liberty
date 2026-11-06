/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.jaspic11.fat.servlet30;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test Description:
 *
 * The test verifies that Servlet 3.0 methods login, logout and authenticate are handled by
 * the JASPI provider according to the JASPIC 1.1 specification section 3.9 Sub-profile for authenticate, login,
 * logout of the HttpServletRequest.
 *
 *
 * The test accesses a protected or unprotected servlet and the servlet issues the method calls to
 * login, logout and authenticate.
 *
 */
@Mode(TestMode.FULL)
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class JASPIServlet30APITest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.servlet30");
    protected static Class<?> logClass = JASPIServlet30APITest.class;
    protected static String protectedQueryString = "/JASPIServlet30/ProtectedJASPIServlet30";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIServlet30APITest() {
        super(myServer, logClass);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.installUserBundle("com.ibm.ws.security.jaspic.test_1.0");
        myServer.installUserFeature("jaspicUserTestFeature-1.0");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_SERVLET30_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
        myServer.uninstallUserBundle("com.ibm.ws.security.jaspic.test_1.0");
        myServer.uninstallUserFeature("jaspicUserTestFeature-1.0");

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
     * <LI> Attempt to access a servlet which uses programmatic security and invokes
     * <LI> the Servlet 3.0 authenticate method for a protected URL first without a userid/pwd and the with a valid user and password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 from unauthenticated request and JASPI processing
     * <LI> Return code 200 from request with valid userid/ pw
     * <LI> Responses contains:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> isAuthenticated returns true
     * <LI> getAuthType returns BASIC
     * <LI> getUserPrincipal returns WSPrincipal:jaspiuser1
     * <LI> gerRemoteUser returns jaspiuser1
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiServlet30Authenticate_ProtectedURL_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryParms = "?method=authenticate1&protected=true";
        // 1st verify 401 challenge, which implies url is protected, then re-issue authenticate request
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, HttpServletResponse.SC_UNAUTHORIZED);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, NO_NAME_VALIDATION);

        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_servlet30User,
                                                   jaspi_servlet30UserPwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_SERVLET30_NAME);
        verifyAuthenticateMethodProcessedInMessageLog("authenticate1");
        verifyAuthenticatedResponse(response, getAuthTypeJaspi, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet which uses programmatic security and invokes
     * <LI> the Servlet 3.0 authenticate method for a valid user with a bad password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403 (Unauthorized)
     * <LI> Message log shows that:
     * <LI> JASPI provider displays a message indicating authentication failed --
     * <LI> CWWKS1652A: Authentication failed with status AuthStatus.SEND_FAILUR for the web request /JASPIServlet30/ProtectedJASPIServlet30. The user defined Java Authentication
     * <LI> SPI for Containers (JASPIC) service class user.feature. JaspiProviderService has determined that the authentication data is not valid.
     * </OL>
     */
    @Test
    public void testJaspiServlet30Authenticate_ProtectedURL_BadPasswordUnauthorized() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        myServer.setMarkToEndOfLog();
        String queryParms = "?method=authenticate2&protected=true";
        executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_basicRoleUser, jaspi_invalidPwd,
                                        HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet which uses programmatic security and invokes
     * <LI> the Servlet 3.0 authenticate method for an invalid user with a bad password.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403 (Unauthorized)
     * <LI> Message log shows that:
     * <LI> JASPI provider displays a message indicating authentication failed:
     * <LI> CWWKS1652A: Authentication failed with status AuthStatus.SEND_FAILUR for the web request /JASPIServlet30/ProtectedJASPIServlet30. The user defined Java Authentication
     * <LI> SPI for Containers (JASPIC) service class user.feature. JaspiProviderService has determined that the authentication data is not valid.
     * </OL>
     */
    @Test
    public void testJaspiServlet30Authenticate_ProtectedURL_BadUserUnauthorized() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryParms = "?method=authenticate3&protected=true";
        executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_invalidUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_JASPI_AUTHENTICATION_FAILED);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a servlet which uses programmatic security and invokes
     * <LI> the Servlet 3.0 authenticate method with protected url and valid user and password followed by the logout method.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Response from authenticate is 200 and
     * <LI> getUserPrincipal returns WSPrincipal:jaspiuser1
     * <LI> getRemoteUser returns jaspiuser1
     * <LI>
     * <LI> Response from logout is 200 and
     * <LI> getUserPrincipal returns WSPrincipal:null
     * <LI> getRemoteUser returns null
     * <LI> get RunAs subject returns null
     * </OL>
     */
    @Test
    public void testJaspiServlet30AuthenticateAndLogout_ValidUserAndPasswordWithProtectedURL_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryParms = "?method=authenticate4&protected=true";
        // 1st verify 401 challenge, which implies url is protected, then re-issue authenticate request
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, HttpServletResponse.SC_UNAUTHORIZED);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, NO_NAME_VALIDATION);

        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_servlet30User,
                                                   jaspi_servlet30UserPwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_SERVLET30_NAME);
        verifyAuthenticateMethodProcessedInMessageLog("authenticate4");
        verifyAuthenticatedResponse(response, getAuthTypeJaspi, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);

        myServer.setMarkToEndOfLog();
        queryParms = "?method=logout";
        response = executeGetRequestNoAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, HttpServletResponse.SC_OK);
        verifyLogoutResponse(response);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet which uses programmatic security and invokes
     * <LI> the Servlet 3.0 login method with valid user and password. Login should result in servlet exception when
     * <LI> JASPI authentication is active.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Response from login is javax.servlet.ServletException: login method invoked while JASPI authentication active
     * </OL>
     */
    @Test
    public void testJaspiServlet30Login_ValidUserAndPasswordWithProtectedURL_FailsWithServletException() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryParms = "?method=login,user=" + jaspi_basicRoleUser + ",password=" + jaspi_basicRolePwd;
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + protectedQueryString + queryParms, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyExceptionResponse(response, SERVLET_EXCEPTION_JASPI_LOGIN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
