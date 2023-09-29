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
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameContinuationClient;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for http2 continuation frame behaviors
 */
@WebServlet(urlPatterns = "/ContinuationFrameTests", asyncSupported = true)
public class ContinuationFrameTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Send a continuation frame on a stream after the end headers flag has already been sent. Server should respond with a
     * protocol error
     */
    public void testContFrameAfterHeaderEndHeadersSet(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testContinuationFrameAfterEndHeadersSet";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the GOAWAY we expect after sending a continuation incorrectly
        byte[] chfwDebugData = "CONTINUATION Frame Received when not in a Continuation State".getBytes();
        byte[] nettyDebugData = "Received 9 frame but not currently processing headers.".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, PROTOCOL_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultUpgradedConnection(h2Client);

        // create headers to send over to the server; note that the end headers flag IS set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the continuation frame to send over to the server
        List<H2HeaderField> continuationHeadersToSend = new ArrayList<H2HeaderField>();
        continuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient continuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        continuationHeaders.setHeaderFields(continuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(continuationHeaders);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a continuation frame on a stream after the end headers flag has already been sent. Server should respond with a
     * protocol error
     */
    public void testContFrameAfterContEndHeadersSet(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testContFrameAfterContEndHeadersSet";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the GOAWAY we expect after sending a continuation incorrectly
        byte[] chfwDebugData = "CONTINUATION Frame Received when not in a Continuation State".getBytes();
        byte[] nettyDebugData = "Received 9 frame but not currently processing headers.".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, PROTOCOL_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultUpgradedConnection(h2Client);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField(":path", HEADERS_AND_BODY_URI));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // create the second continuation frame to illegally send over to the server
        List<H2HeaderField> lastContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        lastContinuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient lastContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        lastContinuationHeaders.setHeaderFields(lastContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);
        h2Client.sendFrame(lastContinuationHeaders);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a continuation frame on a stream after the end headers flag AND a data frame have been sent. Expect a
     * protocol error from the server.
     */
    public void testContFrameAfterDataSent(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testContFrameAfterDataSent";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // add the GOAWAY we expect after sending a continuation incorrectly
        byte[] chfwDebugData = "CONTINUATION Frame Received when not in a Continuation State".getBytes();
        byte[] nettyDebugData = "Received 9 frame but not currently processing headers.".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, PROTOCOL_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        setupDefaultUpgradedConnection(h2Client);

        // create headers to send over to the server; note that end_headers IS set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField(":path", HEADERS_AND_BODY_URI));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // send over the header frames followed by the data and continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(new FrameData(3, "derp".getBytes(), false));
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }
}
