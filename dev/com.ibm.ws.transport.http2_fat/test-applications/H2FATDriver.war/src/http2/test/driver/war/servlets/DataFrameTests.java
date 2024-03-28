/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package http2.test.driver.war.servlets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.frames.FrameWindowUpdate;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for http2 data frame behaviors
 */
@WebServlet(urlPatterns = "/DataFrameTests", asyncSupported = true)
public class DataFrameTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;

    private final String dataString = "ABC123";

    /**
     * Send a DATA frame on a stream that's in IDLE state. Expect a STREAM_CLOSED error in response.
     *
     * spec 6.1
     * If a DATA frame is received whose stream is not in "open" or "half-closed (local)" state, the recipient MUST respond
     * with a stream error (Section 5.4.2) of type STREAM_CLOSED.
     */
    public void testDataOnIdleStream(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testDataOnIdleStream";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] chfwDebugData = "DATA Frame Received in the wrong state of: IDLE".getBytes();
        byte[] nettyDebugData = "Stream 3 does not exist".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, PROTOCOL_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultUpgradedConnection(h2Client);

        String dataString = "invalid data frame";
        FrameData data = new FrameData(3, dataString.getBytes(), 0, false, false, false);

        h2Client.sendFrame(data);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a DATA frame with the padding field set to 0. Expect a normal response.
     */
    public void testZeroLengthPadding(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testZeroLengthPadding";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        // cannot assume language of test machine
        secondHeadersReceived.add(new H2HeaderField("content-language", ".*"));

        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 15, false, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        // Initialize connection after adding expected frames
        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        String dataString = "invalid data frame";
        FrameData dataFrame = new FrameData(3, dataString.getBytes(), 0, true, true, false);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a DATA frame with a frame length of 5 and a padding length of 6. Expect a GOAWAY:
     *
     * If the length of the padding is the length of the
     * frame payload or greater, the recipient MUST treat this as a
     * connection error
     */
    public void testInvalidPaddingValue(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testInvalidPaddingValue";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Add expected goaway before the init sequence.
        byte[] chfwDebugData = "Error processing the payload for DATA frame on stream 5".getBytes();
        byte[] nettyDebugData = "Frame payload too small for padding.".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, PROTOCOL_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(5, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        byte[] dataBytes = hexStringToByteArray("0000050009000000050654657374");

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendBytes(dataBytes);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);

    }

    /**
     * Send a DATA frame that exceeds the maximum payload size. Expect a FRAME_SIZE_ERROR in return.
     */
    public void testDataFrameExceedingMaxFrameSize(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testDataFrameExceedingMaxFrameSize";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] chfwDebugData = "DATA payload greater than allowed by the max frame size".getBytes();
        byte[] nettyDebugData = "Frame length: 57601 exceeds maximum: 57344".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, FRAME_SIZE_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(5, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        byte[] data = new byte[57345];
        for (int i = 0; i < data.length; i++) {
            data[i] = 0x01;
        }
        FrameData dataFrame = new FrameData(5, data, 255, true, true, false);

        h2Client.sendFrame(frameHeadersToSend);

        // delay to try to make sure all activity is done before sending the frame, so we can see
        // the GOAWAY coming back before the connection close
        try {
            Thread.sleep(1000);
        } catch (Exception x) {
        }

        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);

    }

    /**
     * Send a DATA frame with no EOS flag, and expect WINDOW_UPDATE frames from the server which restore its read window
     */
    public void testSimpleWindowUpdatesReceived(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testSimpleWindowUpdatesReceived";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // expect window updates on streams 0 (connection) and 3
        FrameWindowUpdate streamUpdateFrame = new FrameWindowUpdate(3, 1000, false);
        FrameWindowUpdate connectionUpdateFrame = new FrameWindowUpdate(0, 1000, false);
        h2Client.addExpectedFrame(streamUpdateFrame);
        h2Client.addExpectedFrame(connectionUpdateFrame);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // generate 1000 bytes for data frame
        byte[] data = new byte[999];
        for (int i = 0; i < data.length; i++) {
            data[i] = 0x01;
        }
        FrameData dataFrame = new FrameData(3, data, 0, false, true, false);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame);

        // now send EOS
        dataFrame = new FrameData(3, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a DATA frames on streams 3 and 7 with no EOS, and expect WINDOW_UPDATE frames from the server which restore the connection and
     * stream read windows. Additionally, send DATA on stream 5 with an EOS set - so no WINDOW_UPDATE is expected.
     */
    public void testMultiStreamWindowUpdatesReceived(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testMultiStreamWindowUpdatesReceived";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // expect window updates on streams 0 (connection), 3, and 7
        FrameWindowUpdate stream3UpdateFrame = new FrameWindowUpdate(3, 1000, false);
        FrameWindowUpdate stream7UpdateFrame = new FrameWindowUpdate(7, 1000, false);
        h2Client.addExpectedFrame(stream3UpdateFrame);
        h2Client.addExpectedFrame(stream7UpdateFrame);
        // expect three connection window updates - one for each stream
        FrameWindowUpdate connectionUpdateFrame = new FrameWindowUpdate(0, 1000, false);
        h2Client.addExpectedFrame(connectionUpdateFrame);
        h2Client.addExpectedFrame(connectionUpdateFrame);
        h2Client.addExpectedFrame(connectionUpdateFrame);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend3 = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend3.setHeaderEntries(firstHeadersToSend);

        FrameHeadersClient frameHeadersToSend5 = new FrameHeadersClient(5, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend5.setHeaderEntries(firstHeadersToSend);

        FrameHeadersClient frameHeadersToSend7 = new FrameHeadersClient(7, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend7.setHeaderEntries(firstHeadersToSend);

        // generate 1000 bytes for data frame
        byte[] data = new byte[999];
        for (int i = 0; i < data.length; i++) {
            data[i] = 0x01;
        }
        FrameData dataFrame3 = new FrameData(3, data, 0, false, true, false);
        // EOS set, so we do NOT expect a window update response
        FrameData dataFrame5 = new FrameData(5, data, 0, true, true, false);
        FrameData dataFrame7 = new FrameData(7, data, 0, false, true, false);

        // send over all the headers and data
        h2Client.sendFrame(frameHeadersToSend3);
        h2Client.sendFrame(frameHeadersToSend5);
        h2Client.sendFrame(frameHeadersToSend7);
        h2Client.sendFrame(dataFrame3);
        h2Client.sendFrame(dataFrame5);
        h2Client.sendFrame(dataFrame7);

        // now send EOS for streams 3 and 7
        FrameData eosFrame = new FrameData(3, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(eosFrame);
        eosFrame = new FrameData(7, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(eosFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a DATA frame with no EOS flag, and expect WINDOW_UPDATE frames from the server which restore its stream read window
     * This test uses a server.xml with stream initial window size set to 1000 and connection set to 65537
     *
     * Window update frames aren't sent until the window is less than 1/2 the max window size. The stream max window size is set
     * to 1000 and the connection is set to 32768.
     *
     * server.xml has stream window set to 1000, connection window set to 35567, and limit window update frames set to true
     *
     */
    public void testSimpleWindowUpdatesReceivedLimitWindowUpdateFrames(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testSimpleWindowUpdatesReceivedLimitWindowUpdateFrames";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // expect window updates on streams 0 (connection) and 3 (stream)
        FrameWindowUpdate streamUpdateFrame = new FrameWindowUpdate(3, 502, false);
        h2Client.addExpectedFrame(streamUpdateFrame);

//        if (USING_NETTY) {
//            h2Client.addExpectedFrame(new FrameSettings(0, -1, -1, 200, 1000, 57344, -1, false));
//            FrameHeaders headers = addFirstExpectedHeaders(h2Client);
//            h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
//            h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
//            h2Client.waitFor(headers);
//        } else {
//            FrameWindowUpdate connectionUpdateFrame = new FrameWindowUpdate(0, 2, false);
//            h2Client.addExpectedFrame(connectionUpdateFrame);
//            setupDefaultUpgradedConnection(h2Client);
//        }

        if (!USING_NETTY) {
            FrameWindowUpdate connectionUpdateFrame = new FrameWindowUpdate(0, 2, false);
            h2Client.addExpectedFrame(connectionUpdateFrame);
        }

        h2Client.addExpectedFrame(new FrameSettings(0, -1, -1, 100, 1000, 57344, -1, false));
        FrameHeaders headers = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(headers);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // generate 251 bytes for data frame
        byte[] data = new byte[250];
        for (int i = 0; i < data.length; i++) {
            data[i] = 0x01;
        }
        FrameData dataFrame = new FrameData(3, data, 0, false, true, false);

        //Send headers followed by 251 bytes, and then another 251 bytes.
        // We should only get one window_udpate frame back, and it should have a value of 502.
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame);
        h2Client.sendFrame(dataFrame);

        // now send EOS
        dataFrame = new FrameData(3, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a DATA frames on streams 3 and 7 with no EOS, and expect WINDOW_UPDATE frames from the server which restore the
     * stream read windows. Additionally, send DATA on stream 5 with an EOS set - so no WINDOW_UPDATE is expected.
     *
     * server.xml has stream window set to 1000, connection window set to 35567, and limit window update frames set to true
     *
     */
    public void testMultiStreamWindowUpdatesReceivedLimitWindowUpdateFrames(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testMultiStreamWindowUpdatesReceivedLimitWindowUpdateFrames";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // expect window updates on streams 0 (connection), 3, and 7
        FrameWindowUpdate stream3UpdateFrame = new FrameWindowUpdate(3, 1000, false);
        FrameWindowUpdate stream7UpdateFrame = new FrameWindowUpdate(7, 1000, false);
        h2Client.addExpectedFrame(stream3UpdateFrame);
        h2Client.addExpectedFrame(stream7UpdateFrame);
        // connection window size is large, so no window_udpates are expected on stream 0

//        if (USING_NETTY) {
//            h2Client.addExpectedFrame(new FrameSettings(0, -1, -1, 200, 1000, 57344, -1, false));
//            FrameHeaders headers = addFirstExpectedHeaders(h2Client);
//            h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
//            h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
//            h2Client.waitFor(headers);
//        } else {
//            setupDefaultUpgradedConnection(h2Client);
//        }

        h2Client.addExpectedFrame(new FrameSettings(0, -1, -1, 100, 1000, 57344, -1, false));
        FrameHeaders headers = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(headers);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend3 = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend3.setHeaderEntries(firstHeadersToSend);

        FrameHeadersClient frameHeadersToSend5 = new FrameHeadersClient(5, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend5.setHeaderEntries(firstHeadersToSend);

        FrameHeadersClient frameHeadersToSend7 = new FrameHeadersClient(7, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend7.setHeaderEntries(firstHeadersToSend);

        // generate 1000 bytes for data frame
        byte[] data = new byte[999];
        for (int i = 0; i < data.length; i++) {
            data[i] = 0x01;
        }
        FrameData dataFrame3 = new FrameData(3, data, 0, false, true, false);
        // EOS set, so we do NOT expect a window update response since the stream is closing
        FrameData dataFrame5 = new FrameData(5, data, 0, true, true, false);
        FrameData dataFrame7 = new FrameData(7, data, 0, false, true, false);

        // send over all the headers and data
        h2Client.sendFrame(frameHeadersToSend3);
        h2Client.sendFrame(frameHeadersToSend5);
        h2Client.sendFrame(frameHeadersToSend7);
        h2Client.sendFrame(dataFrame3);
        h2Client.sendFrame(dataFrame5);
        h2Client.sendFrame(dataFrame7);

        // Sleep just a bit to wait for server to send window update frames before we send eos on streams 3 and 7
        try {
            Thread.sleep(1000);
        } catch (Exception x) {
        }

        // now send EOS for streams 3 and 7
        FrameData eosFrame = new FrameData(3, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(eosFrame);
        eosFrame = new FrameData(7, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(eosFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);

    }

    /**
     * Use both http/2 and deflate compression at the same time. This should not produce an http/2 FRAME_SIZE_ERROR
     */
    public void testCompression(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testCompression";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(5, null, 0, 0, 15, false, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(5, null, 0, 0, 0, false, true, false, false, false, false);

        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", COMPRESSION_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("data", "compression"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("content-type", "application/xml"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("accept-encoding", "deflate"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(5, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);

        // delay to try to make sure all activity is done before sending the frame, so we can see
        // the GOAWAY coming back before the connection close
        try {
            Thread.sleep(1000);
        } catch (Exception x) {
        }

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                  + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}
