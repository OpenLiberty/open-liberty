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
 * The test verifies that the JASPI provider supports the acquisition and use of a Request
 * Dispatcher within the processing for validateRequest. The RequestDispatcher is used to handle forward or include of the
 * request to a login form. Reference Java Authentication SPI for Containers JSR-196 Maintenance Release B, Version 1.1 specification
 * section 3.8.3.4.
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIRequestDispatcherTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat");
    protected static Class<?> logClass = JASPIRequestDispatcherTest.class;
    protected static String queryString = "/JASPIFormLoginServlet/JASPIForm";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIRequestDispatcherTest() {
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
            myServer.stopServer("SRVE8115W");
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
     * <LI> Attempt to access a protected resource configured for FORM login with JASPI activated.
     * <LI> The test specifies method=forward such that the JASPI provider will process a RequestDispatcher forward
     * <LI> to loginJaspi.jsp form to obtain the userID and password. The userId and password obtained from this
     * <LI> form will be used to authenticate the user before returning to the client.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> JASPI validateRequest is processed with SEND_CONTINUE and secureResponse
     * <LI> The loginJaspi.jsp login form is processed with j_security_check to obtain userid and password
     * <LI> JASPI authenticates the user and password and returns to the client.
     * </OL>
     */
    @Test
    public void testJaspiRequestDispatcher_Forward_ForwardsToJaspiLoginPage() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryParms = "?method=forward";
        // Send servlet query which triggers the JASPI provider to forward to a specified login page
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + queryParms, DEFAULT_JASPI_PROVIDER, FORM_LOGIN_JASPI_PAGE);
        verifyJaspiAuthenticationProcessedInMessageLog();
        // Execute Form login to process j_security_check so JASPI validates the ID/pw and returns control to the client
        myServer.setMarkToEndOfLog();
        executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, jaspi_formRoleUser, jaspi_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected resource configured for FORM login with JASPI activated.
     * <LI> The test specifies method=include such that the JASPI provider will process a RequestDispatcher include
     * <LI> to display the form loginJaspi.jsp and then return to the client.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> JASPI validateRequest and secureResponse are processed with SEND_CONTINUEah g
     * <LI> The loginJaspi.jsp login form is displayed and control returns to the client.
     * </OL>
     */
    @Test
    public void testJaspiRequestDispatcher_Include_DisplaysIncludedJaspiLoginPage() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String queryParms = "?method=include";
        // Send servlet query which triggers the JASPI provider to process RequestDispatcher include with specified login page
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString + queryParms, DEFAULT_JASPI_PROVIDER, FORM_LOGIN_JASPI_PAGE);
        verifyJaspiAuthenticationProcessedInMessageLog();

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
