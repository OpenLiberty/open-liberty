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
public class Http2FullModeTests extends FATServletClient {

    private static final String CLASS_NAME = Http2FullModeTests.class.getName();
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

    private void runTest(String servletPath, String testName) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test " + servletPath + " on server " + server.getServerName());
        }
        FATServletClient.runTest(runtimeServer,
                                 servletPath + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    public void runStressTest(int iterations) throws Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "runTest()", "Running test with iterations of: " + Utils.STRESS_ITERATIONS);
        }

        FATServletClient.runTest(runtimeServer,
                                 "H2FATDriver/H2FATDriverServlet?hostName=" + server.getHostname() + "&port=" + server.getHttpSecondaryPort() + "&iterations="
                                                + iterations + "&testdir=" + Utils.TEST_DIR,
                                 testName);
    }

    @Test
    public void testUpgradeHeaderFollowedBySettingsFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a Priority frame on an idle stream. All streams start in idle state.
     * Receiving any frame other than HEADERS or PRIORITY on a stream in this state MUST be treated as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     */
    @Test
    public void testPriorityFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a Window_Update frame on an half-closed stream.
     * half-closed (remote):
     * A stream that is "half-closed (remote)" is no longer being used by
     * the peer to send frames. In this state, an endpoint is no longer
     * obligated to maintain a receiver flow-control window.
     *
     * If an endpoint receives additional frames, other than
     * WINDOW_UPDATE, PRIORITY, or RST_STREAM, for a stream that is in
     * this state, it MUST respond with a stream error (Section 5.4.2) of
     * type STREAM_CLOSED.
     */
    @Test
    public void testWindowsUpdateFrameOnHalfClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a RST_STREAM frame on an half-closed stream.
     * half-closed (remote):
     * A stream that is "half-closed (remote)" is no longer being used by
     * the peer to send frames. In this state, an endpoint is no longer
     * obligated to maintain a receiver flow-control window.
     *
     * If an endpoint receives additional frames, other than
     * WINDOW_UPDATE, PRIORITY, or RST_STREAM, for a stream that is in
     * this state, it MUST respond with a stream error (Section 5.4.2) of
     * type STREAM_CLOSED.
     */
    @Test
    public void testRstStreamFrameOnHalfClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a priority frame after a stream has finished.
     * An endpoint MUST NOT send frames other than PRIORITY on a closed stream.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a Continuation frame after a Headers frame.
     *
     * Any number of CONTINUATION
     * frames can be sent, as long as the preceding frame is on the same
     * stream and is a HEADERS, PUSH_PROMISE, or CONTINUATION frame without
     * the END_HEADERS flag set.
     *
     * @throws Exception
     *
     */
    @Test
    public void testContFrameAfterHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testTwoContFrameAfterHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Sending HEADER with 8 bytes padding and 8 bytes data block.
     *
     * The HEADERS frame can include padding.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeaderWithPaddingFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending HEADER with priority set.
     *
     * @throws Exception
     */
    @Test
    public void testSendHeaderFrameWithPriorityValue() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending a PRIORITY frame on stream 3 before sending anything else.
     *
     * The PRIORITY frame (type=0x2) specifies the sender-advised priority
     * of a stream (Section 5.3). It can be sent in any stream state,
     * including idle or closed streams.
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithPriorityOne() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testSendPriorityFrameWithPriority256() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Send a PRIORITY frame on stream 3 with a stream dependency on stream 5.
     *
     * Stream Dependency: A 31-bit stream identifier for the stream that
     * this stream depends on (see Section 5.3). This field is only
     * present if the PRIORITY flag is set.
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithStreamDependency() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending PRIORITY frame with exclusive flag on.
     *
     * E: A single-bit flag indicating that the stream dependency is
     * exclusive (see Section 5.3).
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameWithExclusiveFlag() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Sending PRIORITY on a higher stream (5) and then send a HEADER on stream (3); in this order.
     *
     * The PRIORITY frame can be sent for a stream in the "idle" or "closed"
     * state. This allows for the reprioritization of a group of dependent
     * streams by altering the priority of an unused or closed parent
     * stream.
     *
     * @throws Exception
     */
    @Test
    public void testSendPriorityFrameOnHigherStreamIdThanHeadersFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending RST_STREAM to cancel stream id 3 after sending a HEADER frame with end of stream false.
     *
     * The RST_STREAM frame (type=0x3) allows for immediate termination of a
     * stream. RST_STREAM is sent to request cancellation of a stream or to
     * indicate that an error condition has occurred.
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameToCancelStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Sending a SETTING frame.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithValues() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending WindowUpdate on stream 0 to affect all streams.
     *
     * Flow control operates at two levels: on each individual stream and on the entire connection.
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Sending WindowUpdate on stream 3 to affect this stream only.
     *
     * Flow control operates at two levels: on each individual stream and on the entire connection.
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * WTL: the server is responding to this request with a 404. Liberty doesn't support the '*' on http 1.1, so for now
     * we can ignore this case.
     *
     * @throws Exception
     */
    //@Test
    public void testSendOptionsRequest() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * :path pseudo-header field:
     *
     * @throws Exception
     */
    @Test
    public void testSendOptionsRequestUrlPath() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testIndexedHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testIndexedHeaderFieldNoHuffman() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testIndexedHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testIndexedCustomHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testIndexedCustomHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNoIndexHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNoIndexHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNoIndexCustomHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNoIndexCustomHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNeverIndexHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNeverIndexHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNeverIndexCustomHeaderField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testNeverIndexCustomHeaderFieldHuffmanEncoded() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Protocols that use HPACK determine the maximum size that the encoder
     * is permitted to use for the dynamic table. In HTTP/2, this value is
     * determined by the SETTINGS_HEADER_TABLE_SIZE setting (see
     * Section 6.5.2 of [HTTP2]).
     *
     * @throws Exception
     */
    @Test
    public void testDynamicTableSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Multiple updates to the maximum table size can occur between the
     * transmission of two header blocks.
     *
     * @throws Exception
     */
    @Test
    public void testDynamicTableSizeChanged() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Implementations MUST discard frames
     * that have unknown or unsupported types.
     *
     * @throws Exception
     */
    @Test
    public void testUnknownFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Flags are assigned semantics specific to the indicated frame type.
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

    /**
     * Need to be able to create a huge Data frame...
     *
     * @throws Exception
     */
    //@Test
    public void testDataFrameMaxSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A decoding
     * error in a header block MUST be treated as a connection error
     * (Section 5.4.1) of type COMPRESSION_ERROR.
     */
    @Test
    public void testInvalidHeaderBlock() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * 8.1.2.3:
     * All HTTP/2 requests MUST include exactly one valid value for the ":method", ":scheme", and ":path" pseudo-header fields,
     * unless it is a CONNECT request (Section 8.3).
     */
    @Test
    public void testInvalidHeaderFields() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * idle: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * this state MUST be treated as a connection error (Section 5.4.1)
     * of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * idle: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * this state MUST be treated as a connection error (Section 5.4.1)
     * of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * idle: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * this state MUST be treated as a connection error (Section 5.4.1)
     * of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testWindowUpdateFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * idle: Receiving any frame other than HEADERS or PRIORITY on a stream in
     * this state MUST be treated as a connection error (Section 5.4.1)
     * of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameOnIdleStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * closed:
     *
     * An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error
     * (Section 5.4.2) of type STREAM_CLOSED.
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * closed:
     *
     * An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error
     * (Section 5.4.2) of type STREAM_CLOSED.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * closed:
     *
     * An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error
     * (Section 5.4.2) of type STREAM_CLOSED.
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameOnClosedStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testDataFrameAfterHeaderFrameWithEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * closed:
     *
     * An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error
     * (Section 5.4.2) of type STREAM_CLOSED.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameAfterHeaderFrameWithEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * closed:
     *
     * An endpoint that receives any frame other than PRIORITY
     * after receiving a RST_STREAM MUST treat that as a stream error
     * (Section 5.4.2) of type STREAM_CLOSED.
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameAfterHeaderFrameWithEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * If the END_HEADERS bit is not set, this frame MUST be followed by
     * another CONTINUATION frame. A receiver MUST treat the receipt of
     * any other type of frame or a frame on a different stream as a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameAfterContinuationFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * CONTINUATION frames MUST be associated with a stream. If a
     * CONTINUATION frame is received whose stream identifier field is 0x0,
     * the recipient MUST respond with a connection error (Section 5.4.1) of
     * type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testContinuationFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * CONTINUATION frames MUST be associated with a stream. If a
     * CONTINUATION frame is received whose stream identifier field is 0x0,
     * the recipient MUST respond with a connection error (Section 5.4.1) of
     * type PROTOCOL_ERROR.
     *
     * @throws
     */
    @Test
    public void testContinuationFrameAfterAnEndOfHeaders() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * CONTINUATION frames MUST be associated with a stream. If a
     * CONTINUATION frame is received whose stream identifier field is 0x0,
     * the recipient MUST respond with a connection error (Section 5.4.1) of
     * type PROTOCOL_ERROR.
     *
     * @throws
     */
    @Test
    public void testSecondContinuationFrameAfterAnEndOfHeaders() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * CONTINUATION frames MUST be associated with a stream. If a
     * CONTINUATION frame is received whose stream identifier field is 0x0,
     * the recipient MUST respond with a connection error (Section 5.4.1) of
     * type PROTOCOL_ERROR.
     *
     * @throws
     */
    @Test
    public void testContinuationFrameAfterDataFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testDataFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * The total number of padding octets is determined by the value of the
     * Pad Length field. If the length of the padding is the length of the
     * frame payload or greater, the recipient MUST treat this as a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testDataFrameBadPaddingLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A HEADERS frame without the END_HEADERS flag set MUST be followed
     * by a CONTINUATION frame for the same stream. A receiver MUST
     * treat the receipt of any other type of frame or a frame on a
     * different stream as a connection error (Section 5.4.1) of type
     * PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameAfterHeaderFrameNoEndHeaders() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testHeaderFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * The HEADERS frame can include padding. Padding fields and flags are
     * identical to those defined for DATA frames (Section 6.1). Padding
     * that exceeds the size remaining for the header block fragment MUST be
     * treated as a PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameBadPaddingLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testPriorityFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * A PRIORITY frame with a length other than 5 octets MUST be treated as
     * a stream error (Section 5.4.2) of type FRAME_SIZE_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameLength4() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    @Test
    public void testRstStreamFrameOnStream0() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testRstStreamFrameLength3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Indicates the sender's initial
     * window size (in octets) for stream-level flow control. The
     * initial value is 2^16-1 (65,535) octets.
     *
     * This setting affects the window size of all streams (see
     * Section 6.9.2).
     *
     * Values above the maximum flow-control window size of 2^31-1 MUST
     * be treated as a connection error (Section 5.4.1) of type
     * FLOW_CONTROL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithLessThanMinimunFrameSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * The initial value is 2^14 (16,384) octets. The value advertised
     * by an endpoint MUST be between this initial value and the maximum
     * allowed frame size (2^24-1 or 16,777,215 octets), inclusive.
     * Values outside this range MUST be treated as a connection error
     * (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithInvalidFrameSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Indicates the sender's initial
     * window size (in octets) for stream-level flow control. The
     * initial value is 2^16-1 (65,535) octets.
     *
     * This setting affects the window size of all streams (see
     * Section 6.9.2).
     *
     * Values above the maximum flow-control window size of 2^31-1 MUST
     * be treated as a connection error (Section 5.4.1) of type
     * FLOW_CONTROL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithInvalidMaxWindowSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * An endpoint that receives a SETTINGS frame with any unknown or
     * unsupported identifier MUST ignore that setting.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithUnkownIdentifier() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * ACK (0x1): When set, bit 0 indicates that this frame acknowledges
     * receipt and application of the peer's SETTINGS frame. When this
     * bit is set, the payload of the SETTINGS frame MUST be empty.
     * Receipt of a SETTINGS frame with the ACK flag set and a length
     * field value other than 0 MUST be treated as a connection error
     * (Section 5.4.1) of type FRAME_SIZE_ERROR. For more information,
     * see Section 6.5.3 ("Settings Synchronization").
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithAckAndPayload() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * SETTINGS frames always apply to a connection, never a single stream.
     * The stream identifier for a SETTINGS frame MUST be zero (0x0). If an
     * endpoint receives a SETTINGS frame whose stream identifier field is
     * anything other than 0x0, the endpoint MUST respond with a connection
     * error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A SETTINGS frame with a length other than a multiple of 6 octets MUST
     * be treated as a connection error (Section 5.4.1) of type
     * FRAME_SIZE_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameBadSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * An endpoint MUST NOT respond to PING frames containing this flag.
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameSentWithACK() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * If a PING
     * frame is received with a stream identifier field value other than
     * 0x0, the recipient MUST respond with a connection error
     * (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testPingFrameBadSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * The GOAWAY frame applies to the connection, not a specific stream.
     * An endpoint MUST treat a GOAWAY frame with a stream identifier other
     * than 0x0 as a connection error (Section 5.4.1) of type
     * PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testGoAwayFrameOnStream3() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
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
    public void testTwoWindowUpdateFrameAboveMaxSize() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
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
     * Clients and servers MUST treat an invalid connection preface as a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR. A GOAWAY
     * frame (Section 6.8) MAY be omitted in this case, since an invalid
     * preface indicates that the peer is not using HTTP/2.
     *
     * FIXME: When this test is on, the server fails to stop.
     *
     * @throws Exception
     */
    @Test
    public void testBadPRI() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
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
     * An endpoint that
     * receives an unexpected stream identifier MUST respond with a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFramesDecreasingStreamIds() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A stream cannot depend on itself. An endpoint MUST treat this as a
     * stream error (Section 5.4.2) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDependsOnItself() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * A stream cannot depend on itself. An endpoint MUST treat this as a
     * stream error (Section 5.4.2) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testPriorityFrameDependsOnItself() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * However, extension frames that appear in
     * the middle of a header block (Section 4.3) are not permitted; these
     * MUST be treated as a connection error (Section 5.4.1) of type
     * PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testUnknownFrameAfterHeaderFrame() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * The initial value is 1, which indicates that server push is
     * permitted. Any value other than 0 or 1 MUST be treated as a
     * connection error (Section 5.4.1) of type PROTOCOL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testSettingFrameWithInvalidPushPromise() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * The sender MUST NOT
     * send a flow-controlled frame with a length that exceeds the space
     * available in either of the flow-control windows advertised by the
     * receiver.
     *
     * @throws Exception
     */
    @Test
    public void testInitialWindowSize1() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * Unknown or unsupported error codes MUST NOT trigger any special
     * behavior. These MAY be treated by an implementation as being
     * equivalent to INTERNAL_ERROR.
     *
     * @throws Exception
     */
    @Test
    public void testGoAwayFrameInvalidErrorCode() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
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
     *
     * Pseudo-header fields are only valid in the context in which they are
     * defined. Pseudo-header fields defined for requests MUST NOT appear
     * in responses; pseudo-header fields defined for responses MUST NOT
     * appear in requests. Pseudo-header fields MUST NOT appear in
     * trailers. Endpoints MUST treat a request or response that contains
     * undefined or invalid pseudo-header fields as malformed
     * (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithInvalidPseudoHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Pseudo-header fields are only valid in the context in which they are
     * defined. Pseudo-header fields defined for requests MUST NOT appear
     * in responses; pseudo-header fields defined for responses MUST NOT
     * appear in requests. Pseudo-header fields MUST NOT appear in
     * trailers. Endpoints MUST treat a request or response that contains
     * undefined or invalid pseudo-header fields as malformed
     * (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithInvalidRequestPseudoHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Pseudo-header fields are only valid in the context in which they are
     * defined. Pseudo-header fields defined for requests MUST NOT appear
     * in responses; pseudo-header fields defined for responses MUST NOT
     * appear in requests. Pseudo-header fields MUST NOT appear in
     * trailers. Endpoints MUST treat a request or response that contains
     * undefined or invalid pseudo-header fields as malformed
     * (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithInvalidTrailerPseudoHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Pseudo-header fields are only valid in the context in which they are
     * defined. Pseudo-header fields defined for requests MUST NOT appear
     * in responses; pseudo-header fields defined for responses MUST NOT
     * appear in requests. Pseudo-header fields MUST NOT appear in
     * trailers. Endpoints MUST treat a request or response that contains
     * undefined or invalid pseudo-header fields as malformed
     * (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithPseudoHeadersLast() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Pseudo-header fields are only valid in the context in which they are
     * defined. Pseudo-header fields defined for requests MUST NOT appear
     * in responses; pseudo-header fields defined for responses MUST NOT
     * appear in requests. Pseudo-header fields MUST NOT appear in
     * trailers. Endpoints MUST treat a request or response that contains
     * undefined or invalid pseudo-header fields as malformed
     * (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithConnectionSpecificFields() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * Pseudo-header fields are only valid in the context in which they are
     * defined. Pseudo-header fields defined for requests MUST NOT appear
     * in responses; pseudo-header fields defined for responses MUST NOT
     * appear in requests. Pseudo-header fields MUST NOT appear in
     * trailers. Endpoints MUST treat a request or response that contains
     * undefined or invalid pseudo-header fields as malformed
     * (Section 8.1.2.6).
     */
    @Test
    public void testHeaderFrameWithBadTEHeader() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
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
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithoutMethodField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithoutSchemeField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameWithoutPathField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDuplicatedMethodField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDuplicatedSchemeField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
     * All HTTP/2 requests MUST include exactly one valid value for the
     * ":method", ":scheme", and ":path" pseudo-header fields, unless it is
     * a CONNECT request (Section 8.3). An HTTP request that omits
     * mandatory pseudo-header fields is malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testHeaderFrameDuplicatedPathField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
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
     *
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
     * @throws Exception
     */
    @Test
    public void testHeaderFrameIncorrectSumContentLength() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * An endpoint that receives a HEADERS
     * frame without the END_STREAM flag set after receiving a final (non-
     * informational) status code MUST treat the corresponding request or
     * response as malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testSecondHeaderFrameWithoutEndOfStream() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     * * A client cannot push. Thus, servers MUST treat the receipt of a
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
     * Endpoints MUST NOT exceed the limit set by their peer. An endpoint
     * that receives a HEADERS frame that causes its advertised concurrent
     * stream limit to be exceeded MUST treat this as a stream error
     * (Section 5.4.2) of type PROTOCOL_ERROR or REFUSED_STREAM.
     *
     * WTL: this is not how SETTINGS_MAX_CONCURRENT_STREAMS works - that setting is only dictates how many streams the other peer is
     * allowed to open, not how many the current peer is.
     *
     * @throws Exception
     */
    //@Test
    public void testSingleConnectionStressMaxStreams() throws Exception {
        runStressTest(5);
    }

    /**
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
     * Just as in HTTP/1.x, header field names are strings of ASCII
     * characters that are compared in a case-insensitive fashion. However,
     * header field names MUST be converted to lowercase prior to their
     * encoding in HTTP/2. A request or response containing uppercase
     * header field names MUST be treated as malformed (Section 8.1.2.6).
     *
     * @throws Exception
     */
    @Test
    public void testSendHeadersFrameUppercaseField() throws Exception {
        runTest(defaultServletPath, testName.getMethodName());
    }

    /**
     *
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
     * When the
     * value of SETTINGS_INITIAL_WINDOW_SIZE changes, a receiver MUST adjust
     * the size of all stream flow-control windows that it maintains by the
     * difference between the new value and the old value.
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
}
