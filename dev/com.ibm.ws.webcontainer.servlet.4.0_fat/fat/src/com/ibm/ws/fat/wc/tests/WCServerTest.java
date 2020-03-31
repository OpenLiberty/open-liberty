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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.fat.wc.WCApplicationHelper;

import componenttest.custom.junit.runner.FATRunner;

/**
 * All Servlet 4.0 tests with all applicable server features enabled.
 */
@RunWith(FATRunner.class)
public class WCServerTest extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(WCServerTest.class.getName());

    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

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

        WCApplicationHelper.addEarToServerDropins(SHARED_SERVER.getLibertyServer(), "TestServlet40.ear", true,
                                                  "TestServlet40.war", true, "TestServlet40.jar", true, "testservlet40.war.servlets",
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

    protected String parseResponse(WebResponse wr, String beginText, String endText) {
        String s;
        String body = wr.getResponseBody();
        int beginTextIndex = body.indexOf(beginText);
        if (beginTextIndex < 0)
            return "begin text, " + beginText + ", not found";
        int endTextIndex = body.indexOf(endText, beginTextIndex);
        if (endTextIndex < 0)
            return "end text, " + endText + ", not found";
        s = body.substring(beginTextIndex + beginText.length(), endTextIndex);
        return s;
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
     * Servlet 4.0
     *
     * @throws Exception
     *                       if something goes horribly wrong
     */
    @Test
    public void testServletHeader() throws Exception {
        WebResponse response = this.verifyResponse("/TestServlet40/MyServlet", "Hello World");

        // verify the X-Powered-By Response header
        response.verifyResponseHeaderEquals("X-Powered-By", false, "Servlet/4.0", true, false);
    }

    /**
     * Verifies that the ServletContext.getMajorVersion() returns 4 and
     * ServletContext.getMinorVersion() returns 0 for Servlet 4.0.
     *
     * @throws Exception
     */

    @Test
    public void testServletContextMajorMinorVersion() throws Exception {
        this.verifyResponse("/TestServlet40/MyServlet?TestMajorMinorVersion=true", "majorVersion: 4");

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