/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.util.Collections;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;

/**
 * All Servlet 4.0 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServerTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());
    private static final String SERVLET_40_APP_NAME = "TestServlet40";

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet40_wcServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), SERVLET_40_APP_NAME + ".ear", true,
                                                  SERVLET_40_APP_NAME + ".war", true, SERVLET_40_APP_NAME + ".jar", true, "testservlet40.war.servlets",
                                                  "testservlet40.war.listeners", "testservlet40.jar.servlets");

        SHARED_SERVER.startIfNotStarted();
        WCApplicationHelper.waitForAppStart("TestServlet40", WCServerTest.class.getName(), SHARED_SERVER.getLibertyServer());
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        SHARED_SERVER.getLibertyServer().stopServer();
    }

    /**
     * Request a simple servlet.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleServlet() throws Exception {
        this.verifyResponse("/TestServlet40/SimpleTestServlet", "Hello World");
    }

    /**
     * Simple test to a servlet then read the header to ensure we are using
     * Servlet 4.0. This test looks for the X-Powered-By header specifically.
     *
     * This test is skipped for EE9_FEATURES repeat because for servlet-5.0 + the
     * X-Powered-By header is going to be disabled by default.
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void testServletXPoweredByHeader() throws Exception {
        WebResponse response = this.verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // verify the X-Powered-By Response header is present by default and equals Servlet/4.0
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/4.0", true, false);
    }

    /**
     * Simple test to a servlet then read the header to ensure the X-Powered-By
     * header is disabled on Servlet-5.0
     *
     * This test is skipped for NO_MODIFICATION(servlet-4.0 feature).
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testServletXPoweredByHeader_Servlet50_Default() throws Exception {
        WebResponse response = this.verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // verify the X-Powered-By Response header is not present by default.
        response.verifyResponseHeaderExists("X-Powered-By", false, false);
    }

    /**
     * Simple test to a servlet then read the header to ensure that when the
     * X-Powered-By header is enabled it contains Servlet/5.0.
     *
     * This test is skipped for NO_MODIFICATION(servlet-4.0 feature).
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void testServletXPoweredByHeader_Servlet50_Enabled() throws Exception {
        // Save the current server configuration
        SHARED_SERVER.getLibertyServer().saveServerConfiguration();

        // Enable the X-PoweredByHeader
        ServerConfiguration configuration = SHARED_SERVER.getLibertyServer().getServerConfiguration();
        LOG.info("Server configuration that was saved: " + configuration);

        configuration.getWebContainer().setDisablexpoweredby(false);

        SHARED_SERVER.getLibertyServer().setMarkToEndOfLog();
        SHARED_SERVER.getLibertyServer().updateServerConfiguration(configuration);

        SHARED_SERVER.getLibertyServer().waitForConfigUpdateInLogUsingMark(Collections.singleton(SERVLET_40_APP_NAME));

        // Execute the test
        WebResponse response = this.verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // verify the X-Powered-By Response header is present and equals Servlet/5.0
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/5.0", true, false);

        // Restore the original server configuration
        SHARED_SERVER.getLibertyServer().setMarkToEndOfLog();
        SHARED_SERVER.getLibertyServer().restoreServerConfiguration();
        SHARED_SERVER.getLibertyServer().waitForConfigUpdateInLogUsingMark(Collections.singleton(SERVLET_40_APP_NAME));
    }

    /**
     * Verifies that the ServletContext.getMajorVersion() returns 4 and
     * ServletContext.getMinorVersion() returns 0 for Servlet 4.0.
     *
     * @throws Exception
     */

    @Test
    public void testServletContextMajorMinorVersion() throws Exception {
        String majorVersionExpectedResult = "majorVersion: 4";
        if (JakartaEE9Action.isActive()) {
            majorVersionExpectedResult = "majorVersion: 5";
        }
        this.verifyResponse("/TestServlet40/MyServlet?TestMajorMinorVersion=true", majorVersionExpectedResult);

        this.verifyResponse("/TestServlet40/MyServlet?TestMajorMinorVersion=true", "minorVersion: 0");
    }

    /**
     * Test the setSessionTimeout and getSessionTimeout methods from the servlet
     * context API.
     *
     * @throws Exception
     */
    @Test
    public void testServletContextSetAndGetSessionTimeout() throws Exception {
        WebBrowser wb = createWebBrowserForTestCase();

        // First request will get a new session and will verify that the
        // getSessionTimeout method returns the correct timeout.
        this.verifyResponse(wb, "/TestServlet40/SessionTimeoutServlet?TestSessionTimeout=new",
                            new String[] { "Session Timeout: 1", "Session object: # HttpSessionImpl #",
                                           "max inactive interval : 60", "valid session : true", "new session : true" });

        // Second request will verify that the session is still the same, so no
        // new session has been created
        this.verifyResponse(wb, "/TestServlet40/SessionTimeoutServlet?TestSessionTimeout=current",
                            new String[] { "Session object: # HttpSessionImpl #", "max inactive interval : 60",
                                           "valid session : true", "new session : false" });

        Thread.sleep(70000); // Wait 70 seconds or 1 minute and 10 seconds

        // Third request will verify if session was invalidated after 70 seconds
        // or 1 minute and 10 seconds.
        this.verifyResponse("/TestServlet40/SessionTimeoutServlet?TestSessionTimeout=current",
                            new String[] { "Session object: null", "Session Invalidated" });
    }
}