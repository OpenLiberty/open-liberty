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
import java.util.concurrent.TimeUnit;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for low window stream rate limiting behaviors
 */
@WebServlet(urlPatterns = "/LowWindowStreamTests")
public class LowWindowStreamTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;
    public static final int NO_ERROR = 0x0;

    /**
     * Test that opening more than maxLowWindowStreams streams with initial window size
     * at or below lowWindowLimit causes the server to send a GOAWAY frame
     * with error code ENHANCE_YOUR_CALM (0xb).
     */
    public void testTooManyLowWindowStreams(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testTooManyLowWindowStreams";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Expect a GOAWAY frame with ENHANCE_YOUR_CALM error code after exceeding the limit
        byte[] debugData = "Too many streams with low initial window size have been opened; closing the connection".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 41, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 1024 bytes (at the lowWindowLimit threshold)
        // This will cause all new streams to be counted as "low window" streams
        FrameSettings lowWindowSettings = new FrameSettings(0, -1, -1, -1, 1024, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, lowWindowSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=2"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        // Open 101 streams (stream 1 is the upgrade, so we start at stream 3)
        // The 101st stream should trigger the GOAWAY
        for (int i = 3; i <= 203; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test that opening streams with initial window size above lowWindowLimit
     * does NOT trigger the rate limiting, even if we open many streams.
     * 
     * This verifies that the rate limiting only applies to low window streams.
     */
    public void testManyHighWindowStreams(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testManyHighWindowStreams";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        int lastStreamId = 203;
        List<H2HeaderField> expectedHeaders = new ArrayList<H2HeaderField>();
        expectedHeaders.add(new H2HeaderField(":status", "200"));
        // Expect responses from all streams
        for (int i = 3; i <= lastStreamId; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderFields(expectedHeaders);
            h2Client.addExpectedFrame(frameHeaders);
        }
        // Set initial window size to above the lowWindowLimit threshold
        // These streams should NOT be counted as "low window" streams
        FrameSettings highWindowSettings = new FrameSettings(0, -1, -1, -1, 32768, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, highWindowSettings);
        // Open streams with high window size
        for (int i = 3; i <= lastStreamId; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test that opening exactly maxLowWindowStreams streams with low initial window size
     * does NOT trigger the GOAWAY.
     * 
     * This verifies the boundary condition of the rate limiting.
     */
    public void testExactlyMaxLowWindowStreams(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testExactlyMaxLowWindowStreams";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        int lastStreamId = 41;
        FrameGoAway errorFrame = new FrameGoAway(0, null, NO_ERROR, lastStreamId, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to 512 bytes (well below the lowWindowLimit threshold)
        FrameSettings lowWindowSettings = new FrameSettings(0, -1, -1, -1, 512, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, lowWindowSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        // This should NOT trigger the GOAWAY
        for (int i = 3; i <= lastStreamId; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

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
     * Test mixed scenario: some streams with low window size and some with high window size.
     * Only the low window streams should be counted toward the limit.
     */
    public void testGoAwayMixedWindowSizes(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testGoAwayMixedWindowSizes";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] debugData = "Too many streams with low initial window size have been opened; closing the connection".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 43, false);
        h2Client.addExpectedFrame(errorFrame);

        // Start with low window size
        FrameSettings lowWindowSettings = new FrameSettings(0, -1, -1, -1, 512, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, lowWindowSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        HeaderEntry pathHeader = new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=33"), HpackConstants.LiteralIndexType.NEVERINDEX, false);
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(pathHeader);

        // Open 10 streams that will queue data
        for (int i = 3; i <= 21; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        // Wait for server to handle streams
        Thread.sleep(2000);

        // Change to high window size so that previous streams are able to write but not all data
        FrameSettings highWindowSettings = new FrameSettings(0, -1, -1, -1, 32768, -1, -1, false);
        h2Client.sendFrame(highWindowSettings);

        // Open 11 more streams with high window size - these should NOT count but should also not finish
        for (int i = 23; i <= 43; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        // Give it time to queue and the server to respond
        Thread.sleep(2000);
        // Verify that the server did NOT send an error GOAWAY
        if (h2Client.receivedGoAway()) {
            throw new Exception(testName + " FAILED: Server unexpectedly sent a GOAWAY frame");
        }
        // Change back to low window size which should count previous unfinished streams and fail
        h2Client.sendFrame(lowWindowSettings);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test mixed scenario: some streams with low window size and some with high window size.
     * Only the low window streams should be counted toward the limit.
     */
    public void testStreamLimitDecreasesOnceDataIsWritten(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testStreamLimitDecreasesOnceDataIsWritten";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        int lastStreamId = 61;
        FrameGoAway errorFrame = new FrameGoAway(0, null, NO_ERROR, lastStreamId, false);
        h2Client.addExpectedFrame(errorFrame);

        // Start with low window size
        FrameSettings lowWindowSettings = new FrameSettings(0, -1, -1, -1, 512, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, lowWindowSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        HeaderEntry pathHeader = new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false);
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(pathHeader);

        // Open 10 streams that will queue data
        for (int i = 3; i <= 21; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        // Wait for server to handle streams
        Thread.sleep(2000);

        // Change to high window size so that previous streams are able to finish
        FrameSettings highWindowSettings = new FrameSettings(0, -1, -1, -1, 2048, -1, -1, false);
        h2Client.sendFrame(highWindowSettings);

        // Set response to be 3KB for queueing writes
        pathHeader.setH2HeaderField(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=3"));

        // Open 10 more streams with high window size - these should NOT count but should also not finish
        for (int i = 23; i <= 41; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        // Give it time to queue and the server to respond
        Thread.sleep(2000);
        // Change back to low window size which should count previous unfinished streams
        h2Client.sendFrame(lowWindowSettings);

        // Open 10 more streams with low window size which should count towards limit
        for (int i = 43; i <= lastStreamId; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

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
     * Test that opening low window streams after changing from high to low window size
     * triggers the GOAWAY. This verifies the limit is enforced even when window size changes.
     */
    public void testExceedLimitAfterWindowSizeDecrease(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testExceedLimitAfterWindowSizeDecrease";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Expect a GOAWAY frame with ENHANCE_YOUR_CALM error code
        byte[] debugData = "Too many streams with low initial window size have been opened; closing the connection".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 101, false);
        h2Client.addExpectedFrame(errorFrame);

        // Start with high window size
        FrameSettings highWindowSettings = new FrameSettings(0, -1, -1, -1, 32768, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, highWindowSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=33"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        // Open 50 streams with high window size - these should NOT count but should queue
        for (int i = 3; i <= 101; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        Thread.sleep(2000);
        // Send SETTINGS frame to decrease window size to 512 bytes (below threshold)
        FrameSettings lowWindowSettings = new FrameSettings(0, -1, -1, -1, 512, -1, -1, false);
        h2Client.sendFrame(lowWindowSettings);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test boundary condition: window size exactly at the lowWindowLimit threshold.
     * Streams with window size = lowWindowLimit should be counted as low window streams.
     */
    public void testWindowSizeAtThreshold(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testWindowSizeAtThreshold";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Expect a GOAWAY frame with ENHANCE_YOUR_CALM error code
        byte[] debugData = "Too many streams with low initial window size have been opened; closing the connection".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, ENHANCE_YOUR_CALM_ERROR, 41, false);
        h2Client.addExpectedFrame(errorFrame);

        // Set initial window size to exactly 1024 bytes (at the threshold)
        FrameSettings thresholdWindowSettings = new FrameSettings(0, -1, -1, -1, 1024, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, thresholdWindowSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=2"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        // Open 101 streams with window size = 1024 - should trigger GOAWAY
        for (int i = 3; i <= 203; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Test boundary condition: window size just above the lowWindowLimit threshold
     * Streams with window size > lowWindowLimit should NOT be counted as low window streams.
     */
    public void testWindowSizeJustAboveThreshold(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testWindowSizeJustAboveThreshold";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        int lastStreamId = 203;
        List<H2HeaderField> expectedHeaders = new ArrayList<H2HeaderField>();
        expectedHeaders.add(new H2HeaderField(":status", "200"));
        // Expect responses from all streams
        for (int i = 3; i <= lastStreamId; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderFields(expectedHeaders);
            h2Client.addExpectedFrame(frameHeaders);
        }

        // Set initial window size to just above the threshold
        FrameSettings aboveThresholdSettings = new FrameSettings(0, -1, -1, -1, 16385, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, aboveThresholdSettings);

        // Create headers to send for each stream
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        // Open streams which should NOT trigger GOAWAY
        for (int i = 3; i <= lastStreamId; i += 2) {
            FrameHeadersClient frameHeaders = new FrameHeadersClient(i, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeaders.setHeaderEntries(headersToSend);
            h2Client.sendFrame(frameHeaders);
        }

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }
}
