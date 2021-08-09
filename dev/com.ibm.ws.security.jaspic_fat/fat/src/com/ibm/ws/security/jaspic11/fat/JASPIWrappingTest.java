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
 * The test verifies that JASPI authentication provider supports the wrapping and unwrapping of the requests
 * and responses. Reference Java Authentication SPI for Containers JSR-196 Maintenance Release B, Version 1.1 specification
 * section 3.8.3.5.
 *
 * The test case indicates to the JASPI provider that it should wrap the request and response in its httpServletRequestWrapper and httpServletResponseWrapper.
 * A servlet is invoked and then accesses a method in the httpServletRequestWrapper and httpServletResponseWrapper to verify
 * that the request was wrapped by JASPI.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIWrappingTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.wrapping");
    protected static Class<?> logClass = JASPIWrappingTest.class;
    protected String queryString = "/JASPIWrappingServlet/JASPIUnprotected";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIWrappingTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIWrappingServlet.war");

        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_WRAPPING_APP);

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
     * <LI> Attempt to access a servlet configured for basic authentication with JASPI activated and
     * <LI> servlet parameter indicating that the httpServletRequest and httpServletResponse should be wrapped by the JASPI provider's
     * <LI> httpServletRequestWrapper and httpServletResponseWrapper. The servlet will then verify access to a method in
     * <LI> the request and response wrapper to verify that the request was wrapped.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Servlet response contains lines to show that JASPI request and response wrapping was processed:
     * <LI> The httpServletRequest has been wrapped by httpServletRequestWrapper.
     * <LI> The httpServletRestponse has been wrapped by httpServletResponseWrapper.
     * </OL>
     */
    @Test
    public void testJaspiRequestAndResponseWrapping_UnwrapAtServletSuccessful() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String queryParms = "?method=wrap";
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + queryString + queryParms, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, "JASPIWrappingServlet");
        verifyJaspiRequestAndResponseWrapping(response);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
