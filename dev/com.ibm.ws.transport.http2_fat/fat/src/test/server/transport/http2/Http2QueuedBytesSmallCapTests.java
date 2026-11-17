/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package test.server.transport.http2;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

/**
 * maxQueuedBytes tests that need a small cap. They run against the "flowcontrol.smallcap" server
 * (maxQueuedBytes=262144); Http2WindowSizeTests uses the 2 MB default and must not be changed.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
@AllowedFFDC({"java.io.IOException", "java.lang.IllegalStateException"})
public class Http2QueuedBytesSmallCapTests extends FATServletClient {

    private static final String CLASS_NAME = Http2QueuedBytesSmallCapTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String queuedBytesSmallCapPath = "H2FATDriver/QueuedBytesSmallCapTests?hostName=";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.flowcontrol.smallcap");

    @Rule
    public TestName testName = new Utils.CustomTestName();

    @BeforeClass
    public static void before() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", true, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", true, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true, true);
        H2FATApplicationHelper.preTestNettyCheck(runtimeServer, server);
    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        server.stopServer("SRVE0777E", "SRVE8115W", "SRVE8094W");
        runtimeServer.stopServer(true);
    }

    private void runTest(String servletPath, String testName) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + servletPath + " on server " + server.getServerName());
        }
        FATServletClient.runTest(runtimeServer,
                                 servletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    /**
     * Test Coverage: 2500 KB response (about 10x the 256 KB cap) on an open window.
     * Test Outcome: 200 and all DATA arrive; no GOAWAY(ENHANCE_YOUR_CALM). Nothing is blocked by flow control,
     * so nothing is parked and the cap must not be hit.
     *
     * @throws Exception
     */
    @Test
    public void testLargeResponseOnOpenWindowDoesNotTriggerSmallCap() throws Exception {
        runTest(queuedBytesSmallCapPath, testName.getMethodName());
    }



}
