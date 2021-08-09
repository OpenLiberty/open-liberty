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
 * The test verifies that Callbacks are processed by the JASPI provider during Basic Authentication.
 * The callbacks which are supported and tested are:
 *
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
public class JASPICallbackBasicAuthTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.callback");
    protected static Class<?> logClass = JASPICallbackBasicAuthTest.class;
    protected static String queryStringBasic = "/JASPICallbackTestBasicAuthServlet/JASPIBasic";

    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPICallbackBasicAuthTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPICallbackTestBasicAuthServlet.war", "JASPICallbackTestFormLoginServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_CALLBACK_APP);

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
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for all callbacks and verify that callbacks complete successfully.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * <LI> CallerSubject, Public credential contins groupIds - group1 (from registry) JASPIGroup is not associated
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_AllCallbacks_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + ALL_CALLBACKS, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);

        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        //  174210 design change only groups in registry are associated with the user when user validated against registry wtih PVCB.
        //        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_basicRoleGroup);
        verifyRunAsUserResponse(response, jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use the JASPI provider skipping all callbacks and adding the user and password to the subject manually.
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
    public void testJaspiBasicAuth_ManualSubjectCreation_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + MANUAL_CALLBACK, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        verifyRunAsUserResponse(response, jaspi_basicRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_basicRoleGroup);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Password validation callback and verify that callback completed successfully.
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
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuth_PasswordValidationCallbackOnly_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + PVCB_CALLBACK, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_basicRoleGroup);
        verifyRunAsUserResponse(response, jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback with no userid/pwd and
     * <LI> verify that this results in an unauthenticed user.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 - Unauthorized.
     * <LI> JASPI authentication is processed as verified in messages.log
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_CPCBOnly_NoUserCredentials_UnauthenticatedUser() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        executeGetRequestNoAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_CALLBACK, HttpServletResponse.SC_UNAUTHORIZED);
        verifyJaspiAuthenticationProcessedInMessageLog();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback only with a user that is
     * <LI> not in the user registry and also does not have the role required to access the servlet.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 - Unauthorized.
     * <LI> JASPI authentication is processed as verified in messages.log
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_CPCBOnly_UserNotInRegistryNotInRole_AuthorizationFailed() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_CALLBACK, jaspi_notInRegistryNotInRoleUser,
                                                          jaspi_notInRegistryNotInRolePwd,
                                                          HttpServletResponse.SC_FORBIDDEN);
        verifyResponseAuthorizationFailed(response);
        verifyJaspiAuthenticationProcessedInMessageLog();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback only with a user that is
     * <LI> NOT in the user registry and verify that callback completed and user is able to access
     * <LI> the protected servlet based on the access_id for the user being in jaspi_basic role needed for servlet access.
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
    public void testJaspiBasicAuth_CPCBOnly_UserNotInRegistryAccessIdInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_CALLBACK, jaspi_notInRegistryInBasicRoleUser,
                                                          jaspi_notInRegistryInBasicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryInBasicRoleUser, getRemoteUserFound + jaspi_notInRegistryInBasicRoleUser);
        verifyNoGroupIdsResponse(response);
        verifyRunAsUserResponse(response, jaspi_notInRegistryInBasicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Group principal callback. The JASPI bnd.bnd file specifies a group.name property.
     * <LI> The Basicauth header contains a user NOT in the user registry and the user is not in the role required to access the servlet.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403 - Unauthorized
     * <LI> JASPI authentication is processed as verified in messages.log
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_GPCBOnly_UserNotInRegistry_AuthenticationFailed() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + GPCB_CALLBACK, jaspi_notInRegistryNotInRoleUser,
                                                          jaspi_notInRegistryNotInRolePwd,
                                                          HttpServletResponse.SC_FORBIDDEN);
        verifyJaspiAuthenticationProcessedInMessageLog();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal and Group principal callbacks with a user that is
     * <LI> NOT in the user registry and the user is not in the role required to access the servlet. Use GPCB to add
     * <LI> a group which has access_id defined with access to the jaspi_basic role needed for the servlet access.
     * <LI> Access is allowed to the protected servlet based on application-bnd information with access_id for group in role.
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
    public void testJaspiBasicAuth_CPCBandGPCB_UserNotInRegistry_UserAccessIdInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCBGPCB_CALLBACK, jaspi_notInRegistryInBasicRoleUser,
                                                          jaspi_notInRegistryInBasicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryInBasicRoleUser, getRemoteUserFound + jaspi_notInRegistryInBasicRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyRunAsUserResponse(response, jaspi_notInRegistryInBasicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal and Group principal callbacks with a user that is
     * <LI> NOT in the user registry and the user is not in the role required to access the servlet. Use GPCB to add
     * <LI> a group which has access_id defined with access to the jaspi_basic role needed for the servlet access.
     * <LI> Access is allowed to the protected servlet based on application-bnd information with access_id for group in role.
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
    public void testJaspiBasicAuth_CPCBandGPCB_UserNotInRegistry_GroupAccessIdInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCBGPCB_CALLBACK, jaspi_notInRegistryNotInRoleUser,
                                                          jaspi_notInRegistryNotInRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_notInRegistryNotInRoleUser, getRemoteUserFound + jaspi_notInRegistryNotInRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyRunAsUserResponse(response, jaspi_notInRegistryNotInRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Password validation and Group principal callbacks with a user that is
     * <LI> in the user registry and in the role required to access the servlet. The GPCB adds another group to
     * <LI> the list of groups to which the user has access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI authentication was processed:
     * <LI> JASPI validateRequest called with auth provider=<provider_name>
     * <LI> JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Subject contains correct values for getUserPrincipal, getRemoteUser
     * <LI> CallerSubject, Public credential contins groupIds - group1 (from registry) JASPIGroup is not associated
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_PVCBandGPCB_UserInRegistry_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + PVCBGPCB_CALLBACK, jaspi_basicRoleUser,
                                                          jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        //  174210 design change - only groups in registry are associated with the user when user validated against registry wtih PVCB.
        //  verifyGroupIdsResponse(response, DEFAULT_REALM, JASPI_GROUP);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_basicRoleGroup);
        verifyRunAsUserResponse(response, jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
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
    public void testJaspiBasicAuth_CPCBOnly_UserInRegistryInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_CALLBACK, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        verifyGroupIdsResponse(response, DEFAULT_REALM, jaspi_basicRoleGroup);
        verifyRunAsUserResponse(response, jaspi_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler with a CallerPrincipalCallback with a Principal with a user that is
     * <LI> in the user registry and has the required role and verify that callback completed, the servlet
     * is accessed, and that req.getUserPrincipal() returns the Principal from the callback
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
    public void testJaspiBasicAuth_CPCB_Principal_UserInRegistryInRole_Successful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_PRINCIPAL_CALLBACK, jaspi_basicRoleUserPrincipal, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_CALLBACK_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFoundJaspiPrincipal, getRemoteUserFound + jaspi_basicRoleUserPrincipal);
        verifyRunAsUserResponse(response, jaspi_basicRoleUserPrincipal);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication.
     * <LI> Use JaspiCallbackHandler for the Caller principal callback only with a user that is
     * <LI> in the user registry but not in the required role and verify that user is unauthorized.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 - Unauthorized
     * <LI> JASPI authentication is processed as verified in messages.log
     * </OL>
     */
    @Test
    public void testJaspiBasicAuth_CPCBOnly_UserInRegistryNotInRole_AuthorizationFailed() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + CPCB_CALLBACK, jaspi_noRoleUser, jaspi_noRolePwd,
                                                          HttpServletResponse.SC_FORBIDDEN);
        verifyResponseAuthorizationFailed(response);
        verifyJaspiAuthenticationProcessedInMessageLog();
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with no callbacks.
     * <LI> Verify that authentication fails when no callbacks are called.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 - Unauthorized
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiBasicAuth_NoCallbacks_AuthenticationFailed() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryStringBasic + NO_CALLBACKS, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
