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

import java.io.File;
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

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
// Allowing IOException and IllegalStateException because some streams may be interrupted when attempting to write
// causing the exception to show up on the test or on following tests
@AllowedFFDC({"java.io.IOException", "java.lang.IllegalStateException"})
public class Http2WindowSizeTests extends FATServletClient {

    private static final String CLASS_NAME = Http2WindowSizeTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.flowcontrol");

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
        // Allow SRVE0777E, SRVE8115W, SRVE8094W due to failed writes on streams because of connection limits
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
     * Test Coverage: Opening more than maxLowWindowStreams streams with initial window size
     * at or below lowWindowLimit causes the server to send a GOAWAY frame with error code ENHANCE_YOUR_CALM (0xb).
     * Test Outcome: GOAWAY received from server with ENHANCE_YOUR_CALM error code
     *
     * @throws Exception
     */
    @Test
    public void testTooManyLowWindowStreams() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Opening many streams with initial window size above lowWindowLimit
     * does NOT trigger the rate limiting.
     * Test Outcome: Connection remains open, no GOAWAY sent
     *
     * @throws Exception
     */
    @Test
    public void testManyHighWindowStreams() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Opening exactly maxLowWindowStreams streams with low initial window size
     * does NOT trigger the GOAWAY, verifying the boundary condition.
     * Test Outcome: Connection remains open, no GOAWAY sent
     *
     * @throws Exception
     */
    @Test
    public void testExactlyMaxLowWindowStreams() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Mixed scenario with some streams having low window size and some with high window size.
     * If streams have not yet finished, they should be counted toward check.
     * Test Outcome: Connection closes total low window streams <= limit
     *
     * @throws Exception
     */
    @Test
    public void testGoAwayMixedWindowSizes() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Mixed scenario with some streams having low window size and some with high window size.
     * Only the low window streams should be counted toward the limit.
     * Test Outcome: Connection remains open when total low window streams <= 100
     *
     * @throws Exception
     */
    @Test
    public void testStreamLimitDecreasesOnceDataIsWritten() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Opening above maxLowWindowStreams after changing from high to low window size.
     * Test Outcome: GOAWAY received from server with ENHANCE_YOUR_CALM error code
     *
     * @throws Exception
     */
    @Test
    public void testExceedLimitAfterWindowSizeDecrease() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Window size exactly at the threshold opening streams with should be counted as low window streams.
     * Test Outcome: GOAWAY received after maxLowWindowStreams
     *
     * @throws Exception
     */
    @Test
    public void testWindowSizeAtThreshold() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Window size just above the threshold should NOT be counted as low window streams.
     * Test Outcome: Connection remains open, no GOAWAY sent
     *
     * @throws Exception
     */
    @Test
    public void testWindowSizeJustAboveThreshold() throws Exception {
        runTest(Http2FullModeTests.lowWindowStreamPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Server tries to send a response larger than maxQueuedBytes
     * on a single stream with a tiny client window, causing queued data to exceed the limit.
     * Test Outcome: GOAWAY received from server with INTERNAL_ERROR (0x2)
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.io.IOException")
    public void testExceedMaxQueuedBytesOnSingleStream() throws Exception {
        runTest(Http2FullModeTests.queuedBytesPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Server tries to send responses larger than maxQueuedBytes
     * across multiple streams with tiny client windows, causing total queued data to exceed the limit.
     * Test Outcome: GOAWAY received from server with INTERNAL_ERROR (0x2)
     *
     * @throws Exception
     */
    @Test
    public void testExceedMaxQueuedBytesAcrossMultipleStreams() throws Exception {
        runTest(Http2FullModeTests.queuedBytesPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Server sends large response with normal client window size.
     * Data flows normally without excessive queuing.
     * Test Outcome: Connection remains open, no GOAWAY sent
     *
     * @throws Exception
     */
    @Test
    public void testNormalWindowSizeDoesNotTriggerLimit() throws Exception {
        runTest(Http2FullModeTests.queuedBytesPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Server response just under maxQueuedBytes with tiny client window.
     * Test Outcome: Connection remains open, no GOAWAY sent
     *
     * @throws Exception
     */
    @Test
    public void testQueuedBytesJustBelowLimit() throws Exception {
        runTest(Http2FullModeTests.queuedBytesPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Server response exactly at maxQueuedBytes with tiny client window.
     * Test Outcome: Connection remains open, no GOAWAY sent (limit is checked with > not >=)
     *
     * @throws Exception
     */
    @Test
    public void testExactlyAtMaxQueuedBytes() throws Exception {
        runTest(Http2FullModeTests.queuedBytesPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Server response on new stream with queued data under maxQueuedBytes
     * when write timeout occurs on previous stream with almost maxQueuedBytes.
     * Test Outcome: Connection closed when stream with data under queued bytes limit hits server
     * due to accumulated queued bytes from previous failed write stream
     *
     * @throws Exception
     */
    @Test
    public void testWriteTimeoutKeepsQueuedBytesTracked() throws Exception {
        runTest(Http2FullModeTests.queuedBytesPath, testName.getMethodName());
    }

}
