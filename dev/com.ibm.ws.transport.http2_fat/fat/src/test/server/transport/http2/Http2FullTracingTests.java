/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.server.transport.http2;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Http2FullTracingTests extends FATServletClient {

    private static final String CLASS_NAME = Http2FullTracingTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime.tracing");
    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.tracing");

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
        // Go through Logs and check if Netty is being used
        boolean runningNetty = false;
        // Wait for endpoints to finish loading and get the endpoint started messages
        server.waitForStringInLog("CWWKO0219I.*");
        runtimeServer.waitForStringInLog("CWWKO0219I.*");
        List<String> test = server.findStringsInLogs("CWWKO0219I.*");
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Got port list...... " + Arrays.toString(test.toArray()));
            LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Looking for port: " + server.getHttpSecondaryPort());
        }
        for (String endpoint : test) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Endpoint: " + endpoint);
            }
            if (!endpoint.contains("port " + Integer.toString(server.getHttpSecondaryPort())))
                continue;
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "test()", "Netty? " + endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils"));
            }
            runningNetty = endpoint.contains("io.openliberty.netty.internal.tcp.TCPUtils");
            break;
        }
        if (runningNetty)
            FATServletClient.runTest(runtimeServer,
                                     Http2FullModeTests.defaultServletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                     "setUsingNetty");
    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        // try for an orderly quiet shutdown
        Thread.sleep(5000);
        runtimeServer.stopServer(true);
        Thread.sleep(5000);
        server.stopServer(true);
    }

    private void runTest(String servletPath, String testName) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + servletPath + " on server " + server.getServerName());
        }
        FATServletClient.runTest(runtimeServer,
                                 servletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    @Test
    public void testHeaderAndDataPost() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    // moved for debug
    @Test
    public void testDataFrameExceedingMaxFrameSize() throws Exception {
        runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame of the wrong size (other than a multiple of 6 octets)
     * Test Outcome: Return a connection error of type FRAME_SIZE_ERROR and close the connection.
     * Spec Section: 6.5
     *
     * @throws Exception
     */
    // moved for debug
    @Test
    public void testSettingFrameBadSize() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    // moved for debug
    @Test
    public void testZeroLengthPadding() throws Exception {
        runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    }

    // Moved to trace
    @Test
    public void testDataOnStreamZero() throws Exception {
        runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    }

    // Moved to tracing
    @Test
    public void testHeaderFrameAfterHeaderFrameWithEndOfStream() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    // Moved to tracing
    @Test
    public void testDataFrameAfterContinuationFrame() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    // Moved to tracing
    @Test
    public void testUnknownFrameType() throws Exception {
        runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    }

    @Test
    public void testContinuationFrameAfterDataFrame() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testInvalidPaddingValue() throws Exception {
        runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    }

    @Test
    public void testInvalidStreamIdSequence() throws Exception {
        runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    }

    @Test
    public void testConnectMethod() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    @Test
    public void testConnectMethodError() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    @Test
    public void testPriorityFrameAfterHeaderFrameNoEndHeaders() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

}
