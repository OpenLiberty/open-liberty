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

import static http2.test.driver.war.servlets.GenericFrameTests.parseHexBinary;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;

import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.frames.FramePriority;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.frames.FrameWindowUpdate;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameContinuationClient;
import com.ibm.ws.http2.test.frames.FrameDataClient;
import com.ibm.ws.http2.test.frames.FrameGoAwayClient;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.frames.FramePushPromiseClient;
import com.ibm.ws.http2.test.helpers.HTTPUtils;
import com.ibm.ws.http2.test.helpers.HeaderEntry;

import componenttest.app.FATServlet;
import test.server.transport.http2.Utils;

/*
 *
 * /MyTestServlet?bucket=lite&test=test1 <- run test1 from the lite bucket
 * /MyTestServlet?test=simpleTest <- run predefined (hardwired) test named simpleTest
 * /MyTestServlet?bucket=lite   <- run the lite bucket
 *
 */
@WebServlet(urlPatterns = "/H2FATDriverServlet", asyncSupported = true)
public class H2FATDriverServlet extends FATServlet {
    /**  */
    private static final long serialVersionUID = 1L;
    protected final long defaultTimeoutToSendFrame = 10000L;

    private static final Logger LOGGER = Logger.getLogger(H2FATDriverServlet.class.getName());

    protected static final String HEADERS_ONLY_URI = "/H2TestModule/H2HeadersOnly";
    protected static final String HEADERS_AND_BODY_URI = "/H2TestModule/H2HeadersAndBody";
    private static final String SERVLET_H2MultiDataFrame = "/H2TestModule/H2MultiDataFrame";
    private static final String SERVLET_H2PriorityWindowUpdate1 = "/H2TestModule/H2PriorityWindowUpdate1";
    private static final String SERVLET_H2Ping1 = "/H2TestModule/H2PriorityWindowUpdate1?testName=Ping1";
    private static final String SERVLET_CONTINUATION = "/H2TestModule/HeadersAndContinuation";

    public static final FrameSettings EMPTY_SETTINGS_FRAME = new FrameSettings();
    public static final FrameSettings DEFAULT_SERVER_SETTINGS_FRAME = new FrameSettings(0, -1, -1, 200, -1, 57344, -1, false);

    protected final int PROTOCOL_ERROR = 0x1;
    protected final int FLOW_CONTROL_ERROR = 0x3;
    protected final int STREAM_CLOSED = 0x5;
    protected final int FRAME_SIZE_ERROR = 0x6;
    protected final int CANCEL_ERROR = 0x8;
    protected final int COMPRESSION_ERROR = 0x9;
    protected final int REFUSED_STREAM_ERROR = 0x7;

    public void testUpgradeHeader(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testUpgradeHeader";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPreface(defaultTimeoutToSendFrame);

        //"kill" connection (it just calls blockUntilConnectionIsDonecountDown() for now)
        h2Client.sendFrame(new FrameGoAway(0, new byte[] { (byte) 0, (byte) 1 }, 0, 1, false));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testUpgradeHeaderFollowedBySettingsFrame(HttpServletRequest request,
                                                         HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUpgradeHeaderFollowedBySettingsFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUpgradeHeaderFollowedBySettingsFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testUpgradeHeaderFollowedBySettingsFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderAndData(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testHeaderAndData";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(1, dataString.getBytes(), 0, false, false, false));
        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testMultiData(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {

        long startTime = System.currentTimeMillis();
        System.out.println("Test start at: " + startTime);

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testMultiData";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "request: " + request);
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "hostName: " + request.getParameter("hostName"));
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "port: " + request.getParameter("port"));
        }
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, Utils.STRESS_TEST_TIMEOUT_testMultipleConnectionStress);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        for (int i = 1; i <= Utils.STREAM_INSTANCES; i++) {
            int sID = (i * 2) - 1;
            String s = "LAST.DATA.FRAME";
            h2Client.addExpectedFrame(new FrameData(sID, s.getBytes(), 0, false, false, false));
        }

        h2Client.sendUpgradeHeader(SERVLET_H2MultiDataFrame);

        //Since this is a conditional send, this will block the thread until the preface is sent.
        //If the this fails, the test needs to fail as well because the H2 protocol was not established successfully.
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        int weight = Utils.FIRST_STREAM_WEIGHT;
        boolean makeDependent = false;
        boolean makeExclusive = false;
        int depNode = 0;
        boolean excNode = false;

        // send window update for the connection (stream 0)
        // Use a smaller value to drive FlowControlExceptions during stress testing
        // FrameWindowUpdate windowGood = new FrameWindowUpdate(0, 32000, false);
        FrameWindowUpdate windowGood = new FrameWindowUpdate(0, Utils.STRESS_CONNECTION_WINDOW_UPDATE, false);
        h2Client.sendFrame(windowGood);

        // send window update for stream 1
        windowGood = new FrameWindowUpdate(1, Utils.STRESS_STREAM_WINDOW_UPDATE_START, false);
        h2Client.sendFrame(windowGood);

        for (int i = 2; i <= Utils.STREAM_INSTANCES; i++) {
            List<HeaderEntry> headersToSend = new ArrayList<HeaderEntry>();
            headersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
            headersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
            headersToSend.add(new HeaderEntry(new H2HeaderField(":path", SERVLET_H2MultiDataFrame), HpackConstants.LiteralIndexType.NEVERINDEX, false));

            FrameHeadersClient frameHeadersToSend = new FrameHeadersClient((i * 2) - 1, null, 0, 0, 0, true, true, false, false, false, false);

            frameHeadersToSend.setHeaderEntries(headersToSend);
            frameHeadersToSend.setStreamID((i * 2) - 1);
            h2Client.sendFrame(frameHeadersToSend);

            try {

                depNode = 0;
                if (makeDependent) {
                    if (makeExclusive) {
                        // make this stream dependent and exclusive on the one that came two before it.
                        depNode = (i * 2) - 5;
                        excNode = true;
                        makeExclusive = false;
                        makeDependent = false;
                    } else {
                        // make this stream dependent on the one that came before it.
                        depNode = (i * 2) - 3;
                        excNode = false;
                        // set for next time around
                        makeExclusive = true;
                    }
                } else {
                    // set for next time around
                    excNode = false;
                    makeDependent = true;
                    makeExclusive = false;
                }

                FramePriority fp = new FramePriority((i * 2) - 1, depNode, weight, excNode, false);
                h2Client.sendBytes(fp.buildFrameForWrite());

                // send window update for new stream
                windowGood = new FrameWindowUpdate((i * 2) - 1, Utils.STRESS_STREAM_WINDOW_UPDATE_START, false);
                h2Client.sendFrame(windowGood);

                Thread.sleep(Utils.STRESS_DELAY_BETWEEN_STREAM_STARTS);

            } catch (Exception x) {

                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.logp(Level.INFO, this.getClass().getName(), "testMultiData", "caught exception: " + x);
                }
            }

            weight += Utils.WEIGHT_INCREMENT_PER_STREAM;
            if (weight > 255) {
                weight = Utils.FIRST_STREAM_WEIGHT;
            }
        }

        //Use CountDownLatch to block this test thread until we know the test is done (meaning, the connection has been closed)
        blockUntilConnectionIsDone.await();

