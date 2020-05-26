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

        byte[] debugData = "DATA Frame Received in the wrong state of: IDLE".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        FrameHeaders headers = setupDefaultPreface(h2Client);

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

        FrameHeadersClient secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        // Initialize connection after adding expected frames
        setupDefaultPreface(h2Client);

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
        byte[] debugData = "Error processing the payload for DATA frame on stream 5".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        FrameHeaders headers = setupDefaultPreface(h2Client);

        h2Client.addExpectedFrame(headers);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(5, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        byte[] dataBytes = hexStringToByteArray("0000050009000000050654657374");

        h2Client.waitFor(headers);
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

        int ERROR_CODE = 0x6; // FRAME_SIZE_ERROR
        byte[] debugData = "DATA payload greater than allowed by the max frame size".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ERROR_CODE, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        FrameHeaders headers = setupDefaultPreface(h2Client);
        h2Client.addExpectedFrame(headers);

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

        h2Client.waitFor(headers);
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

        FrameHeaders headers = setupDefaultPreface(h2Client);
        h2Client.addExpectedFrame(headers);

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

        h2Client.waitFor(headers);
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame);

        // now send EOS
        dataFrame = new FrameData(3, "".getBytes(), 0, true, true, false);
        h2Client.sendFrame(dataFrame);

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
