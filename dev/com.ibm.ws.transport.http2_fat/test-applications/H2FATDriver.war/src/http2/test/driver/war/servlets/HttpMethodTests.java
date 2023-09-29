/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
package http2.test.driver.war.servlets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for http2 continuation frame behaviors
 */
@WebServlet(urlPatterns = "/HttpMethodTests", asyncSupported = true)
public class HttpMethodTests extends H2FATDriverServlet {

    /**
     * Send a header with CONNECT set
     */
    public void testConnectMethod(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testConnectMethod";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 15, false, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        // add an expected PING frame
        byte[] pingData = "aaaaaaaa".getBytes();
        FramePing pingFrame = new FramePing(0, pingData, false);
        pingFrame.setAckFlag();
        h2Client.addExpectedFrame(pingFrame);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "CONNECT"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":authority", request.getServerName() + ":"
                                                                               + request.getServerPort()), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        String dataString = "test";
        FrameData dataFrame1 = new FrameData(3, dataString.getBytes(), 0, false, false, false);
        FrameData dataFrame2 = new FrameData(3, dataString.getBytes(), 0, false, false, false);
        FrameData dataFrame3 = new FrameData(3, dataString.getBytes(), 0, false, false, false);
        FrameData dataFrame4 = new FrameData(3, dataString.getBytes(), 0, true, false, false);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame1);
        h2Client.sendFrame(dataFrame2);
        h2Client.sendFrame(dataFrame3);
        h2Client.sendFrame(dataFrame4);

        // So that the test doesn't end prematurely when the headers are received, send a ping and
        // expect a response.
        // send over a PING frame and expect a response
        pingFrame = new FramePing(0, pingData, false);
        h2Client.sendFrame(pingFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a header with CONNECT set along with a disallowed pseudo-header. Then immediately send some DATA frames.
     * Expect an RST_STREAM on the erroneous thread, an no GOAWAY despite sending DATA frames after the RST_STREAM is received
     */
    public void testConnectMethodError(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testConnectMethodError";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        // Allow frames after end of stream due to lack of timing/answer control
        h2Client.allowFramesAfterEndOfStream();
        FrameRstStream errorFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(errorFrame);

        // add an expected PING frame
        byte[] pingData = "aaaaaaaa".getBytes();
        FramePing pingFrame = new FramePing(0, pingData, false);
        pingFrame.setAckFlag();
        h2Client.addExpectedFrame(pingFrame);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "CONNECT"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":authority", request.getServerName() + ":"
                                                                               + request.getServerPort()), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        String dataString = "test";
        FrameData dataFrame1 = new FrameData(3, dataString.getBytes(), 0, false, false, false);
        FrameData dataFrame2 = new FrameData(3, dataString.getBytes(), 0, false, false, false);
        FrameData dataFrame3 = new FrameData(3, dataString.getBytes(), 0, false, false, false);
        FrameData dataFrame4 = new FrameData(3, dataString.getBytes(), 0, true, false, false);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame1);
        h2Client.sendFrame(dataFrame2);
        h2Client.sendFrame(dataFrame3);
        h2Client.sendFrame(dataFrame4);

        // So that the test doesn't end prematurely when the reset is received, send a ping and
        // expect a response.
        // send over a PING frame and expect a response
        FramePing sendPingFrame = new FramePing(0, pingData, false);
        h2Client.sendFrame(sendPingFrame);

        h2Client.waitFor(pingFrame);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a header with the HEAD method set. Expect back one header with a content-length set, and no body
     */
    public void testHeadMethod(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testHeadMethod";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        secondHeadersReceived.add(new H2HeaderField("content-length", "6"));
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 15, true, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "HEAD"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a header with the Option method set and a valid servlet path set for :path
     */
    public void testOptionMethod(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testOptionMethod";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        secondHeadersReceived.add(new H2HeaderField("content-length", "0"));
        secondHeadersReceived.add(new H2HeaderField("allow", "GET, HEAD, POST, TRACE, OPTIONS"));
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 15, true, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        setupDefaultUpgradedConnection(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "OPTIONS"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a header with the Option method set and an invalid path. Expect an RST_STREAM in response.
     *
     */
    public void testOptionMethod400Uri(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testOptionMethod400Uri";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // set up the header we expect to receive
        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "400"));
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 15, false, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        setupDefaultUpgradedConnection(h2Client);

        // set up the first headers to send out
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "OPTIONS"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", "invalid_path"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }

    /**
     * Send a header with the Option method set and an invalid path. Expect a 404 response in return.
     *
     */
    public void testOptionMethod404Uri(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testOptionMethod404Uri";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // set up the header we expect to receive
        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "404"));
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 15, false, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        setupDefaultUpgradedConnection(h2Client);

        // set up the first headers to send out
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "OPTIONS"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", "/invalid_path/"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }
}
