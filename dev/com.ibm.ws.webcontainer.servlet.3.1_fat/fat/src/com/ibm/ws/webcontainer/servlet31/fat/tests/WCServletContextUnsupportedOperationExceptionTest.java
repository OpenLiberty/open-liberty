/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.webcontainer.servlet31.fat.FATSuite;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to ensure that an UnsupportedOperationException is thrown when certain methods
 * are invoked on the ServletContext using the following Servlet features: servlet-3.1, servlet-4.0,
 * servlet-5.0 and that the exception is not thrown for the servlet-6.0 feature.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServletContextUnsupportedOperationExceptionTest {
    private static final String TEST_PROGRAMATIC_LISTENER_ADDITION_JAR_NAME = "TestProgrammaticListenerAddition";
    private static final String TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME = "TestProgrammaticListenerAddition";

    @Server("servlet31_ServletContext_UnsupportedOperationException")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        JavaArchive TestProgrammaticListenerJar = ShrinkHelper.buildJavaArchive(TEST_PROGRAMATIC_LISTENER_ADDITION_JAR_NAME + ".jar",
                                                                                "com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners");
        TestProgrammaticListenerJar = (JavaArchive) ShrinkHelper.addDirectory(TestProgrammaticListenerJar, "test-applications/TestProgrammaticListenerAddition.jar/resources");

        WebArchive TestProgrammaticListenerApp = ShrinkHelper.buildDefaultApp(TEST_PROGRAMATIC_LISTENER_ADDITION_APP_NAME + ".war");
        TestProgrammaticListenerApp = TestProgrammaticListenerApp.addAsLibraries(TestProgrammaticListenerJar);

        ShrinkHelper.exportDropinAppToServer(server, TestProgrammaticListenerApp);

        server.startServer(WCServletContextUnsupportedOperationExceptionTest.class.getSimpleName() + ".log");

        if (FATSuite.isWindows) {
            FATSuite.setDynamicTrace(server, "*=info=enabled");
        }
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * This test case will use a ServletContainerInitializer to add a ServletContextListener in a
     * programmatic way. Then the ServletContextListener contextInitialized method calls methods
     * on the ServletContext.
     *
     * These methods should throw an UnsupportedOperationException according to the Servlet 3.1 ServletContext API.
     *
     * The following features will be tested: servlet-3.1, servlet-4.0, and servlet-5.0.
     *
     * Check to ensure that the correct message is thrown and the NLS message is resolved correctly.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void test_ProgrammaticListenerAddition_Throws_UnsupportedOperationException() throws Exception {
        // PI41941: Changed the message. Wait for the full message.
        String logMessage = server
                        .waitForStringInLog("SRVE9002E:.*\\(Operation: getEffectiveMajorVersion \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getEffectiveMajorVersion.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE9002E:.*\\(Operation: getEffectiveMinorVersion \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getEffectiveMinorVersion.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE8011E:.*\\(Operation: getDefaultSessionTrackingModes \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getDefaultSessionTrackingModes.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE8011E:.*\\(Operation: getEffectiveSessionTrackingModes \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getEffectiveSessionTrackingModes.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE8011E:.*\\(Operation: getJspConfigDescriptor \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getJspConfigDescriptor.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE9002E:.*\\(Operation: getClassLoader \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getClassLoader.", logMessage);

        logMessage = server
                        .waitForStringInLog("SRVE9002E:.*\\(Operation: getVirtualServerName \\| Listener: com.ibm.ws.webcontainer.servlet_31_fat.testprogrammaticlisteneraddition.jar.listeners.MyProgrammaticServletContextListener \\| Application: TestProgrammaticListenerAddition\\)");
        Assert.assertNotNull("The correct message was not logged for getVirtualServerName.", logMessage);

    }

    /**
     * This test case will use a ServletContainerInitializer to add a ServletContextListener in a
     * programmatic way. Then the ServletContextListener contextInitialized method calls methods
     * on the ServletContext.
     *
     * These methods should not throw an UnsupportedOperationException in the servlet-6.0 and later features.
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES, EE9_FEATURES })
    public void test_ProgrammaticListenerAddition_No_UnsupportedOperationException() throws Exception {
        String logMessage = server
                        .waitForStringInLog("UnsupportedOperationException was not thrown.");
        Assert.assertNotNull("An UnsupportedOperationException was thrown and should not have been.", logMessage);
    }
}
