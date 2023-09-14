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
package com.ibm.ws.http2.test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http2.test.connection.H2Connection;
import com.ibm.ws.http2.test.exceptions.ClientPrefaceTimeoutException;
import com.ibm.ws.http2.test.exceptions.ExpectedPushPromiseDoesNotIncludeLinkHeaderException;
import com.ibm.ws.http2.test.exceptions.FATTimeoutException;
import com.ibm.ws.http2.test.exceptions.StreamDidNotReceivedEndOfStreamException;
import com.ibm.ws.http2.test.exceptions.UnableToSendFrameException;
import com.ibm.ws.http2.test.frames.FrameSettingsClient;
import com.ibm.ws.http2.test.helpers.HTTPUtils;
import com.ibm.ws.http2.test.listeners.FramesListener;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 *
 */
public class Http2Client {

    private H2Connection h2Connection = null;
    private final String hostName;
    private final int httpDefaultPort;
    private final long defaultTimeOutToSendFrame;
    private final CountDownLatch blockUntilConnectionIsDone;
    private final AtomicBoolean isTestDone = new AtomicBoolean(false);
    private final AtomicBoolean didTimeout = new AtomicBoolean(false);
    private final AtomicBoolean lockWaitFor = new AtomicBoolean(true);
    private boolean waitForAck = true;

    private final Map<Frame, Frame> sendFrameConditional = new HashMap<Frame, Frame>();
    private final List<SimpleEntry<Frame, Frame>> sendFrameConditionalList = new LinkedList<AbstractMap.SimpleEntry<Frame, Frame>>();

