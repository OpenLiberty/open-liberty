/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
public class Http2FullModeTests extends FATServletClient {

    // to run this bucket with trace on flip the comments here (or figure out something I couldn't about the tracing!)
    private static final String CLASS_NAME = Http2FullModeTests.class.getName();
    private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat");
    // private static final String CLASS_NAME = Http2FullTracingTests.class.getName();
    // private final static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.transport.http2.fat.tracing");

    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static final String TEST_DIR = System.getProperty("dir.build.classes") + File.separator + "test" + File.separator + "server" + File.separator + "transport"
                                          + File.separator + "http2" + File.separator + "buckets";

    private final static LibertyServer runtimeServer = LibertyServerFactory.getLibertyServer("http2ClientRuntime");

    public static final String defaultServletPath = "H2FATDriver/H2FATDriverServlet?hostName=";
    public static final String genericServletPath = "H2FATDriver/GenericFrameTests?hostName=";
    public static final String continuationServletPath = "H2FATDriver/ContinuationFrameTests?hostName=";
    public static final String dataServletPath = "H2FATDriver/DataFrameTests?hostName=";
    public static final String methodServletPath = "H2FATDriver/HttpMethodTests?hostName=";
    public static final String pushPromisePath = "H2FATDriver/PushPromiseTests?hostName=";

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
                                     defaultServletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
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

    /**
     * Test Coverage: Client Sends Upgrade Header followed by a SETTINGS frame
     * Test Outcome: Connection should work ok
     * Spec Section: 6.5
     *
     * @throws Exception
     */
    @Test
    public void testUpgradeHeaderFollowedBySettingsFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a Priority frame on an idle stream. All streams start in idle state.
     * Test Outcome: Connections and streams should complete successfully.
     * Spec Section: 5.1
     *
     * Test Notes:
     * Receiving any frame other than HEADERS or PRIORITY on a stream in this state
     * MUST be treated as a connection error of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a Window_Update frame on an half-closed stream.
     * Test Outcome: Respond with a connection error, PROTOCOL_ERROR, and close the connection
     * Spec Section: 5.1
     *
     * Test Notes:
     * A stream that is "half-closed (remote)" is no longer being used by
     * the peer to send frames. In this state, an endpoint is no longer
     * obligated to maintain a receiver flow-control window.
     *
     * @throws Exception
     */
    @Test
    public void testWindowsUpdateFrameOnHalfClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a RST_STREAM frame on an half-closed stream.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 5.1
     *
     */
    @Test
    public void testRstStreamFrameOnHalfClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a priority frame after a stream has finished.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 5.1
     *
     * Test Notes:
     * An endpoint MUST NOT send frames other than PRIORITY on a closed stream.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a Continuation frame after a Headers frame.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.10
     *
     * Test Notes:
     * Any number of CONTINUATION frames can be sent, as long as the preceding frame is on the same
     * stream and is a HEADERS, PUSH_PROMISE, or CONTINUATION frame without the END_HEADERS flag set.
     *
     * @throws Exception
     *
     */
    @Test
    public void testContFrameAfterHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage:
     * Test Outcome:
     * Spec Section:
     *
     * @throws Exception
     *
     */
    @Test
    public void testTwoContFrameAfterHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Test Coverage: Sending HEADER with 8 bytes padding and 8 bytes data block.
     * Test Outcome: The HEADERS frame can include padding, so connection should work ok.
     * Spec Section: 6.2, 6.1
     *
     * @throws Exception
     */
    @Test
    public void testSendHeaderWithPaddingFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending HEADERS frame with priority set.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.2
     *
     *
     * @throws Exception
     */
    @Test
    public void testSendHeaderFrameWithPriorityValue() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a PRIORITY frame on stream 3 before sending anything else.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.3
     *
     * Test Notes:
     * The PRIORITY frame (type=0x2) specifies the sender-advised priority
     * of a stream. It can be sent in any stream state, including idle or closed streams.
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithPriorityOne() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a PRIORITY frame with the max weight.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.3
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithPriority256() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a PRIORITY frame on stream 3 with a stream dependency on stream 5.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.3, 5.3
     *
     * Test Notes:
     * Stream Dependency: A 31-bit stream identifier for the stream that
     * this stream depends on. This field is only present if the PRIORITY flag is set.
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithStreamDependency() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending PRIORITY frame with exclusive flag on.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.3
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithExclusiveFlag() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Test Coverage: Sending PRIORITY on a higher stream (5) and then send a HEADER on stream (3); in this order.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 5.3.3
     *
     * Test Notes:
     * The PRIORITY frame can be sent for a stream in the "idle" or "closed" state. This allows for the
     * reprioritization of a group of dependent streams by altering the priority of an unused or closed parent stream.
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameOnHigherStreamIdThanHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending RST_STREAM to cancel stream id 3 after sending a HEADER frame with end of stream false.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.4
     *
     * Test Notes:
     * The RST_STREAM frame (type=0x3) allows for immediate termination of a stream. RST_STREAM is sent to
     * request cancellation of a stream or to indicate that an error condition has occurred.
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameToCancelStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a SETTING frame with various values for the input parameters
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.5.2
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithValues() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending WindowUpdate on stream 0 to affect all streams.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.9
     *
     * Test Notes:
     * Flow control operates at two levels: on each individual stream and on the entire connection.
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending WindowUpdate on stream 3 to affect this stream only.
     * Test Outcome: Flow control operates at two levels: on each individual stream and on the entire connection.
     * Therefore only stream 3 flow control should be affected.
     * Spec Section: 6.9
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

//    /**
//     * WTL: the server is responding to this request with a 404. Liberty doesn't support the '*' on http 1.1, so for now
//     * we can ignore this case.
//     *
//     * @throws Exception
//     */
//    //@Test
//    public void testSendOptionsRequest() throws Exception {
//        runTest(defaultServletPath, testName.getMethodName());
//    }

