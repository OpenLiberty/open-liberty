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
 * The test verifies that Callbacks are processed by the JASPI provider during Form Login.
 * The callbacks which are supported and tested are:
 * - Password validation callback (PVCB)
 * - Caller Principal callback (CPCB)
 * - Group Principal callback (GPCB)
 *
 * The test access a protected servlet which interfaces with the provider to make callbacks. The test verifies that the servlet response
 * contains the correct values in the Subject based on the callback which was issued during JASPI authentication.
 *
 * This function differs from tWAS in that the CPCB can be called on its own (without PVCB) for a user that is not in the
 * registry. When the user is not in the registry an access-id must be present in the application-bnd for the role needed
 * to access the servlet.
 *
 * Note that this test relies on the group name for the Group Principal callback to be passed in the group.name property
 * in the bnd.bnd file for the JASPI provider bundle.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPICallbackFormLoginTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.callback");
    protected static Class<?> logClass = JASPICallbackFormLoginTest.class;
    protected static String queryStringForm = "/JASPICallbackTestFormLoginServlet/JASPIForm";

    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPICallbackFormLoginTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPICallbackTestBasicAuthServlet.war", "JASPICallbackTestFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_CALLBACK_FORM_APP);

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

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form authentication.
     * <LI> Use JaspiCallbackHandler for all callbacks and verify that callbacks complete successfully.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed for initial
     * <LI> login form (both validateRequest and secureResponse called) and for submission of login
     * <LI> form (only validateRequest is called).
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLogin_AllCallbacks_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringForm + ALL_CALLBACKS, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_CALLBACK_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_formRoleGroup);
        verifyRunAsUserResponse(response, jaspi_formRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form authentication.
     * <LI> Use JaspiCallbackHandler for Password Validation Callback and verify that callback completed successfully.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed for initial
     * <LI> login form (both validateRequest and secureResponse called) and for submission of login
     * <LI> form (only validateRequest is called).
     * </OL>
     */
    @Test
    public void testJaspiFormLogin_PasswordValidationCallbackOnly_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringForm + PVCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_CALLBACK_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_formRoleGroup);
        //  174210 design change - only groups in registry are associated with the user when user validated against registry wtih PVCB.
        //        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyRunAsUserResponse(response, jaspi_formRoleUser);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form authentication.
     * <LI> Use JaspiCallbackHandler for Password Validation Callback and verify that callback completed successfully.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed for initial
     * <LI> login form (both validateRequest and secureResponse called) and for submission of login
     * <LI> form (only validateRequest is called).
     * </OL>
     */
    @Test
    public void testJaspiFormLogin_ManualSubjectCreation_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringForm + MANUAL_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_CALLBACK_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_formRoleGroup);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form login.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback only with a user that is
     * <LI> NOT in the user registry and verify that callback completed and user is able to access
     * <LI> the protected servlet based on the access_id for the user being in jaspi_form role needed for servlet access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * </OL>
     */
    @Test
    public void testJaspiFormLogin_CPCBOnly_UserNotInRegistry_UserAccessIdInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringForm + CPCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_CALLBACK_FORM_LOGIN_PAGE, jaspi_notInRegistryInFormRoleUser, jaspi_notInRegistryInFormRolePwd,
                                           CPCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryInFormRoleUser, getRemoteUserFound + jaspi_notInRegistryInFormRoleUser);
        verifyNoGroupIdsResponse(response);
        verifyRunAsUserResponse(response, jaspi_notInRegistryInFormRoleUser);
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form login.
     * <LI> Use JaspiCallbackHandler for the Caller principal and Group principal callbacks with a user that is
     * <LI> NOT in the user registry and the user has access-id for the role required to access the servlet. Use GPCB to add
     * <LI> a group which has access_id defined with access to the jaspi_form role needed for the servlet access.
     * <LI> Access is allowed to the protected servlet based on application-bnd information with access_id for user and group in role.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * </OL>
     */
    @Test
    public void testJaspiFormLogin_CPCBandGPCB_UserNotInRegistry_UserAndGroupAccessIdInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringForm + CPCBGPCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_CALLBACK_FORM_LOGIN_PAGE, jaspi_notInRegistryInFormRoleUser, jaspi_notInRegistryInFormRolePwd,
                                           CPCBGPCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryInFormRoleUser, getRemoteUserFound + jaspi_notInRegistryInFormRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyRunAsUserResponse(response, jaspi_notInRegistryInFormRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for form login.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback only with a user that is
     * <LI> in the user registry and has the required role and verify that callback completed and servlet is accessed.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * </OL>
     */
    @Test
    public void testJaspiFormLogin_CPCBandGPCB_UserInRegistryInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryStringForm + CPCBGPCB_CALLBACK, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_CALLBACK_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd, CPCBGPCB_CALLBACK_DESCR);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_CALLBACK_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_formRoleUser, getRemoteUserFound + jaspi_formRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_formRoleGroup);
        //  174210 design change - only groups in registry are associated with the user when user validated against registry wtih PVCB.
//        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyRunAsUserResponse(response, jaspi_formRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
