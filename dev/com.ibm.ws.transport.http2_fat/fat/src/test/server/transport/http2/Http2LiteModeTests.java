/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
@Mode(TestMode.LITE)
public class Http2LiteModeTests extends FATServletClient {

    private static final String CLASS_NAME = Http2LiteModeTests.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");
    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat");

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

    @Test
    public void testUpgradeHeaderFollowedBySettingsFrame() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Client sends an HTTP GET request on a new stream.
     * Test Outcome: Server sends an HTTP response on the same stream as the request.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testSendGetRequest() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Client sends an HTTP POST request on a new stream.
     * Test Outcome: Server sends an HTTP response on the same stream as the request.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testSendPostRequest() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Both Client and Server can send Header frames.
     * Test Outcome: Frames transfer with no error.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadRequest() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Test Server can send Data frames.
     * Test Outcome: Frames transfer with no error.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testHeaderAndData() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Client send an HTTP/2 Post Request. Test that Server can respond with Header and Data frames.
     * Test Outcome: Frames transfer with no error.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    // Currently in Http2FullTracingTests
    //@Test
    //public void testHeaderAndDataPost() throws Exception {
    //    runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Client sends two HTTP/2 Requests on two HTTP/2 Streams.
     * Test that Server can respond on each stream.
     * Test Outcome: Frames transfer with no error.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testSecondRequest() throws Exception {
        runTest();
    }