    /**
     * Test Coverage: use OPTIONS header with path URI that is different for the upgrade header
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 3.2
     *
     * @throws Exception
     */
    @Test
    public void testSendOptionsRequestUrlPath() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use and Add non-Huffman encoded Index Header,
     * header value is empty string, header key is in static table
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testIndexedHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use and Add non-Huffman encoded Index Header
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testIndexedHeaderFieldNoHuffman() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use and Add Huffman encoded Index Header
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testIndexedHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use and Add non-Huffman encoded Custom Header
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testIndexedCustomHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use and Add Huffman encoded Custom Header
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testIndexedCustomHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a non-Huffman encoded header that is not indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNoIndexHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a Huffman encoded header that is not indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNoIndexHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a non-Huffman encoded custom header that is not indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNoIndexCustomHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a Huffman encoded custom header that is not indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNoIndexCustomHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a non-Huffman encoded header that is never indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNeverIndexHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a Huffman encoded header that is never indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNeverIndexHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a non-Huffman encoded custom header that is never indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNeverIndexCustomHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Use a Huffman encoded custom header that is never indexed
     * Test Outcome: Process all Frames as valid.
     * Spec Section: n/a
     *
     * @throws Exception
     */
    @Test
    public void testNeverIndexCustomHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Protocols that use HPACK determine the maximum size that the encoder
     * is permitted to use for the dynamic table. In HTTP/2, this value is determined by the
     * SETTINGS_HEADER_TABLE_SIZE setting. Change this size using the SETTINGS frame from the client.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.5.2.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicTableSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send Multiple updates for the maximum table size between the transmission of two header blocks.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.5, 6.5.2.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicTableSizeChanged() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a frame with an invalid type, followed by a PING frame;
     * Test Outcome: expect only a PING from the server. Server should ignore any unknown types.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testUnknownFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a Ping that has an undefined flag bit set
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 4.1
     *
     * Test Notes: Flags are assigned semantics specific to the indicated frame type.
     * Flags that have no defined semantics for a particular frame type
     * MUST be ignored and MUST be left unset (0x0) when sending.
     *
     * @throws Exception
     */

    @Test
    public void testPingFrameBadFlags() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage Send a Ping that has the reserve bit set
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 4.1
     *
     * Test Notes:
     * R: A reserved 1-bit field. The semantics of this bit are undefined,
     * and the bit MUST remain unset (0x0) when sending and MUST be
     * ignored when receiving.
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameReservedFlag() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

//    /**
//     * Need to be able to create a huge Data frame...
//     *
//     * @throws Exception
//     */
//    //@Test
//    public void testDataFrameMaxSize() throws Exception {
//        runTest(defaultServletPath, testName.getMethodName());
//    }

