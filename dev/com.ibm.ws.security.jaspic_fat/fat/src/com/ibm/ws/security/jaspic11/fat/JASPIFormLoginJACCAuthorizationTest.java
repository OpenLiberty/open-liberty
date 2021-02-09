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
 * The test verifies that JASPI authentication functions in conjunction with JACC authorization.
 * Both the JASPI and JACC providers are supplied as user defined features.
 * This tests access to a protected,form login servlet that requires jaspi to authenticate the user through various
 * callbacks (password validation, caller principal and/or group principal callback).
 * Access to the servlet also requires that the user be in the jaspi_form role for JACC authorization. The JACC
 * authorization check uses values in the roleMapping.props file as referenced in server.xml.
 *
 * This tests both positive and negative JACC authorization following JASPI authentication.
 *
 * The test server depends upon having a roleMapping.props file under <server_name>/resources/security
 * The file contains the following role definitions for form login servlet and JACC.
 *
 * # <application name>::<role name>::<type : USER, GROUP or SPECIAL>::<accessid of user, group, or special subject name (either EVERYONE or ALLAUTHENTICATEDUSERS)):
 *
 * JASPIFormLoginServlet::jaspi_form::USER::user:JaspiRealm/jaspiuser102
 * JASPIFormLoginServlet::jaspi_form::GROUP::group:JaspiRealm/JASPIGroup
 * JASPIFormLoginServlet::jaspi_form::USER::user:JaspiRealm/jaspiuser3
 * JASPIFormLoginServlet::jaspi_form::GROUP::group:JaspiRealm/group5
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIFormLoginJACCAuthorizationTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.jacc");
    protected static Class<?> logClass = JASPIFormLoginJACCAuthorizationTest.class;
    protected static String queryString = "/JASPIFormLoginServlet/JASPIForm";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIFormLoginJACCAuthorizationTest() {
        super(myServer, logClass);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.installJaccUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war", "JASPIFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_FORM_APP);

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

    @Rule
    public TestName name = new TestName();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form login with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId and password with password validation callback
     * <LI> where user is in the jaspi_form role for JACC as defined in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginJACC_ValidUserInRegistryInRole_PVCBCallback_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + PVCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd, PVCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form login with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId not in the registry with
     * <LI> caller principal and group principal callback where the group is in the required
     * <LI> jaspi_form role for JACC as defined in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and the servlet is accessed and displays subject.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginJACC_ValidUserNotInRegistryWhereGroupInRole_CPCBGPCBCallback_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + CPCBGPCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_notInRegistryNotInRoleUser, jaspi_notInRegistryNotInRolePwd,
                                           CPCBGPCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.

        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryNotInRoleUser, getRemoteUserFound + jaspi_notInRegistryNotInRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form login with JASPI
     * <LI> authentication and JACC authorization. Login with a valid userId and password with Password valdation callback and
     * <LI> a valid registry user that is NOT in the required jaspi_form role specified in the JACC roleMappings.xml file.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Servlet response contains lines to show that JASPI authentication was processed
     * <LI> (validateRequest and secureResponse) and that JACC authorization failed --
     * <LI> CWWKS9124A: Authorization by the JACC provider failed for user jaspiuser4 while invoking JASPIFormLoginServlet on /JASPIForm.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginJACC_ValidUserInRegistryNotInRole_PVCBCallback_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + PVCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_noRoleUser, jaspi_noRolePwd, PVCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        executeGetRequestNoAuthCreds(httpclient, location, HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog(MSG_JACC_AUTHORIZATION_FAILED + jaspi_noRoleUser);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
