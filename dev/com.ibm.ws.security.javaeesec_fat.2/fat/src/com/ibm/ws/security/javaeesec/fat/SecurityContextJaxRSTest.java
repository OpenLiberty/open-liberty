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
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SecurityContextJaxRSTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.jaxrs.fat");
    protected static Class<?> logClass = SecurityContextJaxRSTest.class;
    protected String queryString = "/securityContextHamApp";
    protected static String[] warList = { "securityContextHamApp.war" };
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;

    public SecurityContextJaxRSTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WCApplicationHelper.addWarToServerApps(myServer, "securityContextHamApp.war", true, JAR_NAME, false, "web.jar.base", "web.war.jaxrs.securitycontext");

        myServer.startServer(true);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer("CWWKS1930W");
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
     * <LI> Login with a valid userId and password in the javaeesec_basic role and verify the caller
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user principal is returned by injected securityContext getCallerPrincipal()
     * </OL>
     */
    @Test
    public void testSecurityContext_getCallerPrincipal_authenticate() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/rest/scham/callerName?username=user1&password=user1pwd";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user1", "user1pwd", HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "authenticated callerPrincipal: " + "user1");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a valid userId and password in and verify the caller
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The user principal is returned by injected securityContext getCallerPrincipal()
     * </OL>
     */
    @Test
    public void testSecurityContext_getCallerPrincipal() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/rest/scham/authCallerName";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "testuser", "testuserpwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "callerPrincipal: " + "testuser");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Try to login with a user that does not have access.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * </OL>
     */
    @Test
    public void testSecurityContext_getCallerPrincipal_noAuth() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/rest/scham/authCallerName";
        executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user1", "user1pwd",
                                        HttpServletResponse.SC_FORBIDDEN);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a user check if user is not in the role specified.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> The caller is not in the Member role
     * </OL>
     */
    @Test
    public void testSecurityContext_callerNotInRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/rest/scham/hasRole?role=Member";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "testuser", "testuserpwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "callerPrincipal testuser is not in role Member.");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a user check if user is not in the role specified.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> User is in Employee role
     * </OL>
     */
    @Test
    public void testSecurityContext_callerInRole() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/rest/scham/hasRole?role=Employee";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "testuser", "testuserpwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "callerPrincipal testuser is in role Employee.");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Access a protected servlet configured for basic authentication.
     * <LI> Login with a user check if user is not in the role specified due to the users group.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> User is in Employee role
     * </OL>
     */
    @Test
    public void testSecurityContext_callerInRole_byGroup() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        queryString = queryString + "/rest/scham/hasRole?role=Employee";
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user3", "user3pwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "callerPrincipal user3 is in role Employee.");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
