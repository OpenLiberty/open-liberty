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
 * The test verifies that JASPI handles the callback property javax.servlet.http.registerSession=true
 * and returns this callback property to the Liberty runtime. Reference Java Authentication SPI for Containers JSR-196 Maintenance Release B, Version 1.1 specification
 * section 3.8.4.
 *
 * The Liberty runtime sets the value for userPrincipal so that the JASPI provider can
 * use getUserPrincipal of a previous registerSession=true was set. This test consists of
 * both a positive and negative test for registerSession.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIRegisterSessionTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat");
    protected static Class<?> logClass = JASPIRegisterSessionTest.class;
    protected String queryString = "/JASPIBasicAuthServlet/JASPIBasic";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIRegisterSessionTest() {
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
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login without credentials and verify that JASPI authentication returns 401 challenge. Then
     * <LI> log in with valid ID/pw and verify the servlet is accessed. Try a second time to access the servlet
     * <LI> without credentials and verify that 403 is returned since userPrincipal is passed from
     * <LI> the runtime to the JASPI provider from the initial successful access to the servlet.
     * <LI> In order to avoid hitting the cached data, the user id should be unique within in this test class.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 challenge after initial access with no credentials.
     * <LI> Servlet is accessed successfully when credentials (userid/pw) are supplied.
     * <LI> Return code 401 challenge for attempt to access servlet without credentials.
     * </OL>
     */
    @Test
    public void testJaspiRegisterSession_SuccessfulAccessFollowedByNoAuthCredsWithoutRegisterSession_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // For protected servlet, verify that JASPI provider challenges with 401 if not credentials supplied.
        executeGetRequestNoAuthCreds(httpclient, urlBase + queryString, HttpServletResponse.SC_UNAUTHORIZED);

        // Access servlet by supplying credentials should result in server accessed.
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleUser, getRemoteUserFound + jaspi_basicRoleUser);
        httpclient.getCredentialsProvider().clear();
        // Accessing the same servlet again results in 401 no userPrincipal is set by the runtime
        executeGetRequestNoAuthCreds(httpclient, urlBase + queryString, HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for basic authentication with JASPI activated.
     * <LI> Login without credentials and verify that JASPI authentication returns 401 challenge. Then
     * <LI> log in with valid ID/pw and verify the servlet is accessed. Try a second time to access the servlet
     * <LI> without credentials and verify that no 401 challenge is presented again since the JASPI provider
     * <LI> set javax.servlet.http.registerSession=true and the runtime sets a value in the request for getUserPrincipal.
     * <LI> The JASPI provider issues a caller principal callback for the user name from getPrincpal.getName().
     * <LI> In order to avoid hitting the cached data, the user id should be unique within in this test class.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401 challenge after initial access with no credentials.
     * <LI> Servlet is accessed successfully when credentials (userid/pw) are supplied.
     * <LI> Return code 200 without a challenge when we attempt to access servlet again without credentials because runtime has
     * <LI> set a value for getUserPrincipal and the JASPI provider uses this value to authenticate with the Caller Principal callback.
     * </OL>
     */
    @Test
    public void testJaspiRegisterSession_SuccessfulAccessFollowedByNoAuthCredsWithRegisterSession_AllowsAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // For protected servlet, verify that JASPI provider challenges with 401 if not credentials supplied.
        executeGetRequestNoAuthCreds(httpclient, urlBase + queryString, HttpServletResponse.SC_UNAUTHORIZED);

        // Access servlet by supplying credentials should result in servlet accessed. JASPI provider will set registerSession=true for runtime.
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString + "?method=registerSession", jaspi_basicRoleGroupUser, jaspi_basicRoleGroupPwd,
                                                          HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + jaspi_basicRoleGroupUser, getRemoteUserFound + jaspi_basicRoleGroupUser);
        httpclient.getCredentialsProvider().clear();
        // Accessing the same servlet again results successful authentication because JASPI provider used the userPrincipal provided by runtime
        String response2 = executeGetRequestNoAuthCreds(httpclient, urlBase + queryString + "?method=processRegisteredSession", HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response2, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);
        verifyUserResponse(response2, getUserPrincipalFound + jaspi_basicRoleGroupUser, getRemoteUserFound + jaspi_basicRoleGroupUser);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
