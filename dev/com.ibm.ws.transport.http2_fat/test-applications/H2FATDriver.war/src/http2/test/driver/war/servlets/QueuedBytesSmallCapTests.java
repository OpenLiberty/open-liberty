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

import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.frames.FrameWindowUpdate;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

/**
 * Test servlet for maxQueuedBytes behaviours that need a SMALL cap.
 *
 * These tests run against the "flowcontrol.smallcap" server, which sets maxQueuedBytes=262144 (256 KB).
 * They are kept apart from {@link QueuedBytesTests}, whose tests are sized around the 2 MB default.
 */
@WebServlet(urlPatterns = "/QueuedBytesSmallCapTests")
public class QueuedBytesSmallCapTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;

    /**
     * F1: a response much larger than maxQueuedBytes is sent on a wide-open window. Nothing is ever blocked by
     * flow control, so nothing is parked and the cap must not be hit. A server that counts bytes that are
     * merely in flight (written but not yet flushed to the socket) as "queued" can reach the cap here and
     * wrongly send GOAWAY(ENHANCE_YOUR_CALM).
     *
     * Expected: 200 and all the DATA arrive; no GOAWAY.
     */
    public void testLargeResponseOnOpenWindowDoesNotTriggerSmallCap(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testLargeResponseOnOpenWindowDoesNotTriggerSmallCap";

        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        List<H2HeaderField> expectedHeaders = new ArrayList<H2HeaderField>();
        expectedHeaders.add(new H2HeaderField(":status", "200"));
        FrameHeadersClient expectedResponse;
        if (USING_NETTY)
            expectedResponse = new FrameHeadersClient(3, null, 0, 0, 15, false, true, false, true, false, false);
        else
            expectedResponse = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        expectedResponse.setHeaderFields(expectedHeaders);
        h2Client.addExpectedFrame(expectedResponse);

        // 10 MB stream window via SETTINGS, plus 10 MB on the connection window
        FrameSettings largeWindowSettings = new FrameSettings(0, -1, -1, -1, 10000000, -1, -1, false);
        setupDefaultUpgradedConnection(h2Client, HEADERS_ONLY_URI, largeWindowSettings);
        h2Client.sendFrame(new FrameWindowUpdate(0, 10000000, false));

        // 2500 KB is about 10x the 256 KB cap
        List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
        headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        headersToSend.add(new HeaderEntry(new H2HeaderField(":path", LARGE_RESPONSE_URI + "?sizeKB=2500"), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderEntries(headersToSend);
        h2Client.sendFrame(frameHeaders);

        blockUntilConnectionIsDone.await();
        this.handleErrors(h2Client, testName);
    }


}
