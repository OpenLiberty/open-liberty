/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a test to ensure that when an Exception is thrown by a ServletContainerInitializer while
 * invoking the onStartup method that a valid Warning message is displayed and not a translation key.
 *
 * Previously the following was displayed in the logs:
 *
 * [WARNING ] exception.occurred.while.running.ServletContainerInitializers.onStartup
 *
 * Just adding this as part of the Servlet 4.0 test bucket as we don't need to test it with more
 * than one Servlet version, the message is in the core WebContainer bundle so if it works here it will
 * work in the other Servlet features as well.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class WCServletContainerInitializerExceptionTest {
    private static final String APP_NAME = "SCIExceptionTest.war";
    private static final String JAR_NAME = "SCIExceptionTest.jar";

    @Rule
    public TestName name = new TestName();

    @Server("servlet40_SCIException")
    public static LibertyServer wcServer;

    @BeforeClass
    public static void before() throws Exception {
        JavaArchive sciExceptionTestJar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        sciExceptionTestJar.addPackage("com.ibm.ws.servlet40.exception.sci");

        ShrinkHelper.addDirectory(sciExceptionTestJar, "test-applications/" + JAR_NAME + "/resources");

        // Create the SCIExceptionTest.war application
        WebArchive sciExceptionTestWar = ShrinkWrap.create(WebArchive.class, APP_NAME);
        sciExceptionTestWar.addAsLibrary(sciExceptionTestJar);
        ShrinkHelper.exportDropinAppToServer(wcServer, sciExceptionTestWar);

        // Start the server and use the class name so we can find logs easily.
        wcServer.startServer(WCServletContainerInitializerExceptionTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (wcServer != null && wcServer.isStarted()) {
            // Allow the Warning message we have generated on purpose.
            wcServer.stopServer("CWWWC0001W");
        }
    }

    /**
     * Ensure the translation key is not output and that a valid message is.
     *
     * @throws Exception
     */
    @Test
    public void testExceptionThrowInSCIOnStartup() throws Exception {
        // An example of the expected warning message is:
        //
        // [WARNING ] CWWWC0001W: The [com.ibm.ws.servlet40.exception.sci.OnStartupExceptionSCI@c054b57] ServletContainerInitializer, located in the following application
        //      [SCIExceptionTest], produced the following error while invoking the onStartup method: [javax.servlet.ServletException: Test Exception thrown from
        //      OnStartupExceptionSCI.onStartup
        //              at com.ibm.ws.servlet40.exception.sci.OnStartupExceptionSCI.onStartup(OnStartupExceptionSCI.java:32)
        //              at com.ibm.ws.webcontainer.webapp.WebApp.initializeServletContainerInitializers(WebApp.java:2502)
        assertTrue("The expected messaged CWWWC0001W was not found in the logs.",
                   !wcServer.waitForStringInLog("CWWWC0001W.*").isEmpty());

    }
}
