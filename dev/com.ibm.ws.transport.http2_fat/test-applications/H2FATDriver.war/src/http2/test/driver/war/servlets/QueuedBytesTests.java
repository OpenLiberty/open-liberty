/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for maxQueuedBytes rate limiting behaviors.
 * 
 * The maxQueuedBytes default limit is 2 MB (2 * 1024 * 1024 = 2,097,152 bytes).
 * When the server tries to send response data that exceeds this limit due to
 * flow control blocking, the server should close the connection.
 */
@WebServlet(urlPatterns = "/QueuedBytesTests")
public class QueuedBytesTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Test that when the server tries to send a response larger than maxQueuedBytes
     * on a single stream with a tiny window, the connection is closed.
     */
    public void testExceedMaxQueuedBytesOnSingleStream(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testExceedMaxQueuedBytesOnSingleStream";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] debugData = "Total queued bytes across all streams exceeded limit!".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 3, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 1 byte to block server writes and force queuing
        FrameSettings tinyWindowSettings = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, tinyWindowSettings);

        // Create headers to request a 15 MB response
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?size=15"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        
        // Send request on stream 3
        FrameHeadersClient frameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderEntries(headersToSend);
        h2Client.sendFrame(frameHeaders);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test that when the server tries to send responses larger than maxQueuedBytes
     * across multiple streams with tiny windows, the connection is closed.
     */
    public void testExceedMaxQueuedBytesAcrossMultipleStreams(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testExceedMaxQueuedBytesAcrossMultipleStreams";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] debugData = "Total queued bytes across all streams exceeded limit!".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 11, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 1 byte to block server writes
        FrameSettings tinyWindowSettings = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, tinyWindowSettings);

        // Create headers for requests
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?size=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        // Make 5 requests, each for a 1 MB response (total 5 MB across streams)
        // Server will try to queue all of this and exceed the 2 MB limit
        for (int streamId = 3; streamId <= 11; streamId += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(streamId, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test that with a normal window size, large responses flow normally without
     * triggering the maxQueuedBytes limit.
     */
    public void testNormalWindowSizeDoesNotTriggerLimit(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testNormalWindowSizeDoesNotTriggerLimit";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> expectedHeaders = new ArrayList<H2HeaderField>();
        expectedHeaders.add(new H2HeaderField(":status", "200"));
        FrameHeadersClient expectedResponse = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        expectedResponse.setHeaderFields(expectedHeaders);
        h2Client.addExpectedFrame(expectedResponse);

        // Use default settings (normal window size) - no custom settings
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI);

        // Create headers to request a 33kb response
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=33"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        // Send request on stream 3
        FrameHeadersClient frameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderEntries(headersToSend);
        h2Client.sendFrame(frameHeaders);
        // Wait for the response
        blockUntilConnectionIsDone.await();        
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test boundary condition: server response just under maxQueuedBytes should not trigger error.
     */
    public void testQueuedBytesJustBelowLimit(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testQueuedBytesJustBelowLimit";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameGoAway errorFrame = new FrameGoAway(0, null, LowWindowStreamTests.NO_ERROR, 3, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 1 byte
        FrameSettings tinyWindowSettings = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, tinyWindowSettings);

        // Create headers to request a 1 MB response (below the 2 MB limit)
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?size=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        
        // Send request on stream 3
        FrameHeadersClient frameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderEntries(headersToSend);
        h2Client.sendFrame(frameHeaders);

        // Give it time to queue and the server to respond
        Thread.sleep(5000);

        // Verify that the server did NOT send an error GOAWAY
        if (h2Client.receivedGoAway()) {
            throw new Exception(testName + " FAILED: Server unexpectedly sent a GOAWAY frame");
        }

        // Send a GOAWAY to close the connection gracefully
        h2Client.sendFrame(new FrameGoAway(0, new byte[] { (byte) 0, (byte) 1 }, 0, 1, false));
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test exact boundary: server response at exactly maxQueuedBytes.
     */
    public void testExactlyAtMaxQueuedBytes(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testExactlyAtMaxQueuedBytes";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameGoAway errorFrame = new FrameGoAway(0, null, LowWindowStreamTests.NO_ERROR, 3, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 1 byte
        FrameSettings tinyWindowSettings = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, tinyWindowSettings);

        // Create headers to request exactly 2 MB
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?size=2"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        
        // Send request on stream 3
        FrameHeadersClient frameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderEntries(headersToSend);
        h2Client.sendFrame(frameHeaders);

        // Give it time to queue
        Thread.sleep(5000);

        // Verify that the server did NOT send an error GOAWAY
        if (h2Client.receivedGoAway()) {
            throw new Exception(testName + " FAILED: Server unexpectedly sent a GOAWAY frame");
        }
        // Send a GOAWAY to close the connection gracefully
        h2Client.sendFrame(new FrameGoAway(0, new byte[] { (byte) 0, (byte) 1 }, 0, 1, false));
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test that when a write timeout occurs with pending data, the queued bytes
     * counter is NOT decremented. The bytes should remain tracked until the
     * connection/stream is properly closed.
     */
    public void testWriteTimeoutKeepsQueuedBytesTracked(HttpServletRequest request, 
                                                        HttpServletResponse response) 
                                                        throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testWriteTimeoutKeepsQueuedBytesTracked";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Expect RST_STREAM on stream 3 due to write timeout
        FrameRstStream rstFrame = new FrameRstStream(3, FLOW_CONTROL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        // Expect GOAWAY when trying to queue more data (bytes still tracked from timeout)
        byte[] debugData = "Total queued bytes across all streams exceeded limit!".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 5, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 1 byte to block server writes
        FrameSettings tinyWindowSettings = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, tinyWindowSettings);

        // Request 1: 1 MB response that will timeout
        List<HeaderEntry> headers1 = new ArrayList<HeaderEntry>();
        headers1.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headers1.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headers1.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?size=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        
        FrameHeadersClient frameHeaders1 = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders1.setHeaderEntries(headers1);
        h2Client.sendFrame(frameHeaders1);

        // Wait for write timeout to occur and reset to be sent
        h2Client.waitFor(rstFrame);

        // Request 2: 2 MB response
        // This should trigger GOAWAY because the 1 MB from the timed-out stream
        // is still counted, and 1 MB + 2 MB > 2 MB limit
        List<HeaderEntry> headers2 = new ArrayList<HeaderEntry>();
        headers2.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headers2.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headers2.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?size=2"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        
        FrameHeadersClient frameHeaders2 = new FrameHeadersClient(5, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders2.setHeaderEntries(headers2);
        h2Client.sendFrame(frameHeaders2);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

}