    /**
     * Test Coverage: A decoding error in a header block MUST be treated as a connection error of type COMPRESSION_ERROR.
     * Therefore send Header frame with an invalid Header Block Fragment.
     * Test Outcome: Connection close with GOAWAY containing Compression error
     * Spec Section: 4.3
     *
     * @throws Exception
     */
    @Test
    public void testInvalidHeaderBlock() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: All HTTP/2 requests MUST include exactly one valid value for the ":method", ":scheme",
     * and ":path" pseudo-header fields, unless it is a CONNECT request.
     * Therefore send a Header Frame without including :scheme or :path
     * Test Outcome: RST_STREAM with PROTOCOL_ERROR should be sent from the server
     * Spec Section: 8.1.2.3, 8.3
     *
     * @throws Exception
     */
    @Test
    public void testInvalidHeaderFields() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: idle: In an IDLE state send a DATA Frame.
     * Test Outcome: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * idle state MUST be treated as a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: idle: In an IDLE state send a RST_STREAM Frame.
     * Test Outcome: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * idle state MUST be treated as a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: idle: In an IDLE state send a WINDOW_UPDATE Frame.
     * Test Outcome: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * idle state MUST be treated as a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: idle: In an IDLE state send a CONTINUATION Frame.
     * Test Outcome: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * idle state MUST be treated as a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: closed: in a CLOSED state send a DATA Frame
     * Test Outcome: An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error of type STREAM_CLOSED.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: closed: in a CLOSED state send a HEADER Frame
     * Test Outcome: An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error of type STREAM_CLOSED.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: closed: in a CLOSED state send a CONTINUATION Frame
     * Test Outcome: An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error of type STREAM_CLOSED.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: closed: send a DATA frame on a closed stream
     * Test Outcome: connection closed with a GOAWAY Frame.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameAfterHeaderFrameWithEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: closed: in a CLOSED state send a HEADER Frame
     * Test Outcome: An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error of type STREAM_CLOSED.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    // Moved to tracing, build break 258327
    //@Test
    //public void testHeaderFrameAfterHeaderFrameWithEndOfStream() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: send DATA frame before END_HEADERS has been sent.
     * Test Outcome: If the END_HEADERS bit is not set, this frame MUST be followed by
     * another CONTINUATION frame. A receiver MUST treat the receipt of
     * any other type of frame or a frame on a different stream as a
     * connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    // Moved to tracing, build break 268375
    // @Test
    // public void testDataFrameAfterContinuationFrame() throws Exception {
    //     runTest(defaultServletPath, testName.getMethodName());
    // }

