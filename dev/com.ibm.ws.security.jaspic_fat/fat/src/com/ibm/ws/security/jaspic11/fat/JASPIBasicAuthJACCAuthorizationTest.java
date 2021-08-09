/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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
 * The test verifies that JASPI authentication functions in conjunction with JACC authorization.
 * Both the JASPI and JACC providers are supplied as user defined features.
 * This tests access to a protected, basic auth servlet that requires JASPI to authenticate the user through various
 * callbacks (password validation, caller principal and/or group principal callback).
 * Access to the servlet also requires that the user be in the jaspi_basic role for JACC authorization. The JACC
 * authorization check uses values in the roleMapping.props file as referenced in server.xml.
 *
 * This tests both positive and negative authorization following successful JASPI authentication.
 * There is also a test for negative JASPI authentication to insure that path is working as expected and returns authentication failure.
 *
 * The test server depends upon having a roleMapping.props file under <server_name>/resources/security
 * The file contains the following role definitions for basic auth servlet and JACC.
 *
 * # <application name>::<role name>::<type : USER, GROUP or SPECIAL>::<accessid of user, group, or special subject name (either EVERYONE or ALLAUTHENTICATEDUSERS)):
 *
 * JASPIBasicAuthServlet::jaspi_basic::USER::user:JaspiRealm/jaspiuser1
 * JASPIBasicAuthServlet::jaspi_basic::GROUP::group:JaspiRealm/group2
 * JASPIBasicAuthServlet::jaspi_basic::USER::user:JaspiRealm/jaspiuser101
 * JASPIBasicAuthServlet::jaspi_basic::GROUP::group:JaspiRealm/JASPIGroup
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIBasicAuthJACCAuthorizationTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.jacc");
    protected static Class<?> logClass = JASPIBasicAuthJACCAuthorizationTest.class;
    protected String queryString = "/JASPIBasicAuthServlet/JASPIBasic";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIBasicAuthJACCAuthorizationTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.installJaccUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeatureAndJacc(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            JASPIFatUtils.uninstallJaspiUserFeature(myServer);
            JASPIFatUtils.uninstallJaccUserFeature(myServer);
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
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId and password in
     * <LI> the jaspi_basic role in JACC as defined in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthJACC_ValidUserInRole_AllCallbacks_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId and password. The user is
     * <LI> in the jaspi_basic role required for JACC as defined in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthJACC_ValidUserInRoleInRegistry_CPCBCallback_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString + CPCB_CALLBACK, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI
     * <LI> authentication with password validation callback (PVCB) and JACC authorization. Login with a valid userId and password. The user is
     * <LI> in the jaspi_basic role required for JACC as defined in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthJACC_ValidUserInRoleInRegistry_PVCBCallback_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString + PVCB_CALLBACK, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI
     * <LI> authentication with caller principal callback (CPCB) and JACC authorization. Login with a valid userId and password. The user not
     * <LI> in the user registry but has a user access-id in the jaspi-basic role in the JACC role mappings.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthJACC_ValidUserInRoleNotInRegistryUserAccessId_CPCBCallback_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString + CPCB_CALLBACK, jaspi_notInRegistryInBasicRoleUser, jaspi_notInRegistryInBasicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryInBasicRoleUser, getRemoteUserFound + jaspi_notInRegistryInBasicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI
     * <LI> authentication with Caller and group principal callbacks and JACC authorization. Login with a valid userId and password. The user not
     * <LI> in the user registry but is in the JASPIGroup that has a group access-id in the jaspi_basic role in the JACC role mappings.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthJACC_ValidUserInRoleNotInRegistryGroupAccessId_CPCBGPCGCallback_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString + CPCBGPCB_CALLBACK, jaspi_notInRegistryNotInRoleUser,
                                                          jaspi_notInRegistryNotInRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryNotInRoleUser, getRemoteUserFound + jaspi_notInRegistryNotInRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId and password and with
     * <LI> the user that is NOT in the required jaspi_basic specified in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and that JACC authorization failed --
     * <LI> CWWKS9124A: Authorization by the JACC provider failed for user jaspiuser4 while invoking JASPIBasicAuthServlet on /JASPIBasic.
     * </OL>
     */
    @Test
    public void testJaspiBasicAuthJACC_ValidUserNotInRole_CPCBCallback_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString + CPCB_CALLBACK, jaspi_noRoleUser, jaspi_noRolePwd, HttpServletResponse.SC_FORBIDDEN);
        verifyJaspiAuthenticationProcessedInMessageLog();
        verifyMessageReceivedInMessageLog(MSG_JACC_AUTHORIZATION_FAILED + jaspi_noRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI and JACC activated.
     * <LI> Login with a valid userId and bad password in the jaspi_basic role and verify that JASPI
     * <LI> authentication is processed and authentication fails.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Message from JASPI provider indicating that authentication failed.
     * </OL>
     */

    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuthJACC_BadUserPassword_AuthenticationFails() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_invalidPwd, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(PROVIDER_AUTHENTICATION_FAILED);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
