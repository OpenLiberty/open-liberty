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
 * The test verifies the JASPI AuthConfigFactory factory.getConfigProvider
 * returns information for the JASPI non-persistent provider which is registered
 * at runtime.
 *
 * Get provider registration for the runtime provider should return
 *
 * Message layer: null
 * App Context: null
 * IsPersistent: false
 * Provider Class: class com.ibm.ws.security.jaspi.test.AuthProvider
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIRuntimeRegistrationTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.registration");
    protected static Class<?> logClass = JASPIRuntimeRegistrationTest.class;
    protected static String queryString = "/JASPIRegistrationTestServlet/JASPIBasic?GET**HttpServlet**default_host__JASPIRegistrationTestServlet**com.ibm.ws.security.jaspi.test.AuthProvider";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIRuntimeRegistrationTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIRegistrationTestServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_REGISTRATION_APP);

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
     * <LI> Login with a valid userId and password in the jaspi_basic role and verify that the servlet can
     * <LI> be accessed and that the JASPI runtime provider registration information is displayed by the servlet.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that
     * <LI> Message layer: null
     * <LI> App Context: null
     * <LI> IsPersistent: false
     * <LI> Provider Class: class com.ibm.ws.security.jaspi.test.AuthProvider
     * </OL>
     */
    @Test
    public void testJaspiRuntimeRegistrationTest_ReturnsProvider() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd,
                                                          HttpServletResponse.SC_OK);

        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_REGISTRATION_SERVLET_NAME);
        verifyRuntimeProviderRegistration(response, DEFAULT_PROVIDER_CLASS);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
