/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to ensure that an UnsupportedOperationException is thrown when certain methods
 * are invoked on the ServletContext.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServletContextUnsupportedOperationExceptionTest {

    private static final Logger LOG = Logger.getLogger(WCServletContextUnsupportedOperationExceptionTest.class.getName());
    private static final String TEST_PROGRAMATIC_LISTENER_ADDITION_JAR_NAME = "TestProgrammaticListenerAddition";
    private static final String TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME = "TestProgrammaticListenerAddition";

    @Server("servlet40_ServletContext_UnsupportedOperationExceptionr")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestServlet40 to the server if not already present.");

        JavaArchive testProgrammaticListenerJar = ShrinkHelper.buildJavaArchive(TEST_PROGRAMATIC_LISTENER_ADDITION_JAR_NAME + ".jar",
                                                                                "com.ibm.ws.webcontainer.servlet_40_fat.testprogrammaticlisteneraddition.jar.listeners");

        WebArchive testProgrammaticListenerApp = ShrinkHelper.buildDefaultApp(TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME + ".war");
        testProgrammaticListenerApp = testProgrammaticListenerApp.addAsLibraries(testProgrammaticListenerJar);

        ShrinkHelper.exportDropinAppToServer(server, testProgrammaticListenerApp);

        server.startServer(WCServletContextUnsupportedOperationExceptionTest.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test case will use a ServletContainerInitializer to add a ServletContextListener in a
     * programmatic way. Then in the ServletContextListener contextInitialized method calls methods
     * on the ServletContext.
     *
     * These methods should throw an UnsupportedOperationException according to the Servlet 4.0 ServletContext API.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void test_ProgrammaticListenerAddition_Throws_UnsupportedOperationException() throws Exception {
        // Ensure that the proper exception was output
        String logMessage = server
                        .waitForStringInLog("SRVE8011E:.*\\(Operation: getSessionTimeout \\| Listener: com.ibm.ws.webcontainer.servlet_40_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getDefaultSessionTrackingModes.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE8011E:.*\\(Operation: getRequestCharacterEncoding \\| Listener: com.ibm.ws.webcontainer.servlet_40_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getDefaultSessionTrackingModes.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE8011E:.*\\(Operation: getResponseCharacterEncoding \\| Listener: com.ibm.ws.webcontainer.servlet_40_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getDefaultSessionTrackingModes.", logMessage);
    }

    /**
     * This test case will use a ServletContainerInitializer to add a ServletContextListener in a
     * programmatic way. Then in the ServletContextListener contextInitialized method calls methods
     * on the ServletContext.
     *
     * These methods should not throw an UnsupportedOperationException in the Servlet 6.0 and later Specifications.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES, EE9_FEATURES })
    public void test_ProgrammaticListenerAddition_No_UnsupportedOperationException() throws Exception {
        String logMessage = server.waitForStringInLog("UnsupportedOperationException was not thrown.");
        Assert.assertNotNull("An UnsupportedOperationException was thrown and should not have been.", logMessage);
    }
}
