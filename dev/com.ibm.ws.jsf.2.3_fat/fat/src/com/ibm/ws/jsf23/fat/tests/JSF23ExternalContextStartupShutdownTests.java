/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
 * A set of tests to test the ExternalContext during startup and shutdown.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23ExternalContextStartupShutdownTests {
    protected static final Class<?> c = JSF23ExternalContextStartupShutdownTests.class;

    private static final String appName = "StartupShutdownExternalContext.war";

    @Rule
    public TestName name = new TestName();

    @Server("jsf23ExternalContextStartupShutdownServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        server.startServer(JSF23ExternalContextStartupShutdownTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * A test to ensure that the getRealPath method can be invoked on the ExternalContext
     * during startup and shutdown. The getRealPath method is called from a
     * PostConstructApplicationEvent and PreDestroyApplicationEvent listener.
     *
     * @throws Exception
     */
    @Test
    public void testExternalContextGetRealPath_Startup_Shutdown() throws Exception {
        String msgToSearchForStartup = ".*JSF23: PostConstructApplicationEvent getRealPath test:.*";
        String msgToSearchForShutdown = ".*JSF23: PreDestroyApplicationEvent getRealPath test:.*";

        // Set the mark to the end of the logs, install the application so the
        // PostConstructApplicationEvent is published.
        server.setMarkToEndOfLog();

        ShrinkHelper.defaultDropinApp(server, "StartupShutdownExternalContext.war", "com.ibm.ws.jsf23.fat.systemevent.listener");

        // Ensure the application was installed successfully.
        assertNotNull("The application " + appName + " did not appear to have been installed.",
                      server.waitForStringInLog("CWWKZ0001I.* " + appName.substring(0, appName.indexOf("."))));

        // Search the logs to see if the PostConstructApplicationEventListener was invoked.
        String startupMsg = server.waitForStringInLog(msgToSearchForStartup);

        // Ensure the message was actually found in the logs.
        assertNotNull("The following message was not found: " + msgToSearchForStartup, startupMsg);

        // Ensure that the output of getRealPath contains the actual file name argument.
        assertTrue("The ExternalContext.getRealPath() method did not work during startup.", startupMsg.contains("index"));

        server.setMarkToEndOfLog();

        // Stop the server to publish the PreDestroyApplicationEvent. We need
        // to look at the logs after the server is stopped so we don't want to archive them.
        server.stopServer(false);

        // Search the logs to see if the PreDestoryApplicationEventListener was invoked.
        String shutdownMsg = server.waitForStringInLog(msgToSearchForShutdown);

        // Ensure the message was actually found in the logs.
        assertNotNull("The following message was not found: " + msgToSearchForShutdown, shutdownMsg);

        // Ensure that the output of getRealPath contains the actual file name argument.
        assertTrue("The ExternalContext.getRealPath() method did not work during shutdown.", shutdownMsg.contains("index"));

        server.postStopServerArchive();
    }
}