    private static final String CLASS_NAME = Http2Client.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public Http2Client(String hostName, int httpDefaultPort, CountDownLatch blockUntilConnectionIsDone, long defaultTimeOutToSendFrame) {

        this.hostName = hostName;
        this.httpDefaultPort = httpDefaultPort;
        this.blockUntilConnectionIsDone = blockUntilConnectionIsDone;
        this.defaultTimeOutToSendFrame = defaultTimeOutToSendFrame;

        FATFramesListener framesListener = new FATFramesListener();

        h2Connection = new H2Connection(this.hostName, this.httpDefaultPort, framesListener, blockUntilConnectionIsDone);

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "constructor", "Client " + this + " bound to connection " + h2Connection);
        }

        h2Connection.startAsyncRead();

        TimeoutHelper timeoutHelper = new TimeoutHelper(blockUntilConnectionIsDone, defaultTimeOutToSendFrame);
        timeoutHelper.setPriority(Thread.MIN_PRIORITY);
        timeoutHelper.start();
    }

    /**
     * Create an Http2Client with the option to use HTTP/2 with prior knowledge
     *
     * @param hostName
     * @param httpDefaultPort
     * @param blockUntilConnectionIsDone
     * @param defaultTimeOutToSendFrame
     * @param useHttp2WithPriorKnowledge
     */
    public Http2Client(String hostName, int httpDefaultPort, CountDownLatch blockUntilConnectionIsDone, long defaultTimeOutToSendFrame,
                       boolean useHttp2WithPriorKnowledge) {
        this(hostName, httpDefaultPort, blockUntilConnectionIsDone, defaultTimeOutToSendFrame);
        if (useHttp2WithPriorKnowledge) {
            // tell the connection not to wait for a the 101 switching protocols response since we're not using h2c here
            h2Connection.setServer101ResponseReceived(true);
        }
    }

    /**
     * By default, this Http2Client will wait for ACK responses when frames are sent that require one.
     * Invoking this method disables that behavior.
     */
    public void doNotWaitForAck() {
        waitForAck = false;
    }

    public void sendUpgradeHeader(String requestUri) {
        sendUpgradeHeader(requestUri, HTTPUtils.HTTPMethod.GET, null);
    }

    public void sendUpgradeHeader(String requestUri, HTTPUtils.HTTPMethod httpMethod) {
        sendUpgradeHeader(requestUri, httpMethod, null);
    }

    public void sendUpgradeHeader(String requestUri, HTTPUtils.HTTPMethod httpMethod, String body) {
        sendUpgradeHeader(requestUri, httpMethod, body, new FrameSettingsClient(-1, -1, -1, -1, -1, -1, -1, false));
    }

    public void sendUpgradeHeader(String requestUri, HTTPUtils.HTTPMethod httpMethod, String body, FrameSettingsClient settingsFrame) {
        String h1_upgradeHeader = new String(httpMethod.name() + " " + requestUri + " HTTP/1.1 \r\n" + "Host: " + hostName + ":" + httpDefaultPort
                                             + "\r\nConnection: Upgrade, HTTP2-Settings\r\nUpgrade: h2c\r\nHTTP2-Settings: " + settingsFrame.getBase64UrlPayload() + "\r\n\r\n");

        if (body != null && !body.isEmpty())
            h1_upgradeHeader += body;

        byte[] toSend = h1_upgradeHeader.getBytes();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendUpgradeHeader", "Sending upgrade header (size: " + toSend.length + " bytes)= " + h1_upgradeHeader);
        }

        long bytesWritten = 0L;
        bytesWritten = h2Connection.sendBytesSync(toSend);
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendUpgradeHeader", "Bytes sent: " + bytesWritten);
        }
    }

    private void sendClientPreface() {
        // PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n
        // 0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a
        long bytesWritten = sendClientPreface(utils.CLIENT_PREFACE_STRING_IN_BYTES);

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "Sending client preface (size: " + utils.CLIENT_PREFACE_STRING_IN_BYTES.length + " bytes)= " + bytesWritten);
        }
    }

    private long sendClientPreface(byte[] magicString) {
        // PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n
        // 0x505249202a20485454502f322e300d0a0d0a534d0d0a0d0a

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface",
                        ":Next Frame: :Writing Out: Magic Preface" + " H2Conn hc: " + h2Connection.hashCode() + " size: " + magicString.length);
        }

        long bytesWritten = h2Connection.sendBytesSync(magicString);

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "bytes written: " + bytesWritten);
        }

        return bytesWritten;
    }

    public void sendBytes(byte[] bytes) {
        long bytesWritten = h2Connection.sendBytesSync(bytes);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes", "Sending bytes (size: " + bytes.length + " bytes written)= " + bytesWritten);
        }
    }

    public void sendBytes(WsByteBuffer bytes) {
        long bytesWritten = h2Connection.sendBytesSync(bytes);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes", "Sending bytes (size: " + bytes.limit() + " bytes written)= " + bytesWritten);
        }
    }

    /**
     * Send client preface only if the server sent the 101 response before the timeout.
     *
     * @param timeout How much time to wait for server's 101.
     * @throws Exception Thrown if the server's 101 has not been received in the <i>timeout</i> milliseconds.
     */
    public void sendClientPreface(long timeout) throws ClientPrefaceTimeoutException {
        long startTime = System.currentTimeMillis();
        //loop until the time is over
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "Sending client preface with timeout of: " + timeout);
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "Start time (Millis): " + startTime);
        }
        while ((System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(100);
            } catch (Exception x) {
            }
            if (wasUpgradeHeaderReceived()) {
                sendClientPreface();
                h2Connection.setPrefaceSent(true);
                return;
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendClientPreface", "Unable to send client preface before timing out. Current time (Millis): " + System.currentTimeMillis());
        }
        throw new ClientPrefaceTimeoutException("Timed out while waiting for 101 Response of the server; therefore the client preface was not sent.");
    }

    /**
     * Send client preface only if the server sent the 101 response before the timeout.
     *
     * @param timeout How much time to wait for server's 101.
     * @throws Exception Thrown if the server's 101 has not been received in the <i>timeout</i> milliseconds.
     */
    public void sendClientPreface(String magicString) throws ClientPrefaceTimeoutException {
        long startTime = System.currentTimeMillis();
        //loop until the time is over
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "Sending client with timeout of: " + defaultTimeOutToSendFrame);
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "Start time (Millis): " + startTime);
        }
        while ((System.currentTimeMillis() - startTime) < defaultTimeOutToSendFrame) {
            if (wasUpgradeHeaderReceived()) {
                sendClientPreface(magicString.getBytes());
                return;
            } else {
                try {
                    Thread.sleep(100);
                } catch (Exception x) {

                }
            }
        }
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendClientPreface", "Unable to send client preface before timing out. Current time (Millis): " + System.currentTimeMillis());
        }
        throw new ClientPrefaceTimeoutException("Timed out while waiting for 101 Response of the server; therefore the client preface was not sent.");
    }

    public void sendClientPrefaceFollowedBySettingsFrame(com.ibm.ws.http.channel.h2internal.frames.FrameSettings settingsFrame) throws ClientPrefaceTimeoutException {
        sendClientPrefaceFollowedBySettingsFrame(settingsFrame, defaultTimeOutToSendFrame);
    }

    public void sendClientPrefaceFollowedBySettingsFrame(com.ibm.ws.http.channel.h2internal.frames.FrameSettings settingsFrame, long timeout) throws ClientPrefaceTimeoutException {
        sendClientPreface(timeout);
        try {
            sendFrame(settingsFrame, 0, true);
            h2Connection.setFirstConnectSent(true);
        } catch (UnableToSendFrameException e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME, "sendClientPreface", "caught exception: " + e);
            }
        }
    }

    public long sendFrame(Frame writableFrame) throws UnableToSendFrameException {
        return sendFrame(writableFrame, false);
    }

    public long sendFrame(Frame writableFrame, boolean forced) throws UnableToSendFrameException {
        if (!forced)
            return sendFrame(writableFrame, defaultTimeOutToSendFrame, false);
        else {
            if (writableFrame instanceof FrameData) {
                WsByteBuffer[] bufferArray = ((FrameData) writableFrame).buildFrameArrayForWrite();
                return h2Connection.sendBytesSync(bufferArray);
            } else {

            }
            return h2Connection.sendBytesSync(writableFrame.buildFrameForWrite());
        }
    }

    /**
     * Send bytes iff the server preface has been received.
     *
     * @param byte[]
     * @param timeout -1 if the frame won't be sent.
     * @return
     * @throws Exception
     */
    public long sendBytesAfterPreface(byte[] bytes) throws UnableToSendFrameException {
        long timeout = defaultTimeOutToSendFrame;
        long startTime = System.currentTimeMillis();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendBytesAfterPreface", "Sending bytes with timeout of: " + timeout);
            LOGGER.logp(Level.INFO, CLASS_NAME, "sendBytesAfterPreface", "Start time (Millis): " + startTime);
        }
        //loop until the time is over
        do {
            //if we are waiting for a settings ACK, this will loop
            if (((wasUpgradeHeaderReceived() && wasServerPrefaceReceived())) && !h2Connection.getWaitingForACK().get()) {
                return h2Connection.sendBytesSync(bytes);
            } else {
                try {
                    Thread.sleep(100);
                } catch (Exception x) {

                }
            }
        } while ((System.currentTimeMillis() - startTime) < timeout);
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendBytesAfterPreface", "Unable to send bytes before timing out. Current time (Millis): " + System.currentTimeMillis());
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendBytesAfterPreface", "wasUpgradeHeaderReceived? " + wasUpgradeHeaderReceived());
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendBytesAfterPreface", "wasServerPrefaceReceived? " + wasServerPrefaceReceived());
        }
        if (((wasUpgradeHeaderReceived() && wasServerPrefaceReceived())) && !h2Connection.getWaitingForACK().get()) {
            return h2Connection.sendBytesSync(bytes);
        }
        throw new UnableToSendFrameException("Unable to send bytes becuase upgrade header and server preface have not been received yet. wasUpgradeHeaderReceived() = "
                                             + wasUpgradeHeaderReceived() + " wasServerPrefaceReceived() = " + wasServerPrefaceReceived() + " bytes = " + bytes);
    }

    /**
     * Send a frame iff the server preface has been received.
     *
     * @param writableFrame
     * @param timeout       -1 if the frame won't be sent.
     * @return
     * @throws Exception
     */
    private long sendFrame(Frame writableFrame, long timeout, boolean bypassPreface) throws UnableToSendFrameException {
        long startTime = System.currentTimeMillis();
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendFrame", "Sending frame with timeout of: " + timeout);
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendFrame", "Start time (Millis): " + startTime);
        }
        //loop until the time is over
        do {
            //if we are waiting for a settings ACK, this will loop
            if (((wasUpgradeHeaderReceived() && wasServerPrefaceReceived()) || bypassPreface) && !h2Connection.getWaitingForACK().get()) {
                return h2Connection.sendFrame(writableFrame);
            } else {
                try {
                    Thread.sleep(100);
                } catch (Exception x) {

                }
            }
        } while ((System.currentTimeMillis() - startTime) < timeout);
        if (LOGGER.isLoggable(Level.SEVERE)) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendFrame", "Unable to send frame before timing out. Current time (Millis): " + System.currentTimeMillis());
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendFrame", "wasUpgradeHeaderReceived? " + wasUpgradeHeaderReceived());
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendFrame", "wasServerPrefaceReceived? " + wasServerPrefaceReceived());
            LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendFrame", "bypassPreface? " + bypassPreface);
        }
        if (((wasUpgradeHeaderReceived() && wasServerPrefaceReceived()) || bypassPreface) && !h2Connection.getWaitingForACK().get()) {
            return h2Connection.sendFrame(writableFrame);
        }
        throw new UnableToSendFrameException("Unable to send frame becuase upgrade header and server preface have not been received yet. wasUpgradeHeaderReceived() = "
                                             + wasUpgradeHeaderReceived() + " wasServerPrefaceReceived() = " + wasServerPrefaceReceived() + " Frame = " + writableFrame);
        //return -1;
    }

    public Http2Client waitFor(Frame expectedFrame) {
        //check once if the same already arrived... then we will use the listener to avoid doing the search.
        do {
            if (h2Connection.didFrameArrive(expectedFrame))
                return this;
            LOGGER.logp(Level.FINEST, CLASS_NAME, "waitFor", "looping");
            try {
                Thread.sleep(100);
            } catch (Exception x) {
            }
        } while (lockWaitFor.get() && !didTimeout.get());
        lockWaitFor.set(true); //set to true in case we call waitFor again.
        return this;
    }

    synchronized public void sendUponArrivalCondition(Frame waitForFrame, Frame frameToSend) throws UnableToSendFrameException {
        //if the frame has been received before calling this method, send frameToSend
        LOGGER.logp(Level.FINEST, CLASS_NAME, "sendUponArrivalCondition", "ENTRY");
        try {
            waitForFrame = h2Connection.frameConverter(waitForFrame, false);
        } catch (CompressionException | IOException e) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendUponArrivalCondition", "caught exception: " + e);
        }
        if (h2Connection.didFrameArrive(waitForFrame)) {
            sendFrame(frameToSend);
        }

        LOGGER.logp(Level.FINEST, CLASS_NAME, "sendUponArrivalCondition", "waitForFrame.getClass: " + waitForFrame.getClass());
        sendFrameConditional.put(waitForFrame, frameToSend);
        sendFrameConditionalList.add(new SimpleEntry<Frame, Frame>(waitForFrame, frameToSend));
        LOGGER.logp(Level.FINEST, CLASS_NAME, "sendUponArrivalCondition", "EXIT");
    }

    public void addExpectedFrames(ArrayList<Frame> frames) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {
        h2Connection.addExpectedFrames(frames);
    }

    public H2StreamResult addExpectedFrame(Frame frame) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {
        return h2Connection.addExpectedFrame(frame);
    }

    /**
     * Add an expected Frame Type. When this method is called, only FrameType receipt will be verified on the given stream
     *
     * @param FrameTypes
     * @param stream
     * @return H2StreamResult
     */
    public H2StreamResult addExpectedFrame(FrameTypes type, int stream) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {
        return h2Connection.addExpectedFrame(type, stream);
    }

    public boolean wasUpgradeHeaderReceived() {
        return h2Connection.wasServer101ResponseReceived();
    }

    public boolean wasServerPrefaceReceived() {
        return h2Connection.wasServerFirstConnectReceived();
    }

    public List<Exception> getReportedExceptions() {
        return h2Connection.getReportedExceptions();
    }

    protected boolean isWaitingForAck() {
        return h2Connection.getWaitingForACK().get();
    }

    public class FATFramesListener implements FramesListener {

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.http2.test.listeners.FramesListener#receivedLastFrame()
         */
        @Override
        public void receivedLastFrame(boolean sendGoAway) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME + "$FATFramesListener", "receivedLastFrame", "Received last frame");
            }
            if (sendGoAway) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.logp(Level.INFO, CLASS_NAME + "$FATFramesListener", "receivedLastFrame", "Sending GoAway");
                }
                try {
                    sendFrame(new FrameGoAway(0, new byte[] { (byte) 0, (byte) 1 }, 0, 1, false));
                } catch (UnableToSendFrameException e) {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.logp(Level.INFO, CLASS_NAME + "$FATFramesListener", "receivedLastFrame", "caught exception: " + e);
                    }
                }
                isTestDone.set(true);
                blockUntilConnectionIsDone.countDown();
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.http2.test.listeners.FramesListener#receivedFrameGoAway()
         */
        @Override
        public void receivedFrameGoAway() {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME + "$FATFramesListener", "receivedFrameGoAway",
                            "Received FrameGoAway from server. Calling blockUntilConnectionIsDone.countDown() and 'closing' connection.");
            }
            // this is our best way to predict the test finished
            isTestDone.set(true);
            h2Connection.close();
            // if we've received a GOAWAY from the server, we shouldn't care about incomplete streams
            h2Connection.getReportedExceptions().removeIf(e -> e instanceof StreamDidNotReceivedEndOfStreamException);
            blockUntilConnectionIsDone.countDown();

        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.http2.test.listeners.FramesListener#receivedSettingsAckFrame()
         */
        @Override
        public void receivedSettingsAckFrame() {
            h2Connection.getWaitingForACK().set(false);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.http2.test.listeners.FramesListener#sentSettingsFrame()
         */
        @Override
        public void sentSettingsFrame() {
            if (waitForAck) {
                h2Connection.getWaitingForACK().set(true);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see com.ibm.ws.http2.test.listeners.FramesListener#receivedFrame(com.ibm.ws.http.channel.h2internal.frames.Frame)
         */
        @Override
        public void receivedFrame(Frame receivedFrame) {

            // don't add DATA frames the have "DoNotAdd" as part of the data payload
            if (receivedFrame.getFrameType() == FrameTypes.DATA) {
                FrameData fd = (FrameData) receivedFrame;
                String s = new String(fd.getData());
                if (s.toLowerCase().contains("donotadd")) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.logp(Level.FINEST, CLASS_NAME + "$FATFramesListener", "receivedFrame",
                                    ":Next Frame: :Read In: " + receivedFrame.getFrameType() + " H2Conn hc: " + h2Connection.hashCode() + " DoNotAdd Frame");
                    }
                } else {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.logp(Level.FINEST, CLASS_NAME + "$FATFramesListener", "receivedFrame",
                                    ":Next Frame: :Read In: " + receivedFrame.getFrameType() + " H2Conn hc: " + h2Connection.hashCode() + " " + receivedFrame);
                    }
                }
            } else {

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.logp(Level.FINEST, CLASS_NAME + "$FATFramesListener", "receivedFrame",
                                ":Next Frame: :Read In: " + receivedFrame.getFrameType() + " H2Conn hc: " + h2Connection.hashCode() + " " + receivedFrame);
                }
            }

            for (Iterator<SimpleEntry<Frame, Frame>> iterator = sendFrameConditionalList.iterator(); iterator.hasNext();) {
                SimpleEntry<Frame, Frame> entry = iterator.next();
                if (entry.equals(entry.getKey())) {
                    try {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.logp(Level.FINEST, CLASS_NAME + "$FATFramesListener", "receivedFrame", "Received frame was found in sendFrameConditionalList.");
                        }
                        sendFrame(entry.getValue());
                        iterator.remove();
                        return;
                    } catch (UnableToSendFrameException e) {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.logp(Level.FINEST, CLASS_NAME + "$FATFramesListener", "receivedFrame", "caught exception: " + e);
                        }
                    }
                }
            }
        }
    }

    public class TimeoutHelper extends Thread {

        private final CountDownLatch countDownLatch;
        private final long timeoutToFinishTest;

        /**
         * @param countDownLatch
         * @param timeout
         */
        public TimeoutHelper(CountDownLatch countDownLatch, long timeoutToFinishTest) {
            super();
            this.countDownLatch = countDownLatch;
            this.timeoutToFinishTest = timeoutToFinishTest;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME + "$TimeoutHelper", "run", "Timeout helper started running!");
            }
            long startTime = System.currentTimeMillis();
            //keep looping until timeout or until the test is done
            while ((System.currentTimeMillis() - startTime) < timeoutToFinishTest && !isTestDone.get()) {
                try {
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.logp(Level.INFO, CLASS_NAME + "$TimeoutHelper", "run", "Timeout helper sleeping!");
                    }

                    Thread.sleep(10);
                } catch (Exception x) {
                }
            }
            didTimeout.set(true);
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.logp(Level.INFO, CLASS_NAME + "$TimeoutHelper", "run",
                            "Finished looping because timeout: " + ((System.currentTimeMillis() - startTime) >= timeoutToFinishTest) + " isTestDone.get()= " + isTestDone.get());

            }
            //if the test is not done, make it finish. Otherwise, let the framework handle the end of the test as usual (meaning receivedFrameGoAway() will handle the end of the test).
            if (!isTestDone.get()) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.logp(Level.SEVERE, CLASS_NAME + "$TimeoutHelper", "run", "Terminating test because the timeout was reached.");
                }

                //add a timeout exception to the list of exceptions as the test did not complete on time!
                getReportedExceptions().add(new FATTimeoutException("The test didn't finish. Timeout: " + timeoutToFinishTest));
                h2Connection.close();

                //On timeout, call countDown() to make the test finish.
                countDownLatch.countDown();
            }
        }

    }

}