        handleErrors(h2Client, testName);
    }

    public void testPriorityWindowUpdate1(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testPriorityWindowUpdate1";
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone,
                        //defaultTimeoutToSendFrame);
                        30000);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        String dataString1 = "ABC123";
        String dataString2 = "LAST.DATA.FRAME";
        h2Client.addExpectedFrame(new FrameData(1, dataString1.getBytes(), 0, false, false, false));
        h2Client.addExpectedFrame(new FrameData(1, dataString2.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(SERVLET_H2PriorityWindowUpdate1);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPing1(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testPing1";
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, 30000); //defaultTimeoutToSendFrame);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        String dataString1 = "ABC123";
        String dataString2 = "LAST.DATA.FRAME";
        h2Client.addExpectedFrame(new FrameData(1, dataString1.getBytes(), 0, false, false, false));
        h2Client.addExpectedFrame(new FrameData(1, dataString2.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(SERVLET_H2Ping1);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderAndDataPost(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testHeaderAndDataPost";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(1, dataString.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI, HTTPUtils.HTTPMethod.POST);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSecondRequest(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testSecondRequest";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    /**
     * Set the window size to be very small; make sure server waits to send over new frames until a window_update is sent
     */
    public void testSmallWindowSize(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testSmallWindowSize";
        try {
            Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

            h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

            String dataString = "ABC123";

            List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();

            //Expected headers for the first (upgrade) request
            firstHeadersReceived.add(new H2HeaderField(":status", "200"));
            firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
            firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
            // cannot assume language of test machine
            firstHeadersReceived.add(new H2HeaderField("content-language", ".*"));
            FrameHeadersClient frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
            frameHeaders.setHeaderFields(firstHeadersReceived);
            h2Client.addExpectedFrame(frameHeaders);

            //Expected headers for the "second" request
            List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
            secondHeadersReceived.add(new H2HeaderField(":status", "200"));
            secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
            secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
            // cannot assume language of test machine
            secondHeadersReceived.add(new H2HeaderField("content-language", ".*"));
            FrameHeadersClient secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
            secondFrameHeaders.setHeaderFields(secondHeadersReceived);
            h2Client.addExpectedFrame(secondFrameHeaders.clone());
            h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

            secondFrameHeaders.setStreamID(5);
            h2Client.addExpectedFrame(secondFrameHeaders.clone());
            h2Client.addExpectedFrame(new FrameData(5, dataString.getBytes(), 0, false, false, false));

            //Headers frame to send for "second" request
            List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
            firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
            firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
            firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
            firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
            FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
            frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

            // start sending out frames
            h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
            FrameSettings settings = new FrameSettings(0, -1, -1, -1, 5, -1, -1, false);
            h2Client.sendClientPrefaceFollowedBySettingsFrame(settings);

            frameHeadersToSend.setStreamID(3);
            h2Client.sendFrame(frameHeadersToSend.clone());

            frameHeadersToSend.setStreamID(5);
            h2Client.sendFrame(frameHeadersToSend.clone());

            // TODO: figure out how to check that stream 5 actually closes before stream 3
            FrameWindowUpdate window = new FrameWindowUpdate(5, 50, false);
            h2Client.sendFrame(window);

            window = new FrameWindowUpdate(3, 50, false);
            h2Client.sendFrame(window);

            blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);

            handleErrors(h2Client, testName);

        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, this.getClass().getName(), "testSmallWindowSize", "Failed to load test: " + e);
            }

            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test sending an RstStream frame
     * In this case, the server will not process the RstStream until after it has sent out responses on the stream
     */
    public void testRstStream(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testRstStream";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);

        // send over an RST_STREAM frame immediately after sending out a header frame on stream 3
        FrameRstStream rstFrame = new FrameRstStream(1, 0, false);
        h2Client.sendFrame(rstFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    /**
     * Set the max frame size to be less than the expected header responds, and make sure that a correct continuation frame is received
     */
    public void testHeaderAndContinuations(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testHeaderAndContinuations";
        try {
            Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
            h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

            //Expected headers for the first (upgrade) request
            h2Client.addExpectedFrame(FrameTypes.HEADERS, 1);

            //Expected headers for the continuation response
            h2Client.addExpectedFrame(FrameTypes.CONTINUATION, 1);

            // start sending out frames
            h2Client.sendUpgradeHeader(SERVLET_CONTINUATION);
            FrameSettings settings = new FrameSettings(0, -1, -1, -1, -1, -1, -1, false);

            h2Client.sendClientPrefaceFollowedBySettingsFrame(settings);

            blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
            handleErrors(h2Client, testName);

        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderAndContinuations", "Failed to load test: " + e);
            }
            Assert.fail(e.getMessage());
        }
    }

    public void testNoPRIFollowedBySettingsFrame(HttpServletRequest request,
                                                 HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUpgradeHeaderFollowedBySettingsFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUpgradeHeaderFollowedBySettingsFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNoPRIFollowedBySettingsFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        // add the GOAWAY / error code that the server should emit following an illegal stream ID order
        byte[] debugData = "Cannot initialize a stream with an ID lower than one previously created".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendFrame(EMPTY_SETTINGS_FRAME, true); //send a setting frames forcedly

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testMangledPRI(HttpServletRequest request,
                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testMangledPRI", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testMangledPRI",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testMangledPRI";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        // add the GOAWAY / error code that the server should emit following an illegal stream ID order
        byte[] debugData = "Cannot initialize a stream with an ID lower than one previously created".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPreface("PRI-Error");

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    //This is just a placeholder to get around the way simplicity works...
    public void testMultipleConnectionStress(HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.testMultiData(request, response);
    }

    //Sending a Priority frame on an idle stream. All streams start in idle state.
    //Receiving any frame other than HEADERS or PRIORITY on a stream in this state MUST be treated as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
    public void testPriorityFrameOnIdleStream(HttpServletRequest request,
                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnIdleStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnIdleStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameOnIdleStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(1, 0, 0, false, false);
        h2Client.sendFrame(priorityFrame);

        //Use CountDownLatch to block this test thread until we know the test is done (meaning, the connection has been closed)
        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testWindowsUpdateFrameOnHalfClosedStream(HttpServletRequest request,
                                                         HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowsUpdateFrameOnHalfClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowsUpdateFrameOnHalfClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testWindowsUpdateFrameOnHalfClosedStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);

        //zeroWindowSizeSettingsFrame will prevent the server from closing the stream we want to test
        FrameSettings zeroWindowSizeSettingsFrame = new FrameSettings(0, -1, -1, -1, 0, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(zeroWindowSizeSettingsFrame);

        //the server should be able to send frames after this windowUpdate is sent
        FrameWindowUpdate windowUpdate = new FrameWindowUpdate(1, 1, false);
        h2Client.sendFrame(windowUpdate);

        blockUntilConnectionIsDone.await();
        handleErrors(h2Client, testName);
    }

    public void testRstStreamFrameOnHalfClosedStream(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameOnHalfClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameOnHalfClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testRstStreamFrameOnHalfClosedStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] emptyBytes = new byte[8];
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);

        //zeroWindowSizeSettingsFrame will prevent the server from closing the stream we want to test
        FrameSettings zeroWindowSizeSettingsFrame = new FrameSettings(0, -1, -1, -1, 0, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(zeroWindowSizeSettingsFrame);

        FrameRstStream rstStreamFrame = new FrameRstStream(1, CANCEL_ERROR, false);
        h2Client.sendFrame(rstStreamFrame);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPriorityFrameOnClosedStream(HttpServletRequest request,
                                                HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameOnClosedStream";
        int streamId = 1;

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] emptyBytes = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //wait until stream one finishes
        h2Client.waitFor(frameHeaders);

        FramePriority priorityFrame = new FramePriority(streamId, 0, 0, false, false);
        h2Client.sendFrame(priorityFrame);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPriorityFrameOnIdlePushStream(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameOnClosedStream";
        int streamId = 1;

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] emptyBytes = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //wait until stream one finishes
        h2Client.waitFor(frameHeaders);

        // Send a priority frame on an idle push stream..  The server should tolerate and ignore this.
        FramePriority priorityFrame = new FramePriority(2, 0, 0, false, false);
        h2Client.sendFrame(priorityFrame);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testContFrameAfterHeadersFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContFrameAfterHeadersFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContFrameAfterHeadersFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testContFrameAfterHeadersFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Add all the expected frames before sending
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, true, false, false));

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<HeaderEntry> firstContinuationHeadersToSend = new ArrayList<HeaderEntry>();
        firstContinuationHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, false, false);
        firstContinuationHeaders.setHeaderEntries(firstContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testTwoContFrameAfterHeadersFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoContFrameAfterHeadersFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoContFrameAfterHeadersFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testTwoContFrameAfterHeadersFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        // Add all the expected frames before sending
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<HeaderEntry> firstContinuationHeadersToSend = new ArrayList<HeaderEntry>();
        firstContinuationHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, false, false, false);
        firstContinuationHeaders.setHeaderEntries(firstContinuationHeadersToSend);

        List<HeaderEntry> secondContinuationHeadersToSend = new ArrayList<HeaderEntry>();
        secondContinuationHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameContinuationClient secondContinuationHeaders = new FrameContinuationClient(3, null, true, false, false);
        secondContinuationHeaders.setHeaderEntries(secondContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);
        h2Client.sendFrame(secondContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testSendHeadersFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadersFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        String dataString = "ABC123";

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendHeaderWithPaddingFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeaderWithPaddingFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeaderWithPaddingFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeaderWithPaddingFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        String dataString = "ABC123";

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, new byte[8], 0, 8, 0, true, true, true, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendHeaderFrameWithPriorityValue(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeaderFrameWithPriorityValue", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeaderFrameWithPriorityValue",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeaderFrameWithPriorityValue";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        String dataString = "ABC123";

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 255, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPriorityFrameWithPriorityOne(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithPriorityOne", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithPriorityOne",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPriorityFrameWithPriorityOne";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(3, 0, 0, false, false);
        h2Client.sendFrame(priorityFrame);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPriorityFrameWithPriority256(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithPriority256", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithPriority256",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPriorityFrameWithPriority256";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(3, 0, 255, false, false);
        h2Client.sendFrame(priorityFrame);

        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPriorityFrameWithStreamDependency(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithStreamDependency", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithStreamDependency",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPriorityFrameWithStreamDependency";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(3, 3 + 2, 255, false, false);
        h2Client.sendFrame(priorityFrame);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPriorityFrameWithExclusiveFlag(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithExclusiveFlag", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameWithExclusiveFlag",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPriorityFrameWithExclusiveFlag";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(3, 0, 0, true, false);
        h2Client.sendFrame(priorityFrame);

        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPriorityFrameOnHigherStreamIdThanHeadersFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameOnHigherStreamIdThanHeadersFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPriorityFrameOnHigherStreamIdThanHeadersFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPriorityFrameOnHigherStreamIdThanHeadersFrame";
        int streamId = 3;

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(streamId, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(streamId + 2, 0, 0, false, false);
        h2Client.sendFrame(priorityFrame);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testRstStreamFrameToCancelStream(HttpServletRequest request,
                                                 HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameToCancelStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameToCancelStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testRstStreamFrameToCancelStream";
        int streamId = 3;
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] emptyBytes = new byte[8];
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(streamId, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameRstStream rstStreamFrame = new FrameRstStream(streamId, CANCEL_ERROR, false);
        h2Client.sendFrame(rstStreamFrame);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithValues(HttpServletRequest request,
                                           HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithValues", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithValues",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithValues";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] emptyBytes = new byte[8];
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameSettings settingsFrameWithValues = new FrameSettings(0, 4096, 1, 100, 65535, 16384, 100, false);
        h2Client.sendFrame(settingsFrameWithValues);

        //send a ping and expect a ping back; this also helps us know if Setting ACK arrived as the PING
        //will not be sent until ACK arrives.
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPingFrame(HttpServletRequest request,
                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPingFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing expectedPing = new FramePing(0, libertyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //send a ping and expect a ping back; this also helps us know if Setting ACK arrived as the PING
        //will not be sent until ACK arrives.
        FramePing ping = new FramePing(0, libertyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testWindowUpdateFrameOnStream0(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowUpdateFrameOnStream0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowUpdateFrameOnStream0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testWindowUpdateFrameOnStream0";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing expectedPing = new FramePing(0, libertyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //the server should be able to send frames after this windowUpdate is sent
        FrameWindowUpdate windowUpdate = new FrameWindowUpdate(0, 1, false);
        h2Client.sendFrame(windowUpdate);

        FramePing ping = new FramePing(0, libertyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testWindowUpdateFrameOnStream3(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowUpdateFrameOnStream3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowUpdateFrameOnStream3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testWindowUpdateFrameOnStream3";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //the server should be able to send frames after this windowUpdate is sent
        FrameWindowUpdate windowUpdate = new FrameWindowUpdate(3, 1, false);
        h2Client.sendFrame(windowUpdate);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendGetRequest(HttpServletRequest request,
                                   HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendGetRequest", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendGetRequest",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendGetRequest";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPostRequest(HttpServletRequest request,
                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPostRequest", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPostRequest",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPostRequest";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //FramePing ping = new FramePing(0, libertyBytes, false);
        //h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPostRequestWithBody(HttpServletRequest request,
                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPostRequestWithBody", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPostRequestWithBody",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPostRequestWithBody";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        secondHeadersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        String testString = "test";
        String s = "Request Body: " + testString +" content-length: " + testString.length();
        FrameDataClient dataFrame = new FrameDataClient(3, s.getBytes(), 0, true, false, false);
        h2Client.addExpectedFrame(dataFrame);


        h2Client.sendUpgradeHeader("/H2TestModule/H2PostEchoBody");
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", "/H2TestModule/H2PostEchoBody"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        dataFrame = new FrameDataClient(3, testString.getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }


    public void testSendHeadRequest(HttpServletRequest request,
                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadRequest", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadRequest",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadRequest";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "HEAD"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //FramePing ping = new FramePing(0, libertyBytes, false);
        //h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendOptionsRequest(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendOptionsRequest", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendOptionsRequest",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendOptionsRequest";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "OPTIONS"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", "*"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendOptionsRequestUrlPath(HttpServletRequest request,
                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendOptionsRequestUrlPath", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendOptionsRequestUrlPath",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendOptionsRequestUrlPath";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "OPTIONS"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testIndexedHeaderField(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedHeaderField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedHeaderField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testIndexedHeaderField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", ""), HpackConstants.LiteralIndexType.INDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testIndexedHeaderFieldNoHuffman(HttpServletRequest request,
                                                HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedHeaderFieldNoHuffman", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedHeaderFieldNoHuffman",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testIndexedHeaderFieldNoHuffman";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", "liberty"), HpackConstants.LiteralIndexType.INDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testIndexedHeaderFieldHuffmanEncoded(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedHeaderFieldHuffmanEncoded", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedHeaderFieldHuffmanEncoded",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testIndexedHeaderFieldHuffmanEncoded";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", "liberty"), HpackConstants.LiteralIndexType.INDEX, true));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testIndexedCustomHeaderField(HttpServletRequest request,
                                             HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedCustomHeaderField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedCustomHeaderField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testIndexedCustomHeaderField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.INDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testIndexedCustomHeaderFieldHuffmanEncoded(HttpServletRequest request,
                                                           HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedCustomHeaderFieldHuffmanEncoded", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testIndexedCustomHeaderFieldHuffmanEncoded",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testIndexedCustomHeaderFieldHuffmanEncoded";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.INDEX, true));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNoIndexHeaderField(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexHeaderField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexHeaderField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNoIndexHeaderField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", "liberty"), HpackConstants.LiteralIndexType.NOINDEXING, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNoIndexHeaderFieldHuffmanEncoded(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexHeaderFieldHuffmanEncoded", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexHeaderFieldHuffmanEncoded",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNoIndexHeaderFieldHuffmanEncoded";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", "liberty"), HpackConstants.LiteralIndexType.NOINDEXING, true));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNoIndexCustomHeaderField(HttpServletRequest request,
                                             HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexCustomHeaderField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexCustomHeaderField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNoIndexCustomHeaderField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NOINDEXING, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNoIndexCustomHeaderFieldHuffmanEncoded(HttpServletRequest request,
                                                           HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexCustomHeaderFieldHuffmanEncoded", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNoIndexCustomHeaderFieldHuffmanEncoded",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNoIndexCustomHeaderFieldHuffmanEncoded";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NOINDEXING, true));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNeverIndexHeaderField(HttpServletRequest request,
                                          HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexHeaderField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexHeaderField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNeverIndexHeaderField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", "liberty"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNeverIndexHeaderFieldHuffmanEncoded(HttpServletRequest request,
                                                        HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexHeaderFieldHuffmanEncoded", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexHeaderFieldHuffmanEncoded",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNeverIndexHeaderFieldHuffmanEncoded";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("user-agent", "liberty"), HpackConstants.LiteralIndexType.NEVERINDEX, true));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNeverIndexCustomHeaderField(HttpServletRequest request,
                                                HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexCustomHeaderField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexCustomHeaderField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNeverIndexCustomHeaderField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNeverIndexCustomHeaderFieldHuffmanEncoded(HttpServletRequest request,
                                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexCustomHeaderFieldHuffmanEncoded", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNeverIndexCustomHeaderFieldHuffmanEncoded",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNeverIndexCustomHeaderFieldHuffmanEncoded";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, true));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDynamicTableSize(HttpServletRequest request,
                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDynamicTableSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDynamicTableSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDynamicTableSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameSettings settingsFrameWithMaxHeaderListSize = new FrameSettings(0, -1, -1, -1, -1, -1, 128, false);
        h2Client.sendFrame(settingsFrameWithMaxHeaderListSize);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDynamicTableSizeChanged(HttpServletRequest request,
                                            HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDynamicTableSizeChanged", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDynamicTableSizeChanged",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDynamicTableSizeChanged";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameSettings settingsFrameWithMaxHeaderListSizeA = new FrameSettings(0, -1, -1, -1, -1, -1, 128, false);
        h2Client.sendFrame(settingsFrameWithMaxHeaderListSizeA);

        FrameSettings settingsFrameWithMaxHeaderListSizeB = new FrameSettings(0, -1, -1, -1, -1, -1, 4096, false);
        h2Client.sendFrame(settingsFrameWithMaxHeaderListSizeB);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testUnknownFrame(HttpServletRequest request,
                                 HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUnknownFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUnknownFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testUnknownFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] emptyBytes = new byte[8];
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //Type 244, lenght 0
        byte[] unknownFrameHeader = { 0, 0, 0, -12, 0, 0, 0, 0, 0 };
        h2Client.sendBytes(unknownFrameHeader);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPingFrameBadFlags(HttpServletRequest request,
                                      HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameBadFlags", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameBadFlags",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPingFrameBadFlags";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing expectedPing = new FramePing(0, libertyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // sendbytes does not wait for the http2 start up sequence to finish, which
        // was causing an intermittent failure when the bad ping frame sometimes got intermixed with
        // the settings frames.  Sendframe does wait, so issue it first, followed by the bad ping.
        // RTC 255368

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, libertyBytes, false);
        h2Client.sendFrame(ping);

        //Type 6, length 0
        byte[] pingFrameBytes = { 0, 0, (byte) 8, (byte) 6, (byte) 255, 0, 0, 0, 0 };
        byte[] payload = { 0, 0, 0, 0, 0, 0, 0, 0 };

        h2Client.sendBytes(pingFrameBytes);
        h2Client.sendBytes(payload);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPingFrameReservedFlag(HttpServletRequest request,
                                          HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameReservedFlag", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameReservedFlag",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPingFrameReservedFlag";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing expectedPing = new FramePing(0, libertyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, libertyBytes, true);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameMaxSize(HttpServletRequest request,
                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameMaxSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameMaxSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameMaxSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);

        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing expectedPing = new FramePing(0, libertyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //16777216 max size frame
        int maxSize = 16777216;
        StringBuilder stringBuilder = new StringBuilder(maxSize);
        while (stringBuilder.toString().getBytes().length < maxSize)
            stringBuilder.append('q');
        FrameDataClient dataFrame = new FrameDataClient(3, stringBuilder.toString().getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        //send a ping and expect a ping back
        FramePing ping = new FramePing(0, libertyBytes, true);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testInvalidHeaderFields(HttpServletRequest request,
                                        HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInvalidHeaderBlock", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInvalidHeaderBlock",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testInvalidHeaderFields";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testInvalidHeaderBlock(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInvalidHeaderBlock", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInvalidHeaderBlock",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testInvalidHeaderBlock";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameGoAway errorFrame = new FrameGoAway(0, "HEADERS frame must have a header block fragment".getBytes(), COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        byte[] errorBytes = "Error".getBytes();
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, errorBytes, 0, 0, 0, true, true, false, false, false, false);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testInvalidStreamId(HttpServletRequest request,
                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInvalidStreamId", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInvalidStreamId",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testInvalidStreamId";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Cannot start a stream from the client with an even numbered ID. stream-id: 2".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        //Even stream id, should trigger error
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(2, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameOnIdleStream(HttpServletRequest request,
                                          HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOnIdleStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOnIdleStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameOnIdleStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "DATA Frame Received in the wrong state of: IDLE".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testRstStreamFrameOnIdleStream(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameOnIdleStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameOnIdleStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testRstStreamFrameOnIdleStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "RST_STREAM Frame Received in the wrong state of: IDLE".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(3, CANCEL_ERROR, false);
        h2Client.sendFrame(rstFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testWindowUpdateFrameOnIdleStream(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowUpdateFrameOnIdleStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testWindowUpdateFrameOnIdleStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testWindowUpdateFrameOnIdleStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "WINDOW_UPDATE Frame Received in the wrong state of: IDLE".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameWindowUpdate windowUpdate = new FrameWindowUpdate(3, 100, false);
        h2Client.sendFrame(windowUpdate);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testContinuationFrameOnIdleStream(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameOnIdleStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameOnIdleStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testContinuationFrameOnIdleStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "CONTINUATION Frame Received in the wrong state of: IDLE".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField(":scheme", "http"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameOnClosedStream(HttpServletRequest request,
                                            HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOnClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOnClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameOnClosedStream";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "DATA frame received on a closed stream".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, STREAM_CLOSED, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        FrameRstStream rstFrame = new FrameRstStream(3, CANCEL_ERROR, false);
        h2Client.sendFrame(rstFrame);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameOnClosedStream(HttpServletRequest request,
                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOnClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOnClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameOnClosedStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "HEADERS frame received on a closed stream".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, STREAM_CLOSED, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        List<HeaderEntry> secondHeadersToSend = new ArrayList<HeaderEntry>();
        secondHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient secondFrameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameRstStream rstFrame = new FrameRstStream(3, CANCEL_ERROR, false);
        h2Client.sendFrame(rstFrame);
        h2Client.sendFrame(secondFrameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testContinuationFrameOnClosedStream(HttpServletRequest request,
                                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameOnClosedStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameOnClosedStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testContinuationFrameOnClosedStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] debugData = "CONTINUATION frame received on a closed stream".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, STREAM_CLOSED, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameRstStream rstFrame = new FrameRstStream(3, CANCEL_ERROR, false);
        h2Client.sendFrame(rstFrame);

        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameAfterHeaderFrameWithEndOfStream(HttpServletRequest request,
                                                             HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameAfterHeaderFrameWithEndOfStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameAfterHeaderFrameWithEndOfStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameAfterHeaderFrameWithEndOfStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "DATA Frame Received in the wrong state of: HALF_CLOSED_REMOTE".getBytes();
        FrameGoAwayClient errorFrame = new FrameGoAwayClient(0, debugData, new int[] { STREAM_CLOSED, PROTOCOL_ERROR }, new int[] { 1, 3 });
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameAfterHeaderFrameWithEndOfStream(HttpServletRequest request,
                                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameAfterHeaderFrameWithEndOfStream", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameAfterHeaderFrameWithEndOfStream",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameAfterHeaderFrameWithEndOfStream";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "HEADERS Frame Received in the wrong state of: HALF_CLOSED_REMOTE".getBytes();
        FrameGoAwayClient errorFrame = new FrameGoAwayClient(0, debugData, new int[] { STREAM_CLOSED, PROTOCOL_ERROR }, new int[] { 1, 3 });
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameAfterContinuationFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameAfterContinuationFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameAfterContinuationFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameAfterContinuationFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "Did not receive the expected continuation frame".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField(":scheme", "http"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, false, false, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testContinuationFrameOnStream0(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameOnStream0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameOnStream0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testContinuationFrameOnStream0";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "CONTINUATION frame streamID cannot be 0x0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField(":scheme", "http"));
        firstContinuationHeadersToSend.add(new H2HeaderField(":path", HEADERS_AND_BODY_URI));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(0, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testContinuationFrameAfterAnEndOfHeaders(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameAfterAnEndOfHeaders", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameAfterAnEndOfHeaders",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testContinuationFrameAfterAnEndOfHeaders";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "CONTINUATION Frame Received when not in a Continuation State".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testSecondContinuationFrameAfterAnEndOfHeaders(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSecondContinuationFrameAfterAnEndOfHeaders", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSecondContinuationFrameAfterAnEndOfHeaders",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSecondContinuationFrameAfterAnEndOfHeaders";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "CONTINUATION Frame Received when not in a Continuation State".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // create the second continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> secondContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        secondContinuationHeadersToSend.add(new H2HeaderField("liberty", "http2"));
        FrameContinuationClient secondContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        secondContinuationHeaders.setHeaderFields(secondContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);
        h2Client.sendFrame(secondContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testContinuationFrameAfterDataFrame(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameAfterDataFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testContinuationFrameAfterDataFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testContinuationFrameAfterDataFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "CONTINUATION Frame Received when not in a Continuation State".getBytes();
        FrameGoAwayClient errorFrame = new FrameGoAwayClient(0, debugData, new int[] { STREAM_CLOSED, PROTOCOL_ERROR }, new int[] { 1, 3 });
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // create the first continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);

        // create the second continuation frame to send over; note that end_headers IS set
        List<H2HeaderField> secondContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        secondContinuationHeadersToSend.add(new H2HeaderField("liberty", "http2"));
        FrameContinuationClient secondContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        secondContinuationHeaders.setHeaderFields(secondContinuationHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        h2Client.sendFrame(firstContinuationHeaders);
        h2Client.sendFrame(secondContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testDataFrameOnStream0(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOnStream0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOnStream0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameOnStream0";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "DATA frame stream ID cannot be 0x0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameDataClient dataFrame = new FrameDataClient(0, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testDataFrameBadPaddingLength(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameBadPaddingLength", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameBadPaddingLength",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameBadPaddingLength";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        byte[] debugData = "Error processing the payload for DATA frame on stream 3".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 3, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);

        h2Client.waitFor(frameHeaders);
        //PayloadLength = 4, pad length = 6
        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 6, true, true, false);
        h2Client.sendFrame(dataFrame);

        // malformed DATA: set padding length to 12, which is greater than the specified total payload length
        //____________________________________ ||____________________ - padding length byte
        String dataString = "00000b0009000000030c74657374000000000000";
        byte[] b = parseHexBinary(dataString);
        h2Client.sendBytes(b);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testPriorityFrameAfterHeaderFrameNoEndHeaders(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameAfterHeaderFrameNoEndHeaders", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameAfterHeaderFrameNoEndHeaders",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameAfterHeaderFrameNoEndHeaders";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Did not receive the expected continuation frame".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);

        FramePriority priorityFrame = new FramePriority(3, 0, 255, false, false);
        h2Client.sendFrame(priorityFrame);

        List<H2HeaderField> firstContinuationHeadersToSend = new ArrayList<H2HeaderField>();
        firstContinuationHeadersToSend.add(new H2HeaderField("harold", "padilla"));
        FrameContinuationClient firstContinuationHeaders = new FrameContinuationClient(3, null, true, true, false);
        firstContinuationHeaders.setHeaderFields(firstContinuationHeadersToSend);
        h2Client.sendFrame(firstContinuationHeaders);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testHeaderFrameOnStream0(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOnStream0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOnStream0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameOnStream0";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "HEADERS frame streamID cannot be 0x0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(0, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testHeaderFrameBadPaddingLength(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameBadPaddingLength", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameBadPaddingLength",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameBadPaddingLength";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "HEADERS padding length must be less than the length of the payload".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end headers flag IS NOT set
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        //PayloadLength: 34, pad length: 50
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 50, 0, true, true, true, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testPriorityFrameOnStream0(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnStream0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameOnStream0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameOnStream0";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        byte[] debugData = "PRIORITY frame stream ID cannot be 0x0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FramePriority priorityFrame = new FramePriority(0, 0, 255, false, false);
        h2Client.waitFor(frameHeaders);
        h2Client.sendFrame(priorityFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testPriorityFrameLength4(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameLength4", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameLength4",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameLength4";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        String errorMessage = "PRIORITY frame must have a length of 5 octets";
        FrameGoAway goaway = new FrameGoAway(0, errorMessage.getBytes(), FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(goaway);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(frameHeaders);

        //length: 4, which is invalid
        String priorityString = "0000040200000000037fffffffff";
        byte[] b = parseHexBinary(priorityString);
        h2Client.sendBytes(b);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testRstStreamFrameOnStream0(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameOnStream0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameOnStream0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testRstStreamFrameOnStream0";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "RST_STREAM frame stream ID cannot be 0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(0, CANCEL_ERROR, false);
        h2Client.sendFrame(rstFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testRstStreamFrameLength3(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameLength3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameLength3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testRstStreamFrameLength3";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        byte[] debugData = "RST_STREAM frame payload must have a length of 4 octets".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(frameHeaders);

        // malformed RST_STREAM: only has 3 byte byte payload
        //______________________||____________ - payload length byte
        String rstString = "000003030000000003000003";
        byte[] b = parseHexBinary(rstString);
        h2Client.sendBytes(b);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithInvalidFrameSize(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithInvalidFrameSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithInvalidFrameSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithInvalidFrameSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        byte[] debugData = "SETTINGS_MAX_FRAME_SIZE value exceeded the max allowable value".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        h2Client.waitFor(frameHeaders);
        FrameSettings settingsFrameWithValues = new FrameSettings(0, -1, -1, -1, -1, 16777216, -1, false);
        h2Client.sendFrame(settingsFrameWithValues);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithLessThanMinimunFrameSize(HttpServletRequest request,
                                                             HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithLessThanMinimunFrameSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithLessThanMinimunFrameSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithLessThanMinimunFrameSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        byte[] debugData = "Initial window size setting value exceeded max allowable value".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FLOW_CONTROL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(frameHeaders);

        //______________________|_____________________||||||||__ - window size bytes: set here as 2^0
        String settingsFrame = "0000060400000000000004ffffffff";
        byte[] b = parseHexBinary(settingsFrame);
        h2Client.sendBytes(b);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithInvalidMaxWindowSize(HttpServletRequest request,
                                                         HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithInvalidMaxWindowSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithInvalidMaxWindowSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithInvalidMaxWindowSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        byte[] debugData = "Initial window size setting value exceeded max allowable value".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FLOW_CONTROL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(frameHeaders);

        //____________________________________________||||||||__ - window size bytes: set here as 2^32
        String settingsFrame = "0000060400000000000004ffffffff";
        byte[] b = parseHexBinary(settingsFrame);
        h2Client.sendBytes(b);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithUnkownIdentifier(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithUnkownIdentifier", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithUnkownIdentifier",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithUnkownIdentifier";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] emptyBytes = new byte[8];
        FramePing expectedPing = new FramePing(0, emptyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        h2Client.waitFor(DEFAULT_SERVER_SETTINGS_FRAME);

        //__________________________________________||________ - setting type bytes: set here as 238
        String settingsFrame = "00000604000000000000eeffffffff";
        byte[] b = parseHexBinary(settingsFrame);
        h2Client.sendBytes(b);

        //send a ping and expect a ping back; this also helps us know if Setting ACK arrived as the PING
        //will not be sent until ACK arrives.
        FramePing ping = new FramePing(0, emptyBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithAckAndPayload(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithAckAndPayload", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithAckAndPayload",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithAckAndPayload";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "SETTINGS frame with ACK set cannot have an additional payload".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameSettings settingsFrameWithValues = new FrameSettings(0, 1, -1, -1, -1, -1, -1, false);
        settingsFrameWithValues.setAckFlag();
        h2Client.sendFrame(settingsFrameWithValues);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameOnStream3(HttpServletRequest request,
                                          HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameOnStream3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameOnStream3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameOnStream3";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "SETTINGS frame stream ID must be 0x0; received 3".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameSettings settingsFrame = new FrameSettings(3, -1, -1, -1, -1, -1, -1, false);
        h2Client.sendFrame(settingsFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameBadSize(HttpServletRequest request,
                                        HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameBadSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameBadSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameBadSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Settings frame is malformed".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // Send the whole frame at one time.  An intermittent error was occurring when the first half of the
        // frame was sent, and before the last 3 bytes could be sent, another frame came in from the server.
        // RTC defect 263417

        //setting with 6 bytes payload
        byte[] settingsFrame = { 0, 0, (byte) 3, (byte) 4, 0, 0, 0, 0, 0, 0, (byte) 3, 0 };
        //SETTINGS_INITIAL_WINDOW_SIZE = 2147483648
        //byte[] parameter = { 0, (byte) 3, 0 };

        h2Client.sendBytes(settingsFrame);
        //h2Client.sendBytes(parameter);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPingFrameSentWithACK(HttpServletRequest request,
                                         HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameSentWithACK", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameSentWithACK",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPingFrameSentWithACK";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] invalid = { 'i', 'n', 'v', 'a', 'l', 'i', 'd', 'o' };
        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing expectedPing = new FramePing(0, libertyBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //send a ping and expect a ping back; this also helps us know if Setting ACK arrived as the PING
        //will not be sent until ACK arrives.
        FramePing invalidPing = new FramePing(0, invalid, false);
        invalidPing.setAckFlag();
        FramePing libertyPing = new FramePing(0, libertyBytes, false);

        h2Client.sendFrame(invalidPing);
        h2Client.sendFrame(libertyPing);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPingFrameOnStream3(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameOnStream3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameOnStream3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPingFrameOnStream3";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "ping frames must be sent on stream 0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //send a ping and expect a ping back; this also helps us know if Setting ACK arrived as the PING
        //will not be sent until ACK arrives.
        byte[] libertyBytes = { 'l', 'i', 'b', 'e', 'r', 't', 'y', '1' };
        FramePing libertyPing = new FramePing(3, libertyBytes, false);
        h2Client.sendFrame(libertyPing);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPingFrameBadSize(HttpServletRequest request,
                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameBadSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPingFrameBadSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPingFrameBadSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "ping frames must have a length of 8 bytes".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(frameHeaders);

        //______________________||___________________________ - frame size byte: set to 7
        String pingFrame = "0000070600000000006c696265727479";
        byte[] b = parseHexBinary(pingFrame);
        h2Client.sendBytes(b);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);

    }

    public void testGoAwayFrameOnStream3(HttpServletRequest request,
                                         HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testGoAwayFrameOnStream3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testGoAwayFrameOnStream3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testGoAwayFrameOnStream3";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "GOAWAY frame streamID must be 0x0 - received: 3".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(new FrameGoAway(3, new byte[] { (byte) 0, (byte) 1 }, 0, 1, false));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);

    }

    public void testTwoWindowUpdateFrameAboveMaxSize(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoWindowUpdateFrameAboveMaxSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoWindowUpdateFrameAboveMaxSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testTwoWindowUpdateFrameAboveMaxSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "processWindowUpdateFrame: out of bounds increment, current connection write limit: 65535 total would have been: 2147549182".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FLOW_CONTROL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameWindowUpdate windowUpdateA = new FrameWindowUpdate(0, 2147483647, false);
        h2Client.sendFrame(windowUpdateA);

        FrameWindowUpdate windowUpdateB = new FrameWindowUpdate(0, 2147483647, false);
        h2Client.sendFrame(windowUpdateB);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);

    }

    public void testTwoWindowUpdateFrameAboveMaxSizeOnStream3(HttpServletRequest request,
                                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoWindowUpdateFrameAboveMaxSizeOnStream3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoWindowUpdateFrameAboveMaxSizeOnStream3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testTwoWindowUpdateFrameAboveMaxSizeOnStream3";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        //byte[] debugData = "processWindowUpdateFrame: out of bounds increment, current connection write limit: 65535 total would have been: 2147549182".getBytes();
        FrameRstStream rstFrame = new FrameRstStream(3, FLOW_CONTROL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // create headers to send over to the server; note that the end stream flag false
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        h2Client.waitFor(frameHeaders);
        FrameWindowUpdate windowUpdateA = new FrameWindowUpdate(3, 2147483647, false);
        h2Client.sendFrame(windowUpdateA);

        FrameWindowUpdate windowUpdateB = new FrameWindowUpdate(3, 2147483647, false);
        h2Client.sendFrame(windowUpdateB);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);

    }

    public void testBadPRI(HttpServletRequest request,
                           HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testBadPRI", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testBadPRI",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testBadPRI";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPreface("Bad-PRI");

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        Assert.assertTrue(testName + " received the server's preface. wasServerPrefaceReceived() = true", !h2Client.wasServerPrefaceReceived());
        //handleErrors(h2Client, testName);
    }

    public void testHeaderFrameOnDifferentStreams(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOnDifferentStreams", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOnDifferentStreams",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameOnDifferentStreams";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "Did not receive the expected continuation frame".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        FrameHeadersClient secondFrameHeadersToSend = new FrameHeadersClient(5, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(secondFrameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFramesDecreasingStreamIds(HttpServletRequest request,
                                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFramesDecreasingStreamIds", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFramesDecreasingStreamIds",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFramesDecreasingStreamIds";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "received a new stream with a lower ID than previous; current stream-id: 3 highest stream-id: 5".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 5, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(5, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        FrameHeadersClient secondFrameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(secondFrameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameDependsOnItself(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDependsOnItself", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDependsOnItself",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameDependsOnItself";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 3, 0, 255, true, true, false, true, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.waitFor(frameHeaders);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testPriorityFrameDependsOnItself(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameDependsOnItself", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testPriorityFrameDependsOnItself",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testPriorityFrameDependsOnItself";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(frameHeaders);

        FramePriority priorityFrame = new FramePriority(3, 3, 255, false, false);
        h2Client.sendFrame(priorityFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        this.handleErrors(h2Client, testName);
    }

    public void testUnknownFrameAfterHeaderFrame(HttpServletRequest request,
                                                 HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUnknownFrameAfterHeaderFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testUnknownFrameAfterHeaderFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testUnknownFrameAfterHeaderFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "Did not receive the expected continuation frame".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, false, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // send over the header frames followed by the continuation frames
        h2Client.sendFrame(frameHeadersToSend);

        //Type 244, lenght 0
        byte[] unknownFrameHeader = { 0, 0, 0, -12, 0, 0, 0, 0, 0 };
        h2Client.sendBytes(unknownFrameHeader);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSettingFrameWithInvalidPushPromise(HttpServletRequest request,
                                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithInvalidPushPromise", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSettingFrameWithInvalidPushPromise",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSettingFrameWithInvalidPushPromise";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "SETTINGS_ENABLE_PUSH must be set to 0 or 1 0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        FrameSettings settingsFrameWithValues = new FrameSettings(0, -1, 2, -1, -1, -1, -1, false);
        h2Client.sendFrame(settingsFrameWithValues);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testInitialWindowSize1(HttpServletRequest request,
                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInitialWindowSize1", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testInitialWindowSize1",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        boolean testPassed = false;
        String testName = "testInitialWindowSize1";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, 1000L);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);

        FrameSettings oneWindowSizeSettingsFrame = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(oneWindowSizeSettingsFrame);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await();

        for (Exception ex : h2Client.getReportedExceptions()) {
            if (ex.getMessage().startsWith("StreamId: 3 did not receive the end of stream flag")) {
                testPassed = true; //if we get this exception, this means the server could not send the DATA frame with data which is what we want.
            }
        }

        Assert.assertTrue("StreamId: 3 did receive and end of stream flag, meaning that the server was able to send the DATA frame with payload greater that the window size.",
                          testPassed);

    }

    public void testGoAwayFrameInvalidErrorCode(HttpServletRequest request,
                                                HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testGoAwayFrameInvalidErrorCode", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testGoAwayFrameInvalidErrorCode",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testGoAwayFrameInvalidErrorCode";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        int NO_ERROR = 0x0;
        FrameGoAway errorFrame = new FrameGoAway(0, null, NO_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(new FrameGoAway(0, new byte[] { (byte) 0, (byte) 1 }, 255, 1, false));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);

    }

    public void testRstStreamFrameInvalidErrorCode(HttpServletRequest request,
                                                   HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameInvalidErrorCode", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testRstStreamFrameInvalidErrorCode",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testRstStreamFrameInvalidErrorCode";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        h2Client.waitFor(frameHeaders);
        FrameRstStream rstFrame = new FrameRstStream(3, 255, false);
        h2Client.sendFrame(rstFrame);

        byte[] testBytes = new byte[] { 'l', 'i', 'b', 'e', 'r', 't', 'y' };
        FramePing expectedPing = new FramePing(0, testBytes, false);
        expectedPing.setAckFlag();
        h2Client.addExpectedFrame(expectedPing);

        FramePing ping = new FramePing(0, testBytes, false);
        h2Client.sendFrame(ping);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithInvalidPseudoHeader(HttpServletRequest request,
                                                       HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithInvalidPseudoHeader", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithInvalidPseudoHeader",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithInvalidPseudoHeader";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.addExpectedFrame(rstFrame);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":test", "Invalid"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithInvalidRequestPseudoHeader(HttpServletRequest request,
                                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithInvalidRequestPseudoHeader", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithInvalidRequestPseudoHeader",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithInvalidRequestPseudoHeader";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.addExpectedFrame(rstFrame);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":status", "200"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithInvalidTrailerPseudoHeader(HttpServletRequest request,
                                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithInvalidTrailerPseudoHeader", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithInvalidTrailerPseudoHeader",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithInvalidTrailerPseudoHeader";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Psuedo-headers are not allowed in trailers: :authority: respect.my".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        List<HeaderEntry> secondHeadersToSend = new ArrayList<HeaderEntry>();
        secondHeadersToSend.add(new HeaderEntry(new H2HeaderField(":authority", "respect.my"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient secondFrameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeadersToSend.setHeaderEntries(secondHeadersToSend);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, false, false, false);

        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame);
        h2Client.sendFrame(secondFrameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithPseudoHeadersLast(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithPseudoHeadersLast", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithPseudoHeadersLast",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithPseudoHeadersLast";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Invalid pseudo-header decoded: all pseudo-headers must appear in the header block before regular header fields.".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("header-before", "pseudo-headers"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithConnectionSpecificFields(HttpServletRequest request,
                                                            HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithConnectionSpecificFields", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithConnectionSpecificFields",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithConnectionSpecificFields";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        byte[] debugData = "Invalid Connection header received: connection: keep-alive".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("connection", "keep-alive"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithBadTEHeader(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithBadTEHeader", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithBadTEHeader",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithBadTEHeader";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        int COMPRESSION_ERROR = 0x9;
        byte[] debugData = "Invalid header: TE header must have value \"trailers\": te: trailers, deflate".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("trailers", "test"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("te", "trailers, deflate"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithEmptyPath(HttpServletRequest request,
                                             HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithEmptyPath", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithEmptyPath",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithEmptyPath";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", ""), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithoutMethodField(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithoutMethodField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithoutMethodField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithoutMethodField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        //firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithoutSchemeField(HttpServletRequest request,
                                                  HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithoutSchemeField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithoutSchemeField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithoutSchemeField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        //firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameWithoutPathField(HttpServletRequest request,
                                                HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithoutPathField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameWithoutPathField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameWithoutPathField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameDuplicatedMethodField(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDuplicatedMethodField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDuplicatedMethodField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameDuplicatedMethodField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        int COMPRESSION_ERROR = 0x9;
        byte[] debugData = "Invalid pseudo-header for decompression context: :method: GET".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameDuplicatedSchemeField(HttpServletRequest request,
                                                     HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDuplicatedSchemeField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDuplicatedSchemeField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameDuplicatedSchemeField";
        //int streamId = 3;

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        boolean testFailed = false;
        StringBuilder message = new StringBuilder("The following exceptions were found: \n");

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME); //FIXME this is the server preface, it may have values

        int COMPRESSION_ERROR = 0x9;
        byte[] debugData = "Invalid pseudo-header for decompression context: :scheme: http".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);

        //Since this is a conditional send, this will block the thread until the preface is sent.
        //If the this fails, the test needs to fail as well because the H2 protocol was not established successfully.
        //zeroWindowSizeSettingsFrame will prevent the server from closing the stream we want to test
        //FrameSettings zeroWindowSizeSettingsFrame = new FrameSettings(0, -1, -1, -1, 0, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);

        //FrameSettings settingsFrameWithValues = new FrameSettings(0, -1, 2, -1, -1, -1, -1, false);

        //h2Client.sendFrame(settingsFrameWithValues);

        //the server should be able to send frames after this windowUpdate is sent
        //FrameWindowUpdate windowUpdate = new FrameWindowUpdate(streamId, 1, false);
        //h2Client.sendFrame(windowUpdate);

        //send a ping and expect a ping back; this also helps us know if Setting ACK arrived as the PING
        //will not be sent until ACK arrives.
        //FramePing ping = new FramePing(0, emptyBytes, false);
        //h2Client.sendFrame(ping);

        //Use CountDownLatch to block this test thread until we know the test is done (meaning, the connection has been closed)
        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);

        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameDuplicatedPathField(HttpServletRequest request,
                                                   HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDuplicatedPathField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameDuplicatedPathField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameDuplicatedPathField";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Invalid pseudo-header for decompression context: :path: /H2TestModule/H2HeadersAndBody".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameIncorrectContentLength(HttpServletRequest request,
                                                      HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameIncorrectContentLength", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameIncorrectContentLength",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameIncorrectContentLength";

        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        //content-length: 1 is wrong as the DATA frame we are sending is heavier
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("content-length", "1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameIncorrectSumContentLength(HttpServletRequest request,
                                                         HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameIncorrectSumContentLength", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameIncorrectSumContentLength",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameIncorrectSumContentLength";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(3, PROTOCOL_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        //content-length: 1 is wrong as the DATA frame we are sending is heavier
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("content-length", "1"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        h2Client.sendFrame(new FrameDataClient(3, "test".getBytes(), 0, false, false, false));
        h2Client.sendFrame(new FrameDataClient(3, "test".getBytes(), 0, true, false, false));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSecondHeaderFrameWithoutEndOfStream(HttpServletRequest request,
                                                        HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameIncorrectContentLength", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameIncorrectContentLength",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameIncorrectContentLength";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "HEADERS frame received on a closed stream".getBytes();
        FrameGoAwayClient errorFrame = new FrameGoAwayClient(0, debugData, new int[] { STREAM_CLOSED, PROTOCOL_ERROR }, new int[] { 1, 3 });
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        List<HeaderEntry> secondHeadersToSend = new ArrayList<HeaderEntry>();
        secondHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient secondFrameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeadersToSend.setHeaderEntries(secondHeadersToSend);

        h2Client.sendFrame(frameHeadersToSend);
        FrameDataClient dataFrame = new FrameDataClient(3, "test".getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);
        h2Client.sendFrame(secondFrameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendPushPromise(HttpServletRequest request,
                                    HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPushPromise", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendPushPromise",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendPushPromise";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "PUSH_PROMISE Frame Received on server side".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        List<H2HeaderField> pushPromiseHeaders = new ArrayList<H2HeaderField>();
        pushPromiseHeaders.add(new H2HeaderField(":method", "GET"));
        pushPromiseHeaders.add(new H2HeaderField(":path", "/H2TestModule/H2HeadersOnly"));
        pushPromiseHeaders.add(new H2HeaderField(":authority", "127.0.0.1"));
        pushPromiseHeaders.add(new H2HeaderField(":scheme", "http"));
        byte[] headerBlockFragment = new byte[0];
        FramePushPromiseClient pushPromise = new FramePushPromiseClient(1, headerBlockFragment, 2, 0, true, false, false);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(pushPromise);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameOf16384Bytes(HttpServletRequest request,
                                          HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOf16384Bytes", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOf16384Bytes",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameOf16384Bytes";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);
        addSecondExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //create a string with 16384 octets
        StringBuilder testString = new StringBuilder();
        for (int i = 0; i < 16384; i++) {
            testString.append('x');
        }

        FrameDataClient dataFrame = new FrameDataClient(3, testString.toString().getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testDataFrameOfMaxPlusOneBytes(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOfMaxPlusOneBytes", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testDataFrameOfMaxPlusOneBytes",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testDataFrameOfMaxPlusOneBytes";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "DATA payload greater than allowed by the max frame size".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);

        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //create a string with 16385 octets, 16384 is the max
        StringBuilder testString = new StringBuilder();
        for (int i = 0; i < 16384 + 1; i++) {
            testString.append('x');
        }
        FrameDataClient dataFrame = new FrameDataClient(3, testString.toString().getBytes(), 0, true, false, false);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testHeaderFrameOverMaxBytes(HttpServletRequest request,
                                            HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOverMaxBytes", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testHeaderFrameOverMaxBytes",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testHeaderFrameOverMaxBytes";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        addFirstExpectedHeaders(h2Client);

        byte[] debugData = "HEADERS payload greater than allowed by the max frame size".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, FRAME_SIZE_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        //creating a huge HEADER block, bigger than the max frame size
        for (int i = 0; i < 16384 + 1; i++) {
            firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("test", "repeat"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        }
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendHeadersFrameUppercaseField(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameUppercaseField", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameUppercaseField",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadersFrameUppercaseField";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Header field names must not contain uppercase characters. Decoded header name: T".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        // cannot assume language of test machine
        firstHeadersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        h2Client.addExpectedFrame(frameHeaders);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //locking for logging purposes; wait for EoS data and then send setting frame
        h2Client.waitFor(new FrameData(1, new byte[0], 0, true, false, false));

        /**
         * FrameType: HEADERS
         * FrameFlags:
         * FlagAckSet: false
         * FlagPrioritySet: false
         * FlagEndStreamSet: true
         * FlagEndHeadersSet: true
         * FlagPaddedSet: false
         * FrameReserveBit: false
         * PayloadLength: 0
         * StreamId: 3
         *
         * PaddingLength: 0
         * isExclusive: false
         * StreamIdDependency: 0
         * Weight: 0
         *
         * Header fields: (NEVERINDEX, NO HUFFMAN)
         * :method: GET
         * :scheme: http
         * :path: /H2TestModule/H2HeadersAndBody
         * T: t
         */
        h2Client.sendBytes(parseHexBinary("0000270104000000038286141E2F4832546573744D6F64756C652F483248656164657273416E64426F64791001540174"));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendHeadersFrameDynamicTableSizeUpdate(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameDynamicTableSizeUpdate", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameDynamicTableSizeUpdate",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadersFrameDynamicTableSizeUpdate";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "dynamic table size update must occur at the beginning of the first header block".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //locking for logging purposes; wait for EoS data and then send setting frame
        h2Client.waitFor(new FrameData(1, new byte[0], 0, true, false, false));

        /**
         * FrameType: HEADERS
         * FrameFlags:
         * FlagAckSet: false
         * FlagPrioritySet: false
         * FlagEndStreamSet: true
         * FlagEndHeadersSet: true
         * FlagPaddedSet: false
         * FrameReserveBit: false
         * PayloadLength: 0
         * StreamId: 3
         *
         * PaddingLength: 0
         * isExclusive: false
         * StreamIdDependency: 0
         * Weight: 0
         *
         * Header fields: (NEVERINDEX, NO HUFFMAN)
         * :method: GET
         * :scheme: http
         * :path: /H2TestModule/H2HeadersAndBody
         * t: T
         * <dynamic window update>
         */
        h2Client.sendBytes(parseHexBinary("0000280105000000038286141E2F4832546573744D6F64756C652F483248656164657273416E64426F6479100174015421"));

        //Use CountDownLatch to block this test thread until we know the test is done (meaning, the connection has been closed)
        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);

        handleErrors(h2Client, testName);
    }

    public void testSendHeadersFrameInvalidIndex(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameInvalidIndex", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameInvalidIndex",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadersFrameInvalidIndex";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Received an invalid header index".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        //locking for logging purposes; wait for EoS data and then send setting frame
        h2Client.waitFor(new FrameData(1, new byte[0], 0, true, false, false));

        /**
         * FrameType: HEADERS
         * FrameFlags:
         * FlagAckSet: false
         * FlagPrioritySet: false
         * FlagEndStreamSet: true
         * FlagEndHeadersSet: true
         * FlagPaddedSet: false
         * FrameReserveBit: false
         * PayloadLength: 0
         * StreamId: 3
         *
         * PaddingLength: 0
         * isExclusive: false
         * StreamIdDependency: 0
         * Weight: 0
         *
         * Header fields: (NEVERINDEX, NO HUFFMAN)
         * :method: GET
         * :scheme: http
         * :path: /H2TestModule/H2HeadersAndBody
         * t: t
         * <invalid header index>
         */
        h2Client.sendBytes(parseHexBinary("0000280105000000038286141E2F4832546573744D6F64756C652F483248656164657273416E64426F64791001740154C6"));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testSendHeadersFrameInvalidHuffmanWithExtraPad(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameInvalidHuffmanWithExtraPad", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameInvalidHuffmanWithExtraPad",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadersFrameInvalidHuffmanWithExtraPad";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "Received an invalid header block fragment".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);

        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(new FrameData(1, new byte[0], 0, true, false, false));

        /**
         * FrameType: HEADERS
         * FrameFlags:
         * FlagAckSet: false
         * FlagPrioritySet: false
         * FlagEndStreamSet: true
         * FlagEndHeadersSet: true
         * FlagPaddedSet: false
         * FrameReserveBit: false
         * PayloadLength: 0
         * StreamId: 3
         *
         * PaddingLength: 0
         * isExclusive: false
         * StreamIdDependency: 0
         * Weight: 0
         *
         * Header fields: (NEVERINDEX, NO HUFFMAN)
         * :method: GET
         * :scheme: http
         * :path: /H2TestModule/H2HeadersAndBody
         * t: t
         * <invalid huffman header>
         */
        h2Client.sendBytes(parseHexBinary("0000330105000000038286141E2F4832546573744D6F64756C652F483248656164657273416E64426F647910017401540085F2B24A84FF8449509FFF"));
        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);

        handleErrors(h2Client, testName);
    }

    public void testSendHeadersFrameFieldIndex0(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameFieldIndex0", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testSendHeadersFrameFieldIndex0",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testSendHeadersFrameFieldIndex0";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        byte[] debugData = "An indexed header cannot have an index of 0".getBytes();
        FrameGoAway errorFrame = new FrameGoAway(0, debugData, COMPRESSION_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.waitFor(new FrameData(1, new byte[0], 0, true, false, false));

        /**
         * FrameType: HEADERS
         * FrameFlags:
         * FlagAckSet: false
         * FlagPrioritySet: false
         * FlagEndStreamSet: true
         * FlagEndHeadersSet: true
         * FlagPaddedSet: false
         * FrameReserveBit: false
         * PayloadLength: 0
         * StreamId: 3
         *
         * PaddingLength: 0
         * isExclusive: false
         * StreamIdDependency: 0
         * Weight: 0
         *
         * Header fields: (NEVERINDEX, NO HUFFMAN)
         * :method: GET
         * :scheme: http
         * :path: /H2TestModule/H2HeadersAndBody
         * t: t
         * <header with invalid index>
         */
        String frameBytes = "0000280105000000038286141E2F4832546573744D6F64756C652F483248656164657273416E64426F6479100174015480";
        h2Client.sendBytes(parseHexBinary(frameBytes));
        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);

        handleErrors(h2Client, testName);
    }

    public void testModifiedInitialWindowSizeAfterHeaderFrame(HttpServletRequest request,
                                                              HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testModifiedInitialWindowSizeAfterHeaderFrame", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testModifiedInitialWindowSizeAfterHeaderFrame",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testModifiedInitialWindowSizeAfterHeaderFrame";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));
        h2Client.addExpectedFrame(new FrameData(1, dataString.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI);

        FrameSettings zeroWindowSizeSettingsFrame = new FrameSettings(0, -1, -1, -1, 1, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(zeroWindowSizeSettingsFrame);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //update SETTINGS_INITIAL_WINDOW_SIZE and make the window big enough for the DATA frame to be sent
        h2Client.sendFrame(new FrameSettings(0, -1, -1, -1, 20, -1, -1, false));

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testNegativeToPositiveWindowSize(HttpServletRequest request,
                                                 HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNegativeToPositiveWindowSize", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testNegativeToPositiveWindowSize",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }
        String testName = "testNegativeToPositiveWindowSize";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(3, dataString.getBytes(), 0, false, false, false));
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);

        // connection window size is too small for any frames to be sent
        FrameSettings threeWindowSizeSettingsFrame = new FrameSettings(0, -1, -1, -1, 3, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(threeWindowSizeSettingsFrame);

        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(3, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        h2Client.sendFrame(frameHeadersToSend);

        //now SETTINGS_INITIAL_WINDOW_SIZE will be 2 and the window size will be negative
        h2Client.sendFrame(new FrameSettings(0, -1, -1, -1, 0, -1, -1, false));

        FrameWindowUpdate windowUpdate = new FrameWindowUpdate(0, 20, false);
        h2Client.sendFrame(windowUpdate);

        //now we should have a window size of 1, allowing flow controlled frames to arrive
        windowUpdate = new FrameWindowUpdate(3, 20, false);
        h2Client.sendFrame(windowUpdate);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    public void testExceedMaxConcurrentStreams(HttpServletRequest request,
                                               HttpServletResponse response) throws InterruptedException, Exception {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoWindowUpdateFrameAboveMaxSizeOnStream3", "Started!");
            LOGGER.logp(Level.INFO, this.getClass().getName(), "testTwoWindowUpdateFrameAboveMaxSizeOnStream3",
                        "Connecting to = " + request.getParameter("hostName") + ":" + request.getParameter("port"));
        }

        final int server_max_concurrent_streams = 200;
        String testName = "testExceedMaxConcurrentStreams";
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        FrameRstStream rstFrame = new FrameRstStream(403, REFUSED_STREAM_ERROR, false);
        h2Client.addExpectedFrame(rstFrame);

        FrameHeaders frameHeaders = addFirstExpectedHeaders(h2Client);
        h2Client.sendUpgradeHeader(HEADERS_ONLY_URI);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        //Headers frame to send for "second" request
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_ONLY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));

        int currentStream = 1;
        // create another 200 streams
        for (int i = 0; i < server_max_concurrent_streams + 1; i++) {
            currentStream += 2;
            // note end_stream is set to false, so this stream will remain open
            FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(currentStream, null, 0, 0, 0, false, true, false, false, false, false);
            frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
            h2Client.sendFrame(frameHeadersToSend);
        }

        blockUntilConnectionIsDone.await(10000, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    /**
     * Send an upgrade header to a server that has servlet 4.0, but has HTTP/2 turned off.
     * This should result in a timeout waiting for the 101 response.
     *
     * @param the Http2Client that will expect a header response
     * @return the expected FrameHeaders
     */
    public void servlet40H2Off(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "servlet40H2Off";

        // Send the upgrade request
        // This is a normal http GET request with an H2 upgrade header
        // Since the server is running servlet 4.0 but with http/2 turned off, it should
        // just return a normal http 200 response
        // There should be no 101 response received since the connection is not upgraded
        // The test should timeout waiting for a 101 response
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI, HTTPUtils.HTTPMethod.POST);

        StringBuilder message = new StringBuilder("The following exceptions were found: ");
        boolean testFailed = true;
        try {
            h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        } catch (Exception cptoe) {
            // This should be a clientPrefaceTimeoutException which is expected
            testFailed = false;
            message.append(cptoe.getClass() + ": " + cptoe.getMessage());
        }

        Assert.assertFalse(message.toString() + " in test: " + testName, testFailed);
    }

    /**
     * Send an upgrade header to a server that has servlet 3.1, but has HTTP/2 turned off.
     * This should result in a timeout waiting for the 101 response.
     *
     * @param the Http2Client that will expect a header response
     * @return the expected FrameHeaders
     */
    public void servlet31H2Off(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "servlet31H2Off";

        // Send the upgrade request
        // This is a normal http request, but with an H2 upgrade header
        // Since the server is running servlet 3.1 but with http/2 off, it should
        // just return a normal http 200 response
        // There should be no 101 response received since the connection is not upgraded
        // The test should timeout waiting for a 101 response
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);
        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI, HTTPUtils.HTTPMethod.POST);

        boolean testFailed = true;
        try {
            h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        } catch (Exception cptoe) {
            // This should be a clientPrefaceTimeoutException,
            testFailed = false;
        }

        Assert.assertFalse("In test: " + testName, testFailed);
    }

    /**
     * Send an upgrade header to a server that has servlet 3.1, but has HTTP/2 turned on.
     * This should result in a normal HTTP/2 response
     *
     * @param the Http2Client that will expect a header response
     * @return the expected FrameHeaders
     */
    public void servlet31H2On(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "servlet31H2On";
        Http2Client h2Client = getDefaultH2Client(request, response, blockUntilConnectionIsDone);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        
        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/3.1"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        firstHeadersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        h2Client.addExpectedFrame(frameHeaders);

        String dataString = "ABC123";
        h2Client.addExpectedFrame(new FrameData(1, dataString.getBytes(), 0, false, false, false));

        h2Client.sendUpgradeHeader(HEADERS_AND_BODY_URI, HTTPUtils.HTTPMethod.POST);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    /**
     * Tests initializing a cleartext HTTP/2 connection initialized without the h2c Upgrade header
     * A single GET request is sent; headers and data are expected back on the same stream
     * 
     * @param request
     * @param response
     * @throws InterruptedException
     * @throws Exception
     */
    public void testHeaderAndDataPriorKnowledge(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testHeaderAndDataPriorKnowledge";

        // config the client to use HTTP/2 with prior knowledge
        Http2Client h2Client =  new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, 
            defaultTimeoutToSendFrame, true);

        // create expected header response
        List<H2HeaderField> headersReceived = new ArrayList<H2HeaderField>();
        headersReceived.add(new H2HeaderField(":status", "200"));
        headersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        headersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        headersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient serverHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        serverHeaders.setHeaderFields(headersReceived);

        // add expected header and body response to the client
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.addExpectedFrame(serverHeaders);
        h2Client.addExpectedFrame(new FrameData(1, "ABC123".getBytes(), 0, true, false, false));

        // create a GET request
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "GET"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("harold", "padilla"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(1, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);

        // init the connection and send request to the server
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    /**
     * Tests initializing a cleartext HTTP/2 connection initialized without the h2c Upgrade header
     * A POST request and body are sent; headers and data are expected back on the same stream
     * 
     * @param request
     * @param response
     * @throws InterruptedException
     * @throws Exception
     */
    public void testPostRequestDataKnowledge(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testHeaderAndDataPriorKnowledge";

        // config the client to use HTTP/2 with prior knowledge
        Http2Client h2Client =  new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, 
            defaultTimeoutToSendFrame, true);

        // create expected header response
        List<H2HeaderField> headersReceived = new ArrayList<H2HeaderField>();
        headersReceived.add(new H2HeaderField(":status", "200"));
        headersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        headersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        headersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient serverHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        serverHeaders.setHeaderFields(headersReceived);

        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        h2Client.addExpectedFrame(serverHeaders);
        h2Client.addExpectedFrame(new FrameData(1, "ABC123".getBytes(), 0, true, false, false));

        // create a POST request and body
        List<HeaderEntry> firstHeadersToSend = new ArrayList<HeaderEntry>();
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":method", "POST"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":scheme", "http"), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField(":path", HEADERS_AND_BODY_URI), HpackConstants.LiteralIndexType.NEVERINDEX, false));
        firstHeadersToSend.add(new HeaderEntry(new H2HeaderField("content-length", String.valueOf("test".getBytes().length)), 
            HpackConstants.LiteralIndexType.NEVERINDEX, false));
        FrameHeadersClient frameHeadersToSend = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeadersToSend.setHeaderEntries(firstHeadersToSend);
        FrameDataClient dataFrame = new FrameDataClient(1, "test".getBytes(), 0, true, false, false);

        // init the connection and send the post request and body to the server
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        h2Client.sendFrame(frameHeadersToSend);
        h2Client.sendFrame(dataFrame);

        blockUntilConnectionIsDone.await(500, TimeUnit.MILLISECONDS);
        handleErrors(h2Client, testName);
    }

    void handleErrors(Http2Client client, String testName) {
        boolean testFailed = false;
        List<Exception> errors = client.getReportedExceptions();
        StringBuilder message = new StringBuilder("The following exceptions were found: ");

        if (errors != null && !errors.isEmpty()) {
            testFailed = true;
            for (Exception e : errors) {
                message.append(e.getClass() + ": " + e.getMessage());
            }
        }
        Assert.assertFalse(message.toString() + " in test: " + testName, testFailed);
    }

    void waitForTestCompletion(CountDownLatch latch) throws InterruptedException {
        latch.await();
    }

    /**
     * Set up a default Http2Client
     *
     * @param request
     * @param response
     * @param blockUntilConnectionIsDone
     * @return the default Http2Client
     */
    Http2Client getDefaultH2Client(HttpServletRequest request, HttpServletResponse response, CountDownLatch blockUntilConnectionIsDone) {
        return new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, defaultTimeoutToSendFrame);
    }

    /**
     * Performs the typical steps needed to start a test:
     * 1. add an expected settings frame
     * 2. add the first expected header response
     * 3. send the HEADERS_AND_BODY_URI upgrade request
     * 4. send the client preface frames
     *
     * @param client
     * @throws IOException
     * @throws Exception
     */
    FrameHeaders setupDefaultPreface(Http2Client client) throws IOException, Exception {
        client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);
        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary

        // use .* below, can not assume language of test machines
        firstHeadersReceived.add(new H2HeaderField("content-language", ".*"));

        FrameHeadersClient frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);

        client.sendUpgradeHeader(HEADERS_AND_BODY_URI);
        client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);
        return frameHeaders;
    }

    /**
     * Set up the first expected header frame response for a generic upgrade request:
     * firstHeadersReceived.add(new H2HeaderField(":status", "200"));
     * firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
     * firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
     * firstHeadersReceived.add(new H2HeaderField("content-language", "en-US"));
     *
     *
     * @param the Http2Client that will expect a header response
     * @return the expected FrameHeaders
     */
    private FrameHeaders addFirstExpectedHeaders(Http2Client client) throws Exception {
        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        // cannot assume language of test machine
        firstHeadersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, false, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        client.addExpectedFrame(frameHeaders);
        return frameHeaders;
    }

    /**
     * Set up the second expected header frame response for a typical test
     *
     * @param the Http2Client that will expect a header response
     * @return the expected FrameHeaders
     */
    private FrameHeaders addSecondExpectedHeaders(Http2Client client) throws Exception {
        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        // cannot assume language of test machine
        secondHeadersReceived.add(new H2HeaderField("content-language", ".*"));
        FrameHeadersClient secondFrameHeaders = new FrameHeadersClient(3, null, 0, 0, 0, false, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        client.addExpectedFrame(secondFrameHeaders);
        return secondFrameHeaders;
    }

}
