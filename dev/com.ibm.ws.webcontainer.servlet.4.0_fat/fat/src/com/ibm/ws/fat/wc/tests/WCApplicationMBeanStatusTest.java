/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 *
 * The testcase will send request to a servlet in TestServlet40.war
 * which has ApplicationMBean and checks for the status of the
 * TestBadServletContextListener.war web application
 * which is expected to fail to start due to runtime exception.
 *
 * PASS: if status is INSTALLED
 *
 */
@RunWith(FATRunner.class)
public class WCApplicationMBeanStatusTest {

    private final static String CLASSNAME = WCApplicationMBeanStatusTest.class.getName();
    private final static Logger LOG = Logger.getLogger(CLASSNAME);

    @Server("servlet40_stopAppStartListenerExceptionServer")
    public static LibertyServer server;

    /**
     * @throws Exception
     *
     *                       The test case expects start of the application fails due to runtime
     *                       exception on server start, add the expected ffdc.
     *                       FFDC1015I: ... RuntimeException ... com.ibm.ws.webcontainer.webapp.WebApp.notifyServletContextCreated 1341
     *                       FFDC1015I: ... RuntimeException ... com.ibm.ws.webcontainer.osgi.DynamicVirtualHost startWebApp
     *                       FFDC1015I: ... Exception ... com.ibm.ws.webcontainer.osgi.WebContainer$3 startModule async
     */
    @BeforeClass
    @ExpectedFFDC({ "java.lang.Exception", "java.lang.RuntimeException" })
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestBadServletContextListener.war to the server if not already present.");
        ShrinkHelper.defaultDropinApp(server, "TestBadServletContextListener.war", "testbadscl.listener");

        LOG.info("Setup : add TestServlet40.war to the server if not already present.");
        ShrinkHelper.defaultDropinApp(server, "TestServlet40.war", "testservlet40.servlets");

        // We can't start the server like normal as there are application startup errors.
        // Instead we'll set the log name, start the server and won't validate the applications.
        // Then we'll validate the applications on our own.
        server.setConsoleLogName(WCApplicationMBeanStatusTest.class.getSimpleName() + ".log");
        server.startServerAndValidate(true, true, false);

        // Validate the necessary application has started.
        server.validateAppLoaded("TestServlet40");

        // wait for the three ffdc which are listed above.
        server.waitForMultipleStringsInLog(3, "FFDC1015I");

        LOG.info("Setup : complete, ready for Tests");
    }

    /**
     * @throws Exception
     *
     *                       The test case expects start of the application fails due to runtime
     *                       exception in listener, add the expected errors
     *                       E SRVE0283E: Exception caught while initializing context: java.lang.RuntimeException: ..
     *                       E SRVE0265E: Error occured while notifying listeners of web application start: ..
     */
    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0283E:.*", "SRVE0265E:.*");
        }
    }

    /**
     * @throws Exception
     *
     *                       The result in logs should be INSTALLED
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void test_AppStatusInstalled() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40/ApplicationMBeanServlet?AppName=TestBadServletContextListener",
                                       "PASS: INSTALLED");
    }

    /**
     * @throws Exception
     *
     *                       The result in logs should be STARTED
     */
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES)
    public void test_AppStatusStarted() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40/ApplicationMBeanServlet?AppName=TestServlet40",
                                       "PASS: STARTED");
    }

    /**
     * Rerun the above 2 tests using the default setting of stopappstartuponlistenerexception which is true in servlet 5
     * i.e the main difference is the setting of stopappstartuponlistenerexception NOT explicitly set in the server.xml in servlet 5 (or EE9)
     * TestBadServletContextListener.war should fail to start.
     *
     * Note: though the tests are skipped when running under NO_MODIFICATION (i.e EE8/servlet4), the junit still reports the tests SUCCESS.
     * This is due to the junit old framework.
     * Example from output.txt during EE8 tests:
     *
     * Skipping test method test_AppStatusInstalled_servlet5_default on action NO_MODIFICATION_ACTION
     * Skipping test method test_AppStatusStarted_servlet5_default on action NO_MODIFICATION_ACTION
     */

    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void test_AppStatusInstalled_servlet5_default() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40/ApplicationMBeanServlet?AppName=TestBadServletContextListener",
                                       "PASS: INSTALLED");
    }

    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(SkipForRepeat.NO_MODIFICATION)
    public void test_AppStatusStarted_servlet5_default() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/TestServlet40/ApplicationMBeanServlet?AppName=TestServlet40",
                                       "PASS: STARTED");
    }
}