    /**
     * Test Coverage: Send a CONTINUATION frame on stream 0
     * Test Outcome: If a CONTINUATION frame is received whose stream identifier field is 0x0,
     * the recipient MUST respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send CONTINUATION after setting End of Headers flag set on HEADER frame
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameAfterAnEndOfHeaders() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send CONTINUATION after setting End of Headers flag set on previous CONTINUATION frame
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws Exception
     */
    @Test
    public void testSecondContinuationFrameAfterAnEndOfHeaders() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send CONTINUATION after sending a DATA frame
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 5.1
     *
     * @throws
     */
    //@Test
    //public void testContinuationFrameAfterDataFrame() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send DATA frame on stream 0
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 6.1
     *
     * @throws
     */
    @Test
    public void testDataFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA Frame with a pad length greater than the payload length
     * Test Outcome: The total number of padding octets is determined by the value of the
     * Pad Length field. If the length of the padding is the length of the
     * frame payload or greater, the recipient MUST treat this as a connection error of type PROTOCOL_ERROR.
     * Spec Section: 6.1
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameBadPaddingLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADER frame without END_HEADERS flag set. Then send a PRIORITY frame, then
     * send CONTINUATION frame(s) with END_HEADERS flag set correctly.
     * Test Outcome: A HEADERS frame without the END_HEADERS flag set MUST be followed
     * by a CONTINUATION frame for the same stream. A receiver MUST treat the receipt of any other
     * type of frame or a frame on a different stream as a connection error of type PROTOCOL_ERROR.
     * Spec Section: 6.2
     *
     * @throws Exception
     */
    //@Test
    public void testPriorityFrameAfterHeaderFrameNoEndHeaders() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send HEADERS frame on stream 0
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 6.2
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame where the pad length is greater than the payload length
     * Test Outcome: HEADERS Padding fields and flags are identical to those defined for DATA frames. Padding
     * that exceeds the size remaining for the header block fragment MUST be treated as a PROTOCOL_ERROR.
     * Spec Section: 6.2, 6.1
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameBadPaddingLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send PRIORITY frame on stream 0
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 6.3
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send PRIORITY frame with a length other than 5 octets
     * Test Outcome: A PRIORITY frame with a length other than 5 octets MUST be treated as
     * a stream error of type FRAME_SIZE_ERROR.
     * Spec Section: 6.3
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameLength4() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sending a priority frame on an idle stream
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 6.3
     *
     * Test Notes:
     * An endpoint MUST NOT send frames other than PRIORITY on a closed stream.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameOnIdlePushStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send RST_FRAME frame on stream 0
     * Test Outcome: respond with a connection error of type PROTOCOL_ERROR.
     * Spec Section: 6.4
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a RST_STREAM with a 3 byte payload
     * Test Outcome: RST_STREAM payload must be 4 bytes, therefore truea this as a connection error
     * Spec Section: 6.4
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameLength3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame with a SETTINGS_INITIAL_WINDOW_SIZE that is above the max size
     * Test Outcome: return a connection error of type FLOW_CONTROL_ERROR
     * Spec Section: 6.5
     *
     * Test Notes:
     * Indicates the sender's initial. window size (in octets) for stream-level flow control.
     * The initial value is 2^16-1 (65,535) octets.
     *
     * This setting affects the window size of all streams (see Section 6.9.2).
     *
     * Values above the maximum flow-control window size of 2^31-1 MUST
     * be treated as a connection error (Section 5.4.1) of type
     * FLOW_CONTROL_ERROR.
     *
     * @throws Exception
     *
     */
    @Test
    public void testSettingFrameWithLessThanMinimunFrameSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame with a value for MAX_FRAME_SIZE that is above the maximum
     * Test Outcome: return a connection error of type PROTOCOL_ERROR
     * Spec Section: 6.5
     *
     * Test Notes:
     * The initial value is 2^14 (16,384) octets. The value advertised
     * by an endpoint MUST be between this initial value and the maximum
     * allowed frame size (2^24-1 or 16,777,215 octets), inclusive.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithInvalidFrameSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    // Current the same as testSettingFrameWithLessThanMinimunFrameSize, so needs to be re-worked
//    /**
//     *
//     * Indicates the sender's initial
//     * window size (in octets) for stream-level flow control. The
//     * initial value is 2^16-1 (65,535) octets.
//     *
//     * This setting affects the window size of all streams (see
//     * Section 6.9.2).
//     *
//     * Values above the maximum flow-control window size of 2^31-1 MUST
//     * be treated as a connection error (Section 5.4.1) of type
//     * FLOW_CONTROL_ERROR.
//     *
//     * @throws Exception
//     */
//    @Test
//    public void testSettingFrameWithInvalidMaxWindowSize() throws Exception {
//        runTest(defaultServletPath, testName.getMethodName());
//    }

