/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

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
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.ServerHelper;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class SecurityContextTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = SecurityContextTest.class;
    protected String queryString = "/JavaEESecBasicAuthServlet";
    protected static String[] warList = { "JavaEESecBasicAuthServlet.war" };
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public SecurityContextTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ServerHelper.setupldapServer();
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");

        myServer.setServerConfigurationFile("securityContext.xml");
        myServer.startServer(true);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        ServerHelper.commonStopServer(myServer, Constants.HAS_LDAP_SERVER);
    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user principal is returned by injected securityContext getCallerPrincipal()
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_getCallerPrincipal_protectedServlet() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecBasic";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        // verifySecurityContextResponse(response, Constants.secContextGetPrincipal + " WSPrincipal:" + Constants.javaeesec_basicRoleUser,
        verifySecurityContextResponse(response,
                                      Constants.secContextGetPrincipalName + " " + Constants.javaeesec_basicRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a unprotected servlet
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> A Unauthenticated user principal is returned by injected securityContext getCallerPrincipal()
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_getCallerPrincipal_unprotectedServlet() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecUnprotected";
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + queryString, HttpServletResponse.SC_OK);
        // verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        // verifySecurityContextResponse(response, Constants.secContextGetPrincipal + " WSPrincipal:" + Constants.unauthenticated_user,
        verifySecurityContextResponse(response,
                                      Constants.secContextGetPrincipalNull);
        //Constants.secContextGetPrincipal + " " + Constants.secContextGetPrincipalNull);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> the caller is in the javeeesec_basic role
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user is in principal
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_isCallerInRole_inRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecBasic?role=" + Constants.BasicRole;
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, Constants.secContextIsCallerInRole + "(" + Constants.BasicRole + "): true");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password that is in a group that is in the javaeesec_basic
     * <LI> role and verify that the caller is in the javeeesec_basic role
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user is in principal
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_isCallerInRole_groupInRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecBasic?role=" + Constants.BasicRole;
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, Constants.secContextIsCallerInRole + "(" + Constants.BasicRole + "): true");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> the caller is not the javeeesec_form role
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user is in principal
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_isCallerInRole_notInRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecBasic?role=" + Constants.FormRole;
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, Constants.secContextIsCallerInRole + "(" + Constants.FormRole + "): false");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> the caller is not the javeeesec_form role
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user is in principal
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_getPrincipalsByType() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecBasic?type=Principal";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.GetPrincipalsByType number of principals: 2");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify that
     * <LI> the caller is has access to Web resource "/Protected" and method GET
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller has access, hasAccessToWebResource returns true
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_hasAccessToWebResource_hasAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/JavaEESecBasic?resource=/Protected&methods=GET";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.hasAccessToWebResource(/Protected,GET): true");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the not in javaeesec_basic role and verify that
     * <LI> the caller does not have access to the Web resource "/Protected" and method GET
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller does not have access, hasAccessToWebResource returns false
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_hasAccessToWebResource_noAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/Unprotected?resource=/Protected&methods=GET";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleGroupUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.hasAccessToWebResource(/Protected,GET): false");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the not in javaeesec_basic role and verify that
     * <LI> the caller does not have access to the Web resource "/Protected" and method GET
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller has access to POST, hasAccessToWebResource returns true
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_hasAccessToWebResource_multipleMethods_hasAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/Unprotected?resource=/Protected&methods=GET,POST";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleGroupUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.hasAccessToWebResource(/Protected,GET,POST): true");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the in javaeesec_basic role and call
     * <LI> hasAccessToWebResource with null for methods. Should test all methods and return true
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller has access, hasAccessToWebResource returns true
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_hasAccessToWebResource_nullMethods() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/Unprotected?resource=/Protected&methods=";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.hasAccessToWebResource(/Protected,): true");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password not in the javaeesec_basic role and call
     * <LI> hasAccessToWebResource with CUSTOM for methods.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller does not have access, hasAccessToWebResource returns false
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_hasAccessToWebResource_custom() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/Unprotected?resource=/CustomBasicAuth&methods=CUSTOM";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleGroupUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.hasAccessToWebResource(/CustomBasicAuth,CUSTOM): false");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in the in javaeesec_basic role and call
     * <LI> hasAccessToWebResource with null for methods. Should test all methods, custom method has access
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller has access, hasAccessToWebResource returns true
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testSecurityContext_hasAccessToWebResource_nullMethods_custom() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/Protected?resource=/CustomBasicAuth&methods=";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, Constants.javaeesec_basicRoleUser, Constants.javaeesec_basicRolePwd,
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "securityContext.hasAccessToWebResource(/CustomBasicAuth,): true");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
