/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http2.test.Http2Client;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.frames.FramePushPromiseClient;

/**
 * Test servlet for http2 push promise
 */
@WebServlet(urlPatterns = "/PushPromiseTests", asyncSupported = true)
public class PushPromiseTests extends H2FATDriverServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PushPromiseTests.class.getName());
    private static final String SERVLET_PUSH_PROMISE = "/H2TestModule/H2PushPromise";

    public void testPushPromisePreload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testPushPromisePreload";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "request: " + request);
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "hostName: " + request.getParameter("hostName"));
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "port: " + request.getParameter("port"));
        }
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, defaultTimeoutToSendFrame);

        boolean testFailed = false;
        StringBuilder message = new StringBuilder("The following exceptions were found: ");

        // We should be expecting
        //  some setting frames
        //  a push promise frame on stream 1
        //  a second push promise frame on stream 1
        //  a headers frame on stream 1 to satisfy the original request
        //  a headers frame on stream 2 to satisfy the pushed request
        //  a headers frame on stream 4 to satisfy the second pushed request

        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Setting frame(s) on stream 0
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        // PushPromiseFrame on stream 1
        List<H2HeaderField> pushPromiseHeadersReceived = new ArrayList<H2HeaderField>();
        pushPromiseHeadersReceived.add(new H2HeaderField(":method", "GET"));
        pushPromiseHeadersReceived.add(new H2HeaderField(":path", SERVLET_PUSH_PROMISE));
        pushPromiseHeadersReceived.add(new H2HeaderField(":authority", "127.0.0.1"));
        pushPromiseHeadersReceived.add(new H2HeaderField(":scheme", "http"));
        byte[] headerBlockFragment = new byte[0];
        FramePushPromiseClient pushPromise = new FramePushPromiseClient(1, headerBlockFragment, 2, 0, true, false, false);
        h2Client.addExpectedFrame(pushPromise);

        // Seconds PushPromiseFrame on stream 1
        List<H2HeaderField> pushPromiseHeadersReceived2 = new ArrayList<H2HeaderField>();
        pushPromiseHeadersReceived2.add(new H2HeaderField(":method", "GET"));
        pushPromiseHeadersReceived2.add(new H2HeaderField(":path", SERVLET_PUSH_PROMISE));
        pushPromiseHeadersReceived2.add(new H2HeaderField(":authority", "127.0.0.1"));
        pushPromiseHeadersReceived2.add(new H2HeaderField(":scheme", "http"));
        byte[] headerBlockFragment2 = new byte[0];
        FramePushPromiseClient pushPromise2 = new FramePushPromiseClient(1, headerBlockFragment2, 4, 0, true, false, false);
        h2Client.addExpectedFrame(pushPromise2);

        // Headers frame with results from the original request on stream 1
        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient frameHeaders;
        if (USING_NETTY)
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 15, true, true, false, true, false, false);
        else
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        h2Client.addExpectedFrame(frameHeaders);

        // Headers frame with results from pushed resource request on stream 2
        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(2, null, 0, 0, 15, true, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(2, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        // Headers frame with results from pushed resource request on stream 4
        List<H2HeaderField> thirdHeadersReceived = new ArrayList<H2HeaderField>();
        thirdHeadersReceived.add(new H2HeaderField(":status", "200"));
        thirdHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        thirdHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient thirdFrameHeaders;
        if (USING_NETTY)
            thirdFrameHeaders = new FrameHeadersClient(6, null, 0, 0, 15, true, true, false, true, false, false);
        else
            thirdFrameHeaders = new FrameHeadersClient(4, null, 0, 0, 0, true, true, false, false, false, false);
        thirdFrameHeaders.setHeaderFields(thirdHeadersReceived);
        h2Client.addExpectedFrame(thirdFrameHeaders);

        h2Client.sendUpgradeHeader(SERVLET_PUSH_PROMISE + new String("?test=preload"));

        //Since this is a conditional send, this will block the thread until the preface is sent.
        //If the this fails, the test needs to fail as well because the H2 protocol was not established successfully.
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await();
        handleErrors(h2Client, testName);
    }

    public void testPushPromisePushBuilder(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testPushPromisePushBuilder";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "request: " + request);
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "hostName: " + request.getParameter("hostName"));
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "port: " + request.getParameter("port"));
        }
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, defaultTimeoutToSendFrame);

        boolean testFailed = false;
        StringBuilder message = new StringBuilder("The following exceptions were found: ");

        // We should be expecting
        //  some setting frames
        //  a push promise frame on stream 1
        //  a headers frame on stream 1 to satisfy the original request
        //  a headers frame on stream 2 to satisfy the pushed request
        //  a data frame with the test results

        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Setting frame(s) on stream 0
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        // PushPromiseFrame on stream 1
        List<H2HeaderField> pushPromiseHeadersReceived = new ArrayList<H2HeaderField>();
        pushPromiseHeadersReceived.add(new H2HeaderField(":method", "GET"));
        pushPromiseHeadersReceived.add(new H2HeaderField(":path", "/H2TestModule/PushBuilderAPIServlet"));
        pushPromiseHeadersReceived.add(new H2HeaderField(":authority", "127.0.0.1"));
        pushPromiseHeadersReceived.add(new H2HeaderField(":scheme", "http"));
        byte[] headerBlockFragment = new byte[0];
        FramePushPromiseClient pushPromise = new FramePushPromiseClient(1, headerBlockFragment, 2, 0, true, false, false);
        //pushPromise.setHeaderFields(pushPromiseHeadersReceived);
        h2Client.addExpectedFrame(pushPromise);

        // Headers frame with results from the original request on stream 1
        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient frameHeaders;
        if (USING_NETTY)
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 15, true, true, false, true, false, false);
        else
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        h2Client.addExpectedFrame(frameHeaders);

        // Headers frame with results from pushed resource request on stream 2
        List<H2HeaderField> secondHeadersReceived = new ArrayList<H2HeaderField>();
        secondHeadersReceived.add(new H2HeaderField(":status", "200"));
        secondHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        secondHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient secondFrameHeaders;
        if (USING_NETTY)
            secondFrameHeaders = new FrameHeadersClient(2, null, 0, 0, 15, true, true, false, true, false, false);
        else
            secondFrameHeaders = new FrameHeadersClient(2, null, 0, 0, 0, true, true, false, false, false, false);
        secondFrameHeaders.setHeaderFields(secondHeadersReceived);
        h2Client.addExpectedFrame(secondFrameHeaders);

        h2Client.sendUpgradeHeader(SERVLET_PUSH_PROMISE + new String("?test=pushBuilder"));

        //Since this is a conditional send, this will block the thread until the preface is sent.
        //If the this fails, the test needs to fail as well because the H2 protocol was not established successfully.
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await();
        handleErrors(h2Client, testName);
    }

    public void testPushPromiseClientNotEnabledPreload(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testPushPromiseClientNotEnabledPreload";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "request: " + request);
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "hostName: " + request.getParameter("hostName"));
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "port: " + request.getParameter("port"));
        }
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, defaultTimeoutToSendFrame);

        boolean testFailed = false;
        StringBuilder message = new StringBuilder("The following exceptions were found: ");

        // We should be expecting
        //  some setting frames (the one this client sends has the push enabled bit off)
        //  a headers frame on stream 1 to satisfy the original request
        //  no push_promise frame

        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Setting frame(s) on stream 0
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        // Headers frame with results from the original request on stream 1
        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient frameHeaders;
        if (USING_NETTY)
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 15, true, true, false, true, false, false);
        else
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        h2Client.addExpectedFrame(frameHeaders);

        h2Client.sendUpgradeHeader(SERVLET_PUSH_PROMISE + new String("?test=preload"));

        // Create a settings frame with push disabled
        FrameSettings PUSH_DISABLED_SETTINGS_FRAME = new FrameSettings(0, -1, 0, -1, -1, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(PUSH_DISABLED_SETTINGS_FRAME);

        //Use CountDownLatch to block this test thread until we know the test is done (meaning, the connection has been closed)
        blockUntilConnectionIsDone.await();
        handleErrors(h2Client, testName);
    }

    public void testPushPromiseClientNotEnabledPushBuilder(HttpServletRequest request, HttpServletResponse response) throws Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testPushPromiseClientNotEnabledPushBuilder";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "request: " + request);
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "hostName: " + request.getParameter("hostName"));
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "port: " + request.getParameter("port"));
        }
        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, defaultTimeoutToSendFrame);

        boolean testFailed = false;
        StringBuilder message = new StringBuilder("The following exceptions were found: ");

        // We should be expecting
        //  some setting frames (the one this client sends has the push enabled bit off)
        //  a headers frame on stream 1 to satisfy the original request
        //  no push_promise frame

        SimpleDateFormat date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        date.setTimeZone(TimeZone.getTimeZone("GMT"));

        // Setting frame(s) on stream 0
        h2Client.addExpectedFrame(DEFAULT_SERVER_SETTINGS_FRAME);

        // Headers frame with results from the original request on stream 1
        List<H2HeaderField> firstHeadersReceived = new ArrayList<H2HeaderField>();
        firstHeadersReceived.add(new H2HeaderField(":status", "200"));
        firstHeadersReceived.add(new H2HeaderField("x-powered-by", "Servlet/4.0"));
        firstHeadersReceived.add(new H2HeaderField("date", ".*")); //regex because date will vary
        FrameHeadersClient frameHeaders;
        if (USING_NETTY)
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 15, true, true, false, true, false, false);
        else
            frameHeaders = new FrameHeadersClient(1, null, 0, 0, 0, true, true, false, false, false, false);
        frameHeaders.setHeaderFields(firstHeadersReceived);
        h2Client.addExpectedFrame(frameHeaders);

        h2Client.sendUpgradeHeader(SERVLET_PUSH_PROMISE + new String("?test=pushBuilder"));

        // Create a settings frame with push disabled
        FrameSettings PUSH_DISABLED_SETTINGS_FRAME = new FrameSettings(0, -1, 0, -1, -1, -1, -1, false);
        h2Client.sendClientPrefaceFollowedBySettingsFrame(PUSH_DISABLED_SETTINGS_FRAME);

        blockUntilConnectionIsDone.await();
        handleErrors(h2Client, testName);
    }

    public void testClientSendPushPromiseError(HttpServletRequest request, HttpServletResponse response) throws InterruptedException, Exception {
        CountDownLatch blockUntilConnectionIsDone = new CountDownLatch(1);
        String testName = "testClientSendPushPromiseError";
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "request: " + request);
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "hostName: " + request.getParameter("hostName"));
            LOGGER.logp(Level.INFO, this.getClass().getName(), testName, "port: " + request.getParameter("port"));
        }

        Http2Client h2Client = new Http2Client(request.getParameter("hostName"), Integer.parseInt(request.getParameter("port")), blockUntilConnectionIsDone, defaultTimeoutToSendFrame);

        boolean testFailed = false;
        StringBuilder message = new StringBuilder("The following exceptions were found: ");

        // Create a push_promise frame with a stream id of 1, and try to send it
        // Clients are not allowed to send pp frames, this should generate a
        // Connection Error of type Protocol Error

        // Expect a gowaway frame from the server with this error
        // Expect a gowaway frame from the server with this error
        byte[] chfwDebugData = "PUSH_PROMISE Frame Received on server side".getBytes();
        byte[] nettyDebugData = "A client cannot push.".getBytes();
        FrameGoAway errorFrame;
        if (USING_NETTY)
            errorFrame = new FrameGoAway(0, nettyDebugData, PROTOCOL_ERROR, 2147483647, false);
        else
            errorFrame = new FrameGoAway(0, chfwDebugData, PROTOCOL_ERROR, 1, false);
        h2Client.addExpectedFrame(errorFrame);

        List<H2HeaderField> pushPromiseHeaders = new ArrayList<H2HeaderField>();
        pushPromiseHeaders.add(new H2HeaderField(":method", "GET"));
        pushPromiseHeaders.add(new H2HeaderField(":path", "/H2TestModule/H2PushPromise"));
        pushPromiseHeaders.add(new H2HeaderField(":authority", "127.0.0.1"));
        pushPromiseHeaders.add(new H2HeaderField(":scheme", "http"));
        byte[] headerBlockFragment = new byte[0];
        FramePushPromiseClient pushPromise = new FramePushPromiseClient(1, headerBlockFragment, 2, 0, true, false, false);

        h2Client.sendUpgradeHeader(SERVLET_PUSH_PROMISE + new String("?test=delay"));
        h2Client.sendClientPrefaceFollowedBySettingsFrame(EMPTY_SETTINGS_FRAME);

        // Sending push_promise frame should cause an error and a GOAWAY frame
        // should be sent with something about push_promise in it.
        h2Client.sendFrame(pushPromise);

        blockUntilConnectionIsDone.await();
        handleErrors(h2Client, testName);
    }

}
