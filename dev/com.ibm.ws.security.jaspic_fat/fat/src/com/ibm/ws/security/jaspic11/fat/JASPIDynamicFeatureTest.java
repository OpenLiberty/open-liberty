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

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
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
 * The test verifies that the JASPI user feature can be dynamically added and removed from the configuration.
 * and that JASPI authentication is activated for authentication decisions when JASPI is added to server.xml and
 * that JASPI authentication is not activated (and native authentication takes place) when JASPI is dynamically removed from
 * server.xml.
 *
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 7, runSyntheticTest = false)
@Mode(TestMode.FULL)
public class JASPIDynamicFeatureTest extends JASPITestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat.dynamic");
    protected static Class<?> logClass = JASPIDynamicFeatureTest.class;
    protected String queryString = "/JASPIBasicAuthServlet/JASPIBasic";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;

    public JASPIDynamicFeatureTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        JASPIFatUtils.installJaspiUserFeature(myServer);
        JASPIFatUtils.transformApps(myServer, "JASPIBasicAuthServlet.war");
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
        // Set cookie policy to ignore cookies so that cookies from initial access to servlet are not preserved and used after dynamic config update
        httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
    }

    @After
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();
        myServer.stopServer();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> On startup, the JASPI user feature is not included in server.xml.
     * <LI> The JASPI user feature should be able to be added dynamically and the servlet
     * <LI> should then be protected using JASPI authentication.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Native authentication used when server started initially.
     * <LI> JASPI authentication used when config is dynamically updated to add JASPI user feature.
     * </OL>
     */

    @Test
    public void noJaspiDefinedOnStartThenEnableJaspiDynamically() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // 1. Start the server with jaspi user feature,  appSecurity and servlet features enabled,
        myServer.setServerConfigurationFile(SERVLET_SECURITY_NOJASPI_SERVER_XML);
        myServer.startServer(true);
        verifyServerStarted(myServer);
        myServer.addInstalledAppForValidation(DEFAULT_APP);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

        // 2. Access the servlet and make sure everything works as expected
        // when jaspi authentication is not used
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyNoJaspiAuthentication(response, DEFAULT_JASPI_PROVIDER);

        // 3. Enable the JASPI user feature dynamically (no restart)
        myServer.setMarkToEndOfLog();
        myServer.setServerConfigurationFile(SERVLET_SECURITY_JASPI_SERVER_XML);
        verifyServerUpdatedWithJaspi(myServer, "JASPIBasicAuthServlet");

        // 4. Access the servlet and make sure everything the servlet can be accessed with JASPI authentication
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> On startup, the JASPI user feature is not included in server.xml.
     * <LI> The JASPI user feature should be able to be removed dynamically and the servlet
     * <LI> should then be protected using native authentication.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> JASPI authentication used when server started initially.
     * <LI> Native authentication used when config is dynamically updated to remove JASPI user feature.
     * </OL>
     */

    @Test
    public void JaspiDefinedOnStartThenDisableJaspiDynamically() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // 1. Start the server with jaspi user feature,  appSecurity and servlet features enabled,
        myServer.setServerConfigurationFile(SERVLET_SECURITY_JASPI_SERVER_XML);
        myServer.startServer(true);
        verifyServerStarted(myServer);
        myServer.addInstalledAppForValidation(DEFAULT_APP);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

        // 2. Access the servlet and make sure everything works as expected
        // when jaspi authentication is used
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyJaspiAuthenticationProcessedByProvider(response, DEFAULT_JASPI_PROVIDER, DEFAULT_BASICAUTH_SERVLET_NAME);

        // 3. Disable the JASPI user feature dynamically (no restart)
        myServer.setMarkToEndOfLog();
        myServer.setServerConfigurationFile(SERVLET_SECURITY_NOJASPI_SERVER_XML);
        verifyServerRemovedJaspi(myServer, "JASPIBasicAuthServlet");

        // 4. Access the servlet and make sure everything the servlet can be accessed with JASPI authentication
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, jaspi_basicRoleUser, jaspi_basicRolePwd, HttpServletResponse.SC_OK);
        verifyNoJaspiAuthentication(response, DEFAULT_JASPI_PROVIDER);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
