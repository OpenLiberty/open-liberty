/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.QUARANTINE)
public class Http2LiteModeTests extends FATServletClient {

    private static final String CLASS_NAME = Http2LiteModeTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");
    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat");

    String defaultServletPath = "H2FATDriver/H2FATDriverServlet?hostName=";
    String genericServletPath = "H2FATDriver/GenericFrameTests?hostName=";
    String continuationServletPath = "H2FATDriver/ContinuationFrameTests?hostName=";
    String dataServletPath = "H2FATDriver/DataFrameTests?hostName=";
    String methodServletPath = "H2FATDriver/HttpMethodTests?hostName=";
    String pushPromisePath = "H2FATDriver/PushPromiseTests?hostName=";

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void before() throws Exception {

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "before()", "Starting servers...");
        }

        H2FATApplicationHelper.addWarToServerDropins(server, "H2TestModule.war", false, "http2.test.war.servlets");
        H2FATApplicationHelper.addWarToServerDropins(runtimeServer, "H2FATDriver.war", false, "http2.test.driver.war.servlets");

        server.startServer(true, true);
        runtimeServer.startServer(true, true);
    }

    @AfterClass
    public static void after() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "after()", "Stopping servers......");
        }
        server.stopServer(true);
        runtimeServer.stopServer(true);
    }

    private void runTest() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + testName + " on server " + server.getServerName());
        }
    }

    private void runTest(String servletPath, String testName) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + servletPath + " on server " + server.getServerName());
        }
        FATServletClient.runTest(runtimeServer,
                                 servletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    public void runStressTest() throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test with iterations of: " + Utils.STRESS_ITERATIONS);
        }

        FATServletClient.runTest(runtimeServer,
                                 "H2FATDriver/H2FATDriverServlet?hostName=" + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&iterations="
                                                + Utils.STRESS_ITERATIONS + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    @Test
    public void testUpgradeHeaderFollowedBySettingsFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A client sends an HTTP request on a new stream, using a previously
     * unused stream identifier (Section 5.1.1). A server sends an HTTP
     * response on the same stream as the request.
     *
     * @throws Exception
     */
    @Test
    public void testSendGetRequest() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A client sends an HTTP request on a new stream, using a previously
     * unused stream identifier (Section 5.1.1). A server sends an HTTP
     * response on the same stream as the request.
     *
     * @throws Exception
     */
    @Test
    public void testSendPostRequest() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A client sends an HTTP request on a new stream, using a previously
     * unused stream identifier (Section 5.1.1). A server sends an HTTP
     * response on the same stream as the request.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadRequest() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testHeaderAndData() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testHeaderAndDataPost() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testSecondRequest() throws Exception {
        runTest();
    }

    @Test
    public void testPriorityWindowUpdate1() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testSmallWindowSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    // test does not pass, test needs to be re-worked @Test
    public void testRstStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testPing1() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a HEADER frame after connection preface
     *
     * The HEADERS frame (type=0x1) is used to open a stream (Section 5.1),
     * and additionally carries a header block fragment.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending PING frame and making sure we get an PING ACK with the same payload.
     *
     * Receivers of a PING frame that does not include an ACK flag MUST send
     * a PING frame with the ACK flag set in response, with an identical
     * payload.
     *
     * @throws Exception
     */
    @Test
    public void testPingFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testHeaderAndContinuations() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testUnknownFrameType() throws Exception {
        runTest(genericServletPath, testName.getMethodName());
    }

    /**
     * Streams
     * initiated by a client MUST use odd-numbered stream identifiers; those
     * initiated by the server MUST use even-numbered stream identifiers.
     *
     * @throws Exception
     */
    @Test
    public void testInvalidStreamId() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testDataOnStreamZero() throws Exception {
        runTest(genericServletPath, testName.getMethodName());
    }

    @Test
    public void testInvalidStreamIdSequence() throws Exception {
        runTest(genericServletPath, testName.getMethodName());
    }

    @Test
    public void testInterleavedHeaderBlocks() throws Exception {
        runTest(genericServletPath, testName.getMethodName());
    }

    @Test
    public void testContFrameAfterHeaderEndHeadersSet() throws Exception {
        runTest(continuationServletPath, testName.getMethodName());
    }

    @Test
    public void testContFrameAfterContEndHeadersSet() throws Exception {
        runTest(continuationServletPath, testName.getMethodName());
    }

    @Test
    public void testContFrameAfterDataSent() throws Exception {
        runTest(continuationServletPath, testName.getMethodName());
    }

    @Test
    public void testDataOnIdleStream() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    @Test
    public void testZeroLengthPadding() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    @Test
    public void testInvalidPaddingValue() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    @Test
    public void testDataFrameExceedingMaxFrameSize() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    @Test
    public void testConnectMethod() throws Exception {
        runTest(methodServletPath, testName.getMethodName());
    }

    @Test
    public void testConnectMethodError() throws Exception {
        runTest(methodServletPath, testName.getMethodName());
    }

    @Test
    public void testHeadMethod() throws Exception {
        runTest(methodServletPath, testName.getMethodName());
    }

    @Test
    public void testOptionMethod() throws Exception {
        runTest(methodServletPath, testName.getMethodName());
    }

    @Test
    public void testOptionMethod400Uri() throws Exception {
        runTest(methodServletPath, testName.getMethodName());
    }

    @Test
    public void testOptionMethod404Uri() throws Exception {
        runTest(methodServletPath, testName.getMethodName());
    }

    @Test
    public void testPushPromisePreload() throws Exception {
        runTest(pushPromisePath, testName.getMethodName());
    }

    @Test
    public void testPushPromisePushBuilder() throws Exception {
        runTest(pushPromisePath, testName.getMethodName());
    }

    @Test
    public void testClientSendPushPromiseError() throws Exception {
        runTest(pushPromisePath, testName.getMethodName());
    }

    @Test
    public void testPushPromiseClientNotEnabledPreload() throws Exception {
        runTest(pushPromisePath, testName.getMethodName());
    }

    @Test
    public void testPushPromiseClientNotEnabledPushBuilder() throws Exception {
        runTest(pushPromisePath, testName.getMethodName());
    }

    /**
     * When the
     * value of SETTINGS_INITIAL_WINDOW_SIZE changes, a receiver MUST adjust
     * the size of all stream flow-control windows that it maintains by the
     * difference between the new value and the old value.
     *
     * FIXME: The server is not adjusting the size of the DATA frame after the new SETTINGS_INITIAL_WINDOW_SIZE changed from 0 to 1.
     * This test will be incomplete when the problem is fixed as I don't know how the DATA frame will look like.
     * WTL: should we sent out partial data frames like this?
     *
     * @throws Exception
     */
    @Test
    public void testModifiedInitialWindowSizeAfterHeaderFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }
}