    /**
     * Test Coverage: Send a SETTINGS frame with an invalid identifier.
     * Test Outcome: An endpoint that receives a SETTINGS frame with any unknown or
     * unsupported identifier MUST ignore that setting. Connection should work ok
     * Spec Section: 6.5
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithUnkownIdentifier() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame with ACK set and a payload
     * Test Outcome: Return a connection error of type FRAME_SIZE_ERROR and close the connection.
     * Spec Section: 6.5.3
     *
     * Test Notes:
     * ACK (0x1): When set, bit 0 indicates that this frame acknowledges
     * receipt and application of the peer's SETTINGS frame. When this
     * bit is set, the payload of the SETTINGS frame MUST be empty.
     * Receipt of a SETTINGS frame with the ACK flag set and a length
     * field value other than 0 MUST be treated as a connection error
     * of type FRAME_SIZE_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithAckAndPayload() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame on a stream other than 0
     * Test Outcome: Return a connection error of type PROTOCOL_ERROR and close the connection
     * Spec Section: 6.5
     *
     * Test Notes:
     * SETTINGS frames always apply to a connection, never a single stream.
     * The stream identifier for a SETTINGS frame MUST be zero (0x0). If an
     * endpoint receives a SETTINGS frame whose stream identifier field is
     * anything other than 0x0, the endpoint MUST respond with a connection
     * error of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a PING frame with ACK set
     * Test Outcome: Verify that a PING response is not returned.
     * Spec Section: 6.7
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameSentWithACK() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a PING frame on a stream other than 0
     * Test Outcome: Return a connection error of type PROTOCOL_ERROR and close the connection
     * Spec Section: 6.7
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a PING frame of the wrong size (other than 8 bytes)
     * Test Outcome: Return a connection error of type FRAME_SIZE_ERROR and close the connection.
     * Spec Section: 6.7
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameBadSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a GOAWAY frame on a stream other than 0
     * Test Outcome: Return a connection error of type PROTOCOL_ERROR and close the connection
     * Spec Section: 6.8
     *
     * @throws Exception
     */
    @Test
    public void testGoAwayFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: One stream 0, send a WINDOW_UPDATE that causes the window to exceed 2^31-1 octets
     * Test Outcome: Return a connection error of type FLOW_CONTROL_ERROR and close the connection
     * Spec Section: 6.9.1
     *
     * Test Notes:
     * A sender MUST NOT allow a flow-control window to exceed 2^31-1
     * octets. If a sender receives a WINDOW_UPDATE that causes a flow-
     * control window to exceed this maximum, it MUST terminate either the
     * stream or the connection, as appropriate. For streams, the sender
     * sends a RST_STREAM with an error code of FLOW_CONTROL_ERROR; for the
     * connection, a GOAWAY frame with an error code of FLOW_CONTROL_ERROR
     * is sent.
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameAboveMaxSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: One stream 3, send a WINDOW_UPDATE that causes the window to exceed 2^31-1 octets
     * Test Outcome: Return a RST_STREAM with error of type FLOW_CONTROL_ERROR for that stream.
     * Spec Section: 6.9.1
     *
     * Test Notes:
     * A sender MUST NOT allow a flow-control window to exceed 2^31-1
     * octets. If a sender receives a WINDOW_UPDATE that causes a flow-
     * control window to exceed this maximum, it MUST terminate either the
     * stream or the connection, as appropriate. For streams, the sender
     * sends a RST_STREAM with an error code of FLOW_CONTROL_ERROR; for the
     * connection, a GOAWAY frame with an error code of FLOW_CONTROL_ERROR
     *
     * @throws Exception
     */
    @Test
    public void testTwoWindowUpdateFrameAboveMaxSizeOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a connection preface with a bad "magic" string
     * Test Outcome: Return a PROTOCOL_ERROR and close the connection
     * Spec Section: 3.5
     *
     * Test Notes:
     * Clients and servers MUST treat an invalid connection preface as a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR. A GOAWAY
     * frame (Section 6.8) MAY be omitted in this case, since an invalid
     * preface indicates that the peer is not using HTTP/2.
     *
     * @throws Exception
     */
    @Test
    public void testBadPRI() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Interleave HEADER and CONTINUATION frames between streams on the same connection
     * Test Outcome: Return a connection error, PROTOCOL_ERROR, and close the connection.
     * Spec Section: 4.3
     *
     * Test Notes:
     * Each header block is processed as a discrete unit. Header blocks
     * MUST be transmitted as a contiguous sequence of frames, with no
     * interleaved frames of any other type or from any other stream.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameOnDifferentStreams() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send an incorrect, lower than previous, stream identifier
     * Test Outcome: Return a connection error, PROTOCOL_ERROR, and close the connection.
     * Spec Section: 5.1.1
     *
     * Test Notes:
     * An endpoint that receives an unexpected stream identifier MUST respond with a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFramesDecreasingStreamIds() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame that makes the new stream dependent on itself
     * Test Outcome: A stream cannot depend on itself. An endpoint MUST treat this as a
     * stream error of type PROTOCOL_ERROR, so respond with an RST_STREAM on that stream.
     * Spec Section: 5.3.1
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDependsOnItself() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a PRIORITY frame that makes the new stream dependent on itself
     * Test Outcome: A stream cannot depend on itself. An endpoint MUST treat this as a
     * stream error of type PROTOCOL_ERROR, so respond with an RST_STREAM on that stream.
     * Spec Section: 5.3.1
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameDependsOnItself() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a frame of undefined type, when a CONTINUATION frame was expected.
     * Test Outcome: Respond with a connection error, PROTOCOL_ERROR, and close the connection.
     * Spec Section: 4.3, 5.4.1
     *
     * Test Notes:
     * However, extension frames that appear in the middle of a header block (Section 4.3) are not permitted; these
     * MUST be treated as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testUnknownFrameAfterHeaderFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a SETTINGS frame with an invalid SETTINGS_ENABLE_PUSH value.
     * Test Outcome: Respond with a connection error, PROTOCOL_ERROR, and close the connection.
     * Spec Section: 6.5.2
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithInvalidPushPromise() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage:
     * Test Outcome:
     * Spec Section:
     *
     * The sender MUST NOT send a flow-controlled frame with a length that exceeds the space
     * available in either of the flow-control windows advertised by the receiver.
     *
     * @throws Exception
     */
    @Test
    public void testInitialWindowSize1() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a GOAWAY with an undefined error code.
     * Test Outcome: Respond with a GOAWAY, do not do anything special/different because of the undefined error code.
     * Spec Section: 7
     *
     * Test Notes:
     * Unknown or unsupported error codes MUST NOT trigger any special
     * behavior. These MAY be treated by an implementation as being equivalent to INTERNAL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testGoAwayFrameInvalidErrorCode() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a RST_STREAM with an undefined error code
     * Test Outcome: process the RST_STREAM as normal
     * Spec Section: 7
     *
     * Test Notes:
     * Unknown or unsupported error codes MUST NOT trigger any special
     * behavior. These MAY be treated by an implementation as being
     * equivalent to INTERNAL_ERROR.
     *
     * FIXME: Sending one RST_STREAM with an invalid error code after a HEADERS frame with EoS false doesn't generate a GOAWAY.
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameInvalidErrorCode() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send an Invalid pseudo header in a HEADERS frame
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.6
     *
     * Test Notes:
     * Pseudo-header fields are only valid in the context in which they are defined.
     * Pseudo-header fields defined for requests MUST NOT appear in responses;
     * pseudo-header fields defined for responses MUST NOT appear in requests.
     * Pseudo-header fields MUST NOT appear in trailers.
     * Endpoints MUST treat a request or response that contain undefined or invalid pseudo-header fields as malformed
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithInvalidPseudoHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send an Invalid pseudo header in a HEADERS frame
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.6
     *
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithInvalidRequestPseudoHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send an Invalid pseudo header in a trailing HEADERS frame
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.6
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithInvalidTrailerPseudoHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send valid pseudo headers but after other headers
     * Test Outcome: Respond with a connection error, COMPRESSION_ERROR, and close the connection.
     * Spec Section:8.1.2.6
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithPseudoHeadersLast() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage:
     * Test Outcome:
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithConnectionSpecificFields() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send an invalid TE Header
     * Test Outcome: Respond with a connection error, COMPRESSION_ERROR, and close the connection.
     * Spec Section:8.1.2.6
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithBadTEHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Test Coverage: Send a HEADERS with an empty ":path" pseudo header
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.3
     *
     * Test Notes:
     * The ":path" pseudo-header field includes the path and query parts
     * of the target URI (the "path-absolute" production and optionally a
     * '?' character followed by the "query" production (see Sections 3.3
     * and 3.4 of [RFC3986]). A request in asterisk form includes the
     * value '*' for the ":path" pseudo-header field.
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithEmptyPath() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS with no ":method" pseudo header
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.3
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithoutMethodField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Test Coverage: Send a HEADERS with no ":scheme" pseudo header
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.3
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithoutSchemeField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS with no ":path" pseudo header
     * Test Outcome: Respond with an RST_STREAM, PROTOCOL_ERROR for that stream
     * Spec Section: 8.1.2.3
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithoutPathField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS with two ":method" pseudo header
     * Test Outcome: Respond with a connection error, COMPRESSION_ERROR, and close the connection.
     * Spec Section:8.1.2.3
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDuplicatedMethodField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS with two ":scheme" pseudo header
     * Test Outcome: Respond with a connection error, COMPRESSION_ERROR, and close the connection.
     * Spec Section:8.1.2.3
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDuplicatedSchemeField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS with an two ":path" pseudo header
     * Test Outcome: Respond with a connection error, COMPRESSION_ERROR, and close the connection.
     * Spec Section:8.1.2.3
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDuplicatedPathField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame which has more data than specified by the content-length header
     * Test Outcome: Respond with a RST_STREAM, PROTOCOL_ERROR, and close the stream
     * Spec Section: 8.1.2.6
     *
     * Test Notes:
     * A request or response that includes a payload body can include a
     * content-length header field. A request or response is also malformed
     * if the value of a content-length header field does not equal the sum
     * of the DATA frame payload lengths that form the body. A response
     * that is defined to have no payload, as described in [RFC7230],
     * Section 3.3.2, can have a non-zero content-length header field, even
     * though no content is included in DATA frames.
     *
     * Intermediaries that process HTTP requests or responses (i.e., any
     * intermediary not acting as a tunnel) MUST NOT forward a malformed
     * request or response. Malformed requests or responses that are
     * detected MUST be treated as a stream error (Section 5.4.2) of type
     * PROTOCOL_ERROR.
     *
     * DebugData: content-length header did not match the expected amount of data received
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameIncorrectContentLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage:
     * Test Outcome:
     * Spec Section:
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameIncorrectSumContentLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Sent HEADERS frame after sending a DATA frame that closed the stream
     * Test Outcome: Respond with a connection error, STREAM_CLOSED, PROTOCOL_ERROR, and close the connection
     * Spec Section: Section 8.1
     *
     * Test Notes:
     * An endpoint that receives a HEADERS frame without the END_STREAM flag set after receiving a final (non-
     * informational) status code MUST treat the corresponding request or response as malformed.
     *
     * @throws Exception
     */
    @Test
    public void testSecondHeaderFrameWithoutEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a PUSH_PROMISE frame from the client
     * Test Outcome: Respond with a connection error, PROTOCOL_ERROR, and close the connection
     * Spec Section: 8.2
     *
     * Test Notes.
     * A client cannot push. Thus, servers MUST treat the receipt of a
     * PUSH_PROMISE frame as a connection error (Section 5.4.1) of type
     * PROTOCOL_ERROR. Clients MUST reject any attempt to change the
     * SETTINGS_ENABLE_PUSH setting to a value other than 0 by treating the
     * message as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSendPushPromise() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame payload of 16384 bytes.
     * Test Outcome: Process all Frames as valid.
     * Spec Section: 4.1
     *
     * Test Notes:
     * All implementations MUST be capable of receiving and minimally
     * processing frames up to 2^14 octets in length, plus the 9-octet frame
     * header (Section 4.1). The size of the frame header is not included
     * when describing frame sizes.
     *
     */
    @Test
    public void testDataFrameOf16384Bytes() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame payload of 16384 + 1 bytes, which is 1 above the maximum.
     * Test Outcome: Respond with a connection error of FRAME_SIZE_ERROR and close the connection
     * Spec Section: 4.1
     *
     * An endpoint MUST send an error code of FRAME_SIZE_ERROR if a frame
     * exceeds the size defined in SETTINGS_MAX_FRAME_SIZE, exceeds any
     * limit defined for the frame type, or is too small to contain
     * mandatory frame data.
     *
     */
    @Test
    public void testDataFrameOfMaxPlusOneBytes() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame that has a header entry that is bigger than the max frame size
     * Test Outcome: Respond with a connection error of FRAME_SIZE_ERROR and close the connection
     * Spec Section: 4.3
     *
     * Test Notes:
     * A frame size error in a frame that could alter
     * the state of the entire connection MUST be treated as a connection
     * error (Section 5.4.1); this includes any frame carrying a header
     * block (Section 4.3) (that is, HEADERS, PUSH_PROMISE, and
     * CONTINUATION), SETTINGS, and any frame with a stream identifier of 0.
     */
    @Test
    public void testHeaderFrameOverMaxBytes() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame that has at least one header field name with at least one capital letter
     * Test Outcome: Respond with a connection error of COMPRESSION_ERROR and close the connection
     * Spec Section: 8.1.2
     *
     * Test Notes:
     * Just as in HTTP/1.x, header field names are strings of ASCII characters that are compared in a
     * case-insensitive fashion. However, header field names MUST be converted to lowercase prior to their
     * encoding in HTTP/2. A request or response containing uppercase header field names MUST be treated as malformed.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrameUppercaseField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame with a change to the dynamic table size, after the stream was closed by
     * the other side:
     * Test Outcome: Respond with a connection error of COMPRESSION_ERROR and close the connection
     * Spec Section: 6.5.3, 6.3
     *
     * Test Notes:
     * A change in the maximum size of the dynamic table is signaled via a
     * dynamic table size update (see Section 6.3). This dynamic table size
     * update MUST occur at the beginning of the first header block
     * following the change to the dynamic table size. In HTTP/2, this
     * follows a settings acknowledgment (see Section 6.5.3 of [HTTP2]).
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrameDynamicTableSizeUpdate() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a Header table index value that is too large.
     * Test Outcome: Respond with a connection error of COMPRESSION_ERROR and close the connection
     * Spec Section:
     *
     * Test Notes:
     * Indices strictly greater than the sum of the lengths of both tables
     * MUST be treated as a decoding error.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrameInvalidIndex() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame that has too much padding and is therefore invalid
     * Test Outcome: Respond with a connection error of COMPRESSION_ERROR and close the connection
     * Spec Section: 6.2, 6.1
     *
     * Test Notes:
     * As the Huffman-encoded data doesn't always end at an octet boundary,
     * some padding is inserted after it, up to the next octet boundary. To
     * prevent this padding from being misinterpreted as part of the string
     * literal, the most significant bits of the code corresponding to the
     * EOS (end-of-string) symbol are used.
     *
     * Upon decoding, an incomplete code at the end of the encoded data is
     * to be considered as padding and discarded. A padding strictly longer
     * than 7 bits MUST be treated as a decoding error. A padding not
     * corresponding to the most significant bits of the code for the EOS
     * symbol MUST be treated as a decoding error. A Huffman-encoded string
     * literal containing the EOS symbol MUST be treated as a decoding
     * error.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrameInvalidHuffmanWithExtraPad() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a HEADERS frame that has a Huffman index value of 0
     * Test Outcome: Respond with a connection error of COMPRESSION_ERROR and close the connection
     * Spec Section:
     *
     * Test Notes:
     * The index value of 0 is not used. It MUST be treated as a decoding
     * error if found in an indexed header field representation.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrameFieldIndex0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Adjust the window size from negative to positive
     * Test Outcome: Data is sent once the window size is large enough
     * Spec Section: 6.9.2
     *
     * Test Notes.
     * When the value of SETTINGS_INITIAL_WINDOW_SIZE changes, a receiver MUST adjust the size of
     * all stream flow-control windows that it maintains by the difference between the new value and the old value.
     *
     * FIXME: The server is not adjusting the size of the DATA frame after the new SETTINGS_INITIAL_WINDOW_SIZE changed from 0 to 1.
     * This test will be incomplete when the problem is fixed as I don't know how the DATA frame will look like.
     *
     * @throws Exception
     */
    @Test
    public void testNegativeToPositiveWindowSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Open more streams from the client than the server has specified in SETTINGS_MAX_CONCURRENT_STREAMS (200)
     * Test Outcome: On the stream that exceeds the server's limit (403) The server sends a reset frame with error code REFUSED_STREAM
     * Spec Section: 6.5.2
     *
     * Notes: stream 1 (servicing the initial upgraded request) will be closed, but streams 3 and up are left open by the client.
     * So client (odd) streams 3 through 401 gives 200 open streams; stream 403 will be peer stream number 201.
     *
     * @throws Exception
     */
    @Test
    public void testExceedMaxConcurrentStreams() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a POST request with body data, but no content-length header
     * Test Outcome: The server response contains a string from the request body
     *
     * @throws Exception
     */
    @Test
    public void testSendPostRequestWithBody() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send a DATA frame
     * Test Outcome: Expect WINDOW_UPDATE frames matching the DATA payload size
     *
     * @throws Exception
     */
    @Test
    public void testSimpleWindowUpdatesReceived() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send multiple DATA frames
     * Test Outcome: Expect WINDOW_UPDATE frames matching the DATA payloads sent
     *
     * @throws Exception
     */
    @Test
    public void testMultiStreamWindowUpdatesReceived() throws Exception {
        runTest(dataServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Send an excessive number of PING frames to the server
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    // Disable for now 268372
    //@Test
    //public void testPingStress() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send an excessive number of PRIORITY frames to the server
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    // Disable for now 268372
    //@Test
    //public void testPriorityStress() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Create an excessive number of streams on the server, each with a malformed
     * request. The server should respond to each stream with a reset.
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    // Disable for now 268372
    //@Test
    //public void testResetStress() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send an excessive number of empty data frames on a single stream
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    // Disable for now 268372
    //@Test
    //public void testEmptyDataFrameStress() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send an excessive number of empty header/continuation frames on a single stream
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    // Disable for now 268372
    //@Test
    //public void testEmptyHeaderFrameStress() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Send an excessive number of settings frames to the server
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    // Disable for now 268372
    //@Test
    //public void testSettingsFrameStress() throws Exception {
    //    runTest(defaultServletPath, testName.getMethodName());
    //}

    /**
     * Test Coverage: Create an excessive number of streams on the server, resetting each stream.
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    @Test
    public void testRapidReset() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Test Coverage: Create an streams on the server, 101 over the number of maxConcurrentStreams.
     * The server should respond to each stream with a reset.
     * Test Outcome: GOAWAY received from server
     *
     * @throws Exception
     */
    @Test
    public void testMaxStreamsRefused() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }
}