    /**
     * Test Coverage: Using one stream have the client send update and priority frames to the server
     * Test that Server can receive update and priority frames successfully.
     * Test Outcome: Frames transfer with no error.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testPriorityWindowUpdate1() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Set the window size to be very small; make sure server waits to send over new frames
     * until a window_update is sent.
     * Test Outcome: stream with small windows waits for WINDOW_UPDATE frame before sending more frames.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testSmallWindowSize() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

//    // test does not pass, test needs to be re-worked @Test
//    public void testRstStream() throws Exception {
//        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
//    }

    /**
     * Test Coverage: Send Pings from the client to the server. Server will send back Ping responses.
     * Test Outcome: Test success is if pings do not prevent the server from
     * performing other http2 traffic correctly.
     * Spec Section: Basic Functionality
     *
     * @throws Exception
     */
    @Test
    public void testPing1() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a HEADER frame on stream 3 after upgrading a connection and sending the connection preface
     * Test Outcome: Correct Frames transfer with no error.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrame() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending PING frame and making sure we get an PING ACK with the same payload.
     * Test Outcome: Receivers of a PING frame that does not include an ACK flag MUST send
     * a PING frame with the ACK flag set in response, with an identical
     * payload.
     * Spec Section: 6.7
     *
     * @throws Exception
     */
    @Test
    public void testPingFrame() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Set the max frame size to be less than the expected header responds.
     * Test Outcome: make sure that a correct continuation frame is received.
     * Spec Section: 4.2, 6.10
     *
     * @throws Exception
     */
    @Test
    public void testHeaderAndContinuations() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a frame with an invalid type, followed by a PING frame;
     * Test Outcome: expect only a PING from the server. The rfc (4.1) states that the server should ignore any unknown types.
     * Spec Section: 4.1
     *
     * @throws Exception
     *
     */
    //@Test
    //public void testUnknownFrameType() throws Exception {
    //    runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Start a stream from the client with an even numbered stream id.
     * Test Outcome: PROTOCOL ERROR - client can only start odd numbered streams.
     * Spec Section: 5.1.1
     *
     * @throws Exception
     */
    @Test
    public void testInvalidStreamId() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame on stream 0.
     * Test Outcome: PROTOCOL ERROR - Stream 0 is the control stream;
     * DATA, HEADERS, CONTINUATION, and PUSH_PROMISE are not allowed on it
     * Spec Section: 5.1.1
     *
     * @throws Exception
     */
    // Moved to trace, build break 259034
    //@Test
    //public void testDataOnStreamZero() throws Exception {
    //    runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Client starts a stream 7, then starts a stream 5, which is illegal;
     * Test Outcome: the server should emit a GOAWAY with the error code PROTOCOL_ERROR
     * Spec Section: 5.1.1
     *
     * @throws Exception
     */
    //@Test Move to trace bucket
    public void testInvalidStreamIdSequence() throws Exception {
        runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Header block fragments must be sent contiguously: headers and corresponding continuation frames
     * cannot be interleaved with different frames types, or even with different frame types on different streams.
     * This test will:
     * 1. send a header stream 3 without the end of header flag set, which implies that a continuation must follow
     * 2. send a header on stream 5
     * Test Outcome: expect a PROTOCOL ERROR from the server
     * Spec Section: 4.3
     *
     * @throws Exception
     */
    @Test
    public void testInterleavedHeaderBlocks() throws Exception {
        runTest(Http2FullModeTests.genericServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a continuation frame on a stream after the end headers flag has already been sent on a headers frame.
     * Test Outcome: Server should respond with a PROTOCOL ERROR
     * Spec Section: 6.2
     *
     * @throws Exception
     */
    @Test
    public void testContFrameAfterHeaderEndHeadersSet() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a continuation frame on a stream after the end headers flag has already been sent on a previous continuation frame.
     * Test Outcome: Server should respond with a PROTOCOL ERROR
     * Spec Section: 6.10
     *
     * @throws Exception
     */
    @Test
    public void testContFrameAfterContEndHeadersSet() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a continuation frame on a stream after the end headers flag AND a data frame have been sent.
     * Test Outcome: Expect a protocol error from the server.
     * Spec Section: 6.10
     *
     * @throws Exception
     */
    @Test
    public void testContFrameAfterDataSent() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and send out a header frame that exceeds established size.
     * Start H2 connection
     * Send large headers that exceed limit
     * Test Outcome: The HTTP/2 stream should receive a go away after exceeding the maximum header size.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderLimitReached() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and send out a continuation frame that exceeds established size.
     * Start H2 connection
     * Send headers with end of headers flag unset
     * Then send a continuation frame that exceeds limits
     * Test Outcome: The HTTP/2 stream should receive a go away after exceeding the maximum header size.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderContinuationLimitReached() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and send out a header frame that exceeds token limit size.
     * Start H2 connection
     * Send headers with extra long value
     * Test Outcome: The HTTP/2 stream should receive a go away after exceeding the limit token size.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderTokenSizeExceeded() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and send out a header frame that exceeds header size limit.
     * Start H2 connection
     * Send a lot of headers for a stream
     * Test Outcome: The HTTP/2 stream should receive a go away after exceeding the header size limit.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderSizeExceeded() throws Exception {
        runTest(Http2FullModeTests.continuationServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame on a stream that's in IDLE state.
     * Test Outcome: Expect a STREAM_CLOSED error in response.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testDataOnIdleStream() throws Exception {
        runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame with the padding field set to 0.
     * Test Outcome: Expect a normal response.
     * Spec Section: 6.1
     *
     * @throws Exception
     */
    // Moved to trace
    //@Test
    //public void testZeroLengthPadding() throws Exception {
    //    runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send a DATA frame with a frame length of 5 and a padding length of 6.
     * Test Outcome: GOAWAY. connection error
     * Spec Section: 6.1
     *
     * @throws Exception
     */

    // Move to trace bucket to debug build break @Test
    //public void testInvalidPaddingValue() throws Exception {
    //    runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    //}

    // Move to trace bucket to debug build break @Test
    //public void testDataFrameExceedingMaxFrameSize() throws Exception {
    //    runTest(Http2FullModeTests.dataServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send a header with CONNECT set
     * Test Outcome: connection should work ok
     * Spec Section: 8.3
     *
     * @throws Exception
     */
    //@Test moved to trace
    public void testConnectMethod() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a header with CONNECT set along with a disallowed pseudo-header.
     * Then immediately send some DATA frames.
     * Test Outcome: Expect an RST_STREAM on the erroneous stream,
     * and no GOAWAY despite sending DATA frames after the RST_STREAM is received
     * Spec Section: 8.3, 8.1.2.3
     *
     * @throws Exception
     */
    //@Test moved to trace
    public void testConnectMethodError() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a header with the HEAD method set.
     * Test Outcome: Expect back one header with a content-length set, and no body
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testHeadMethod() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a header with the Option method set and a valid servlet path set for :path
     * Test Outcome: connection should work ok
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testOptionMethod() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a header with the Option method set and an invalid path.
     * Test Outcome: Expect an RST_STREAM in response.
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testOptionMethod400Uri() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a header with the Option method set and an invalid path.
     * Test Outcome: Expect a 404 response in return.
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testOptionMethod404Uri() throws Exception {
        runTest(Http2FullModeTests.methodServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Test the header link rel=preload path, server side serlvet will send a Response
     * with two Link headers, rel=preload.
     * Test Outcome: the two Link headers will be converted into two successful PUSH_PROMISE frames and streams.
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testPushPromisePreload() throws Exception {
        runTest(Http2FullModeTests.pushPromisePath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use the Servlet 4 Push Builder API to create and send a PUSH_PROMISE frame
     * Test Outcome: New stream should result from the PUSH_PROMISE frame and complete successfully
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testPushPromisePushBuilder() throws Exception {
        runTest(Http2FullModeTests.pushPromisePath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a Push Promise frame from the client on stream 1
     * Test Outcome: Respond with a connection error, PROTOCOL_ERROR, and close the connection.
     * Spec Section: 8.2
     *
     * @throws Exception
     */
    @Test
    public void testClientSendPushPromiseError() throws Exception {
        runTest(Http2FullModeTests.pushPromisePath, testName.getMethodName());
    }

    /**
     * Test Coverage: Disable Push Promise in SETTINGS frame, then send a Link preload header in a response
     * Test Outcome: No Push Promise frame should be generated.
     * Spec Section: 6.6
     *
     * @throws Exception
     */
    @Test
    public void testPushPromiseClientNotEnabledPreload() throws Exception {
        runTest(Http2FullModeTests.pushPromisePath, testName.getMethodName());
    }

    /**
     *
     * Test Coverage: Disable Push Promise in SETTINGS frame, then have the servlet use the PushBuilder API to attempt
     * a Push Promise session
     * Test Outcome: No Push Promise frame should be generated.
     * Spec Section: 6.6
     *
     * @throws Exception
     */
    @Test
    public void testPushPromiseClientNotEnabledPushBuilder() throws Exception {
        runTest(Http2FullModeTests.pushPromisePath, testName.getMethodName());
    }

    /**
     * Test Coverage: In first Settings frame set the INITIAL_WINDOW_SIZE to 1.
     * Send Headers
     * Use a new Settings frame to set the INITIAL_WINDOW_SIZE to a larger size
     * verify DATA frame then comes back from the server
     * Test Outcome: DATA frame arrives when expected
     * Spec Section: 6.9.2
     *
     * @throws Exception
     */
    @Test
    public void testModifiedInitialWindowSizeAfterHeaderFrame() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and immediately send the HTTP/2 magic string.
     * The HTTP/2 connection preface should complete.
     * Then send a standard HTTP/2 request
     * Test Outcome: HTTP/2 response arrives as expected.
     * Spec Section: 3.4
     *
     * @throws Exception
     */
    @Test
    public void testHeaderAndDataPriorKnowledge() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Connect to server via the insecure port and immediately send the HTTP/2 magic string.
     * The HTTP/2 connection preface should complete.
     * Then send a standard HTTP/2 POST request and body
     * Test Outcome: HTTP/2 response arrives as expected.
     * Spec Section: 3.4
     *
     * @throws Exception
     */
    @Test
    public void testPostRequestDataKnowledge() throws Exception {
        runTest(Http2FullModeTests.defaultServletPath, testName.getMethodName());
    }

}
