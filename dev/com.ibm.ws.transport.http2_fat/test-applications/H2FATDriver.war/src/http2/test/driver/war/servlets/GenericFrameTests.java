/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for generic http2 frame behaviors
 */
@WebServlet(urlPatterns = "/GenericFrameTests", asyncSupported = true)
public class GenericFrameTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;

    private final String dataString = "ABC123";

    /**
     * Create a stream 7, then create a stream 5, which is illegal; the server should emit a GOAWAY with the error code PROTOCOL_ERROR
     */
    public void testInvalidStreamIdSequence(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testInvalidStreamIdSequence";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the GOAWAY / error code that the server should emit following an illegal stream ID order
        byte[] debugData = "received a new stream with a lower ID than previous; current stream-id: 5 highest stream-id: 7".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 7, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultPreface(h2Client);

        //Expected headers for stream 7 request
        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        // cannot assume language of test machine
        secondHeadersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient secondFrameHeaders = new FrameHeadersClient(7, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);

        //Headers frame to send for stream 7 request
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(7, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.addExpectedFrame(secondFrameHeaders.clone());
        h2Client.addExpectedFrame(new FrameData(7, dataString.getBytes(), 0, false, false, false));

        // Skip over stream IDs 3 and 5 create a stream with ID 7
        h2Client.sendFrame(frameHeadersToSend.clone());

        // make sure stream 7 is seen before stream 5
        h2Client.waitFor(secondFrameHeaders);

        // Send out the INVALID stream ID 5
        frameHeadersToSend.setStreamID(5);
        h2Client.sendFrame(frameHeadersToSend.clone());

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Header block fragments must be sent contiguously: headers and corresponding continuation frames cannot be interleaved with different
     * frames types, or even with different frame types on different streams. This test will:
     * 1. send a header stream 3 without the end of header flag set, which implies that a continuation must follow
     * 2. send a header on stream 5
     * 3. expect a protocol error from the server
     */
    public void testInterleavedHeaderBlocks(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testInterleavedHeaderBlocks";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the GOAWAY / error code that the server should emit following an illegal stream ID order
        byte[] debugData = "Did not receive the expected continuation frame".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        FrameHeaders headers = setupDefaultPreface(h2Client);
        h2Client.addExpectedFrame(headers);

        //Headers frame to send for "second" request
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.waitFor(headers);

        // send out stream 3 headers; end of headers / stream are not set
        frameHeadersToSend.setStreamID(3);
        h2Client.sendFrame(frameHeadersToSend.clone());

        // send out stream 5 headers, before sending stream 3 continuation
        frameHeadersToSend.setStreamID(5);
        h2Client.sendFrame(frameHeadersToSend.clone());

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Set the reserved field on a PING frame: " A reserved 1-bit field. The semantics of this bit are undefined,
     * and the bit MUST remain unset (0x0) when sending and MUST be ignored when receiving."
     */
    public void testSetReservedHeaderField(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testSetReservedHeaderField";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the expected PING frame
        byte[] pingData = "aaaaaaaa".getBytes();
        FramePing pingFrame = new FramePing(0, pingData, false);
        pingFrame.setAckFlag();
        h2Client.addExpectedFrame(pingFrame);

        FrameHeaders headers = setupDefaultPreface(h2Client);
        h2Client.addExpectedFrame(headers);

        // allow first stream to process before sending Ping and ending the test
        h2Client.waitFor(headers);

        // send a PING frame with the reserved bit set; the server should ignore the reserved field
        boolean reserved = true;
        FramePing invalid = new FramePing(0, pingData, reserved);
        h2Client.sendFrame(invalid);

        waitForTestCompletion(blockUntilConnectionIsDone);
        handleErrors(h2Client, testName);
    }

    /**
     * Send a frame with an invalid type, followed by a PING frame; the test will only expect a PING from the server.
     * The rfc (4.1) states that the server should ignore any unknown types.
     */
    public void testUnknownFrameType(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testUnknownFrameType";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // create a PING frame to send to the server
        byte[] pingData = "aaaaaaaa".getBytes();
        FramePing pingFrame = new FramePing(0, pingData, false);
        pingFrame.setAckFlag();
        h2Client.addExpectedFrame(pingFrame);

        setupDefaultPreface(h2Client);

        // send over a PING frame and expect a response
        pingFrame = new FramePing(0, pingData, false);
        h2Client.sendFrame(pingFrame);

        // malformed frame: set frame type byte to unknown
        //_________________________||____________________ - frame type byte
        String dataString = "0000060f0000000003414243313233";
        byte[] b = parseHexBinary(dataString);
        h2Client.sendBytesAfterPreface(b);

        waitForTestCompletion(blockUntilConnectionIsDone);
        handleErrors(h2Client, testName);
    }

    /**
     * Send a DATA frame on stream 0. Stream 0 is the control stream; DATA, HEADERS, CONTINUATION, and PUSH_PROMISE are not allowed on it
     */
    public void testDataOnStreamZero(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testDataOnStreamZero";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the expected error frame
        byte[] debugData = "DATA frame stream ID cannot be 0x0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        FrameHeaders headers = setupDefaultPreface(h2Client);
        h2Client.addExpectedFrame(headers);

        // send a DATA frame with an ID of 0, for which we'll expect the error frame back from the server
        FrameData invalid = new FrameData(0, "test".getBytes(), 0, false, false, false);

        // wait to give the first stream time to complete in order to have the last stream set correctly in GoAway
        h2Client.waitFor(headers);

        h2Client.sendFrame(invalid);

        waitForTestCompletion(blockUntilConnectionIsDone);
        handleErrors(h2Client, testName);
    }

    // This does the same thing as DatatypeConverter.parseHexBinary(str), but it allows us to avoid a dependency on JAX-B for this FAT
    public static byte[] parseHexBinary(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                                  + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }
}
