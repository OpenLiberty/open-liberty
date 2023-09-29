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
package com.ibm.ws.http2.test.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameState;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameContinuation;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.frames.FramePriority;
import com.ibm.ws.http.channel.h2internal.frames.FramePushPromise;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.frames.FrameWindowUpdate;
import com.ibm.ws.http2.test.CFWManager;
import com.ibm.ws.http2.test.Constants;
import com.ibm.ws.http2.test.H2StreamResult;
import com.ibm.ws.http2.test.H2StreamResultManager;
import com.ibm.ws.http2.test.utils;
import com.ibm.ws.http2.test.exceptions.ConnectionNotClosedAfterGoAwayException;
import com.ibm.ws.http2.test.exceptions.ExpectedPushPromiseDoesNotIncludeLinkHeaderException;
import com.ibm.ws.http2.test.exceptions.ReceivedFrameAfterEndOfStream;
import com.ibm.ws.http2.test.exceptions.ReceivedHeadersFrameAfterEndOfHeaders;
import com.ibm.ws.http2.test.exceptions.ReceivedUnexpectedGoAwayExcetion;
import com.ibm.ws.http2.test.exceptions.UnexpectedUpgradeHeader;
import com.ibm.ws.http2.test.frames.FrameContinuationClient;
import com.ibm.ws.http2.test.frames.FrameDataClient;
import com.ibm.ws.http2.test.frames.FrameGoAwayClient;
import com.ibm.ws.http2.test.frames.FrameHeadersClient;
import com.ibm.ws.http2.test.frames.FramePushPromiseClient;
import com.ibm.ws.http2.test.helpers.H2HeadersUtils;
import com.ibm.ws.http2.test.listeners.FramesListener;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContextFactory;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public class H2Connection {

    private static CFWManager cfwManager = null;
    private WsByteBufferPoolManager bufferMgr = null;
    private TCPConnectRequestContextFactory tcpConnectFactory = null;

    private OutboundVirtualConnection outConn1 = null;
    private TCPConnectRequestContext outReqContext1 = null;
    private TCPConnectionContext outContext1 = null;

    private TCPReadRequestContext readConn = null;
    private TCPWriteRequestContext writeConn = null;

    private HTTP1_1Helper h1_1Helper = null;

    private H2TCPReadCallback h2TcpReadCallback = null;

    private FrameReadProcessor frameReadProcessor = null;
    private final H2StreamResultManager streamResultManager;

    private final H2HeadersUtils headerUtils = new H2HeadersUtils();

    private WsByteBuffer readBuffer = null;
    private WsByteBuffer slicedBuffer = null;

    private boolean server101ResponseReceived = false;
    private boolean serverFirstConnectReceived = false;
    private boolean prefaceSent = false;
    private boolean firstConnectSent = false;

    private final List<Exception> reportedExceptions = Collections.synchronizedList(new ArrayList<Exception>());

    private int pendingBufferStop;
    private int pendingBufferStart;
    private WsByteBuffer[] myPendingBuffers = new WsByteBuffer[10];
    private final int PENDING_BUFFER_MIN_GROWTH_SIZE = 4;

    private final AtomicBoolean waitingForACK = new AtomicBoolean(false);
    private final FrameSettings ackSettingsFrame;

    private boolean closeCalled = false;

    private static String sendBackPriority1 = "SEND.BACK.PRIORITY.1";
    private static String sendBackWinUpdate1 = "SEND.BACK.WINDOW.UPDATE.1";
    private static String sendBackPing1 = "SEND.BACK.PING.1";

    private static final String CLASS_NAME = H2Connection.class.getName();
    private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public H2Connection(String hostName, int httpDefaultPort, FramesListener framesListener, CountDownLatch blockUntilConnectionIsDone) {
        cfwManager = new CFWManager();
        //We just need one chain
        bufferMgr = CFWManager.getWsByteBufferPoolManager();
        tcpConnectFactory = cfwManager.getTCPConnectRequestContextFactory();

        h1_1Helper = new HTTP1_1Helper();

        outConn1 = cfwManager.createOutboundVirtualConnection();
        outReqContext1 = tcpConnectFactory.createTCPConnectRequestContext(hostName, httpDefaultPort, utils.IO_DEFAULT_TIMEOUT);
        outContext1 = cfwManager.connectTCPOutbound(outConn1, outReqContext1);

        readConn = outContext1.getReadInterface();
        writeConn = outContext1.getWriteInterface();

        readBuffer = bufferMgr.allocate(utils.IO_DEFAULT_BUFFER_SIZE);

        readConn.setBuffer(readBuffer);

        h2TcpReadCallback = new H2TCPReadCallback(this);
        frameReadProcessor = new FrameReadProcessor(null);
        frameReadProcessor.setFrameState(FrameState.INIT);

        streamResultManager = new H2StreamResultManager(this);
        streamResultManager.setFramesListener(framesListener);

        ackSettingsFrame = new FrameSettings();
        ackSettingsFrame.setAckFlag();

    }

    public synchronized long sendBytesSync(WsByteBuffer[] toSend) {
        return (sendBytes(toSend));
    }

    private long sendBytes(WsByteBuffer[] toSend) {
        writeConn.setBuffers(toSend);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes(WsByteBuffer[])", "Sending " + toSend.length + " buffers synchronously through connection " + this + ".");
        }

        long bytesWritten = 0L;
        try {
            bytesWritten = writeConn.write(TCPWriteRequestContext.WRITE_ALL_DATA, utils.IO_DEFAULT_TIMEOUT);
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendBytes(WsByteBuffer[])", "Unable to send bytes: ", e);
            }
            reportedExceptions.add(e);
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes(WsByteBuffer[])", bytesWritten + " bytes sent synchronously.");
        }
        writeConn.clearBuffers();
        return bytesWritten;
    }

    public synchronized long sendBytesSync(WsByteBuffer toSend) {
        return (sendBytes(toSend));
    }

    private long sendBytes(WsByteBuffer toSend) {
        writeConn.setBuffer(toSend);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes(WsByteBuffer)", "Sending " + toSend.limit() + " bytes synchronously through connection " + this + ".");
        }

        long bytesWritten = 0L;
        try {
            bytesWritten = writeConn.write(TCPWriteRequestContext.WRITE_ALL_DATA, utils.IO_DEFAULT_TIMEOUT);
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendBytes(WsByteBuffer)", "Unable to send bytes: ", e);
            }
            reportedExceptions.add(e);
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes(WsByteBuffer)", bytesWritten + " bytes sent synchronously.");
        }
        writeConn.clearBuffers();
        return bytesWritten;
    }

    public synchronized long sendBytesSync(byte[] toSend) {
        return (sendBytes(toSend));
    }

    private long sendBytes(byte[] toSend) {
        WsByteBuffer writeBuffer = bufferMgr.allocate(toSend.length);
        writeBuffer.put(toSend);
        writeBuffer.position(0);
        writeBuffer.limit(toSend.length);
        writeConn.setBuffer(writeBuffer);

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes", "Sending " + toSend.length + " bytes synchronously through connection " + this + ".");
        }

        long bytesWritten = 0L;
        try {
            bytesWritten = writeConn.write(TCPWriteRequestContext.WRITE_ALL_DATA, utils.IO_DEFAULT_TIMEOUT);
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendBytes", "Unable to send bytes: ", e);
            }
            reportedExceptions.add(e);
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendBytes", bytesWritten + " bytes sent synchronously.");
        }
        writeConn.clearBuffers();
        return bytesWritten;
    }

    /**
     *
     * If the H2 preface has completed, this will send frame async, otherwise, sync and blocking.
     *
     * @param writableFrame
     * @return -2 if it we are writing asynchronously. Otherwise, the amount of the bytes written.
     */
    public synchronized long sendFrame(Frame writableFrame) {

        // synchronized to protect access to at least the pending buffers logic, and maybe other stuff.

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendFrame", "Sending frame: (connection: " + this + ")");
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendFrame",
                        ":Next Frame: :Writing Out: " + writableFrame.getFrameType() + " H2Conn hc: " + this.hashCode() + " " + writableFrame.toString());
        }
        processFrame(writableFrame);
        try {
            writableFrame = streamResultManager.processFrame(writableFrame, true, false);
        } catch (CompressionException | IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.logp(Level.SEVERE, CLASS_NAME, "sendFrame", "Exception: ", e);
            }
            reportedExceptions.add(e);
        }

        if (wasServer101ResponseReceived() && wasServerFirstConnectReceived()) {

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, "sendFrame", "Writing frame synchronously.");
            }
            if (writableFrame instanceof FrameData || writableFrame instanceof FrameDataClient) {
                WsByteBuffer[] bufferArray = ((FrameData) writableFrame).buildFrameArrayForWrite();
                addToPendingByteBuffer(bufferArray, bufferArray.length);
            } else {
                addToPendingByteBuffer(writableFrame.buildFrameForWrite(), 1);
            }
            syncWrite();

            // Means that we write all data, kind of loss it's meaning once we made all writes sync.
            return -1;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "sendFrame", "Writing frame synchronously.");
        }
        return sendBytes(writableFrame.buildFrameForWrite());
    }

    public WsByteBuffer[] frameBytesToWsByteBuffer(byte[] frame) {
        WsByteBuffer[] frameByteBuffer = new WsByteBuffer[1];
        frameByteBuffer[0] = bufferMgr.allocate(frame.length);
        WsByteBufferUtils.putByteArrayValue(frameByteBuffer, frame, true);
        return frameByteBuffer;
    }

    public long processRead(int timeout) {

        WsByteBuffer readBuffer = bufferMgr.allocate(utils.IO_DEFAULT_BUFFER_SIZE);
        readConn.setBuffer(readBuffer);

        long bytesRead = 0L;
        try {
            bytesRead = readConn.read(1, timeout);
        } catch (IOException e) {
            bytesRead = -1L;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "processRead", "Reading synchronously. Bytes read: " + bytesRead);
        }

        return bytesRead;
    }

    public long processRead() {
        return processRead(utils.IO_DEFAULT_TIMEOUT);
    }

    public void startAsyncRead() {

        if (closeCalled)
            return;

//        if (wasServer101ResponseReceived()) {
//            int count = 0;
//            // don't read until we have sent the client preface
//            // wait up to 5 seconds for preface to be received after the 101 is received, before reading for the first Settings frame
//            while ((!getPrefaceSent() && count < 50)) {
//                count++;
//                try {
//                    Thread.sleep(10);
//                } catch (Exception x) {
//                }
//            }
//        }

        if (slicedBuffer == null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, "startAsyncRead", "Allocating a new buffer for and calling TCPChannel read");
            }
            WsByteBuffer readBuffer = bufferMgr.allocate(utils.IO_DEFAULT_BUFFER_SIZE);

            readConn.setBuffer(readBuffer);

            readConn.read(1, h2TcpReadCallback, true, utils.IO_DEFAULT_TIMEOUT);
        } else {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, "startAsyncRead", "Using slice buffer and calling complete on the callback in connection " + this + ".");
            }
            // need to process end of the last buffer instead of reading for a new buffer
            // call the callback complete to allow the read logic path to execute in full
            // the complete will execute serially on this thread
            slicedBuffer.position(slicedBuffer.limit());
            readConn.setBuffer(slicedBuffer);
            slicedBuffer = null;
            h2TcpReadCallback.complete(null, readConn);
            // the complete will execute serially on this thread - so no more logic after this
        }

    }

    // can not do concurrent writes on the same TCP Channel connection, therfore this is synchronized
    public synchronized void syncWrite() {
        WsByteBuffer[] writeBuffers = getBuffList();
        if (null != writeBuffers) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, "syncWrite", "Writing (sync) " + writeBuffers.length + " buffers in connection " + this + ".");
            }
            // make this a sync write, at least for now
            writeConn.setBuffers(writeBuffers);
            try {
                writeConn.write(TCPWriteRequestContext.WRITE_ALL_DATA, utils.IO_DEFAULT_TIMEOUT);
            } catch (IOException e) {
                // sync write failed
            }
        }
    }

    /**
     * Collect all the buffers that need to be written out this call.
     *
     * @return WsByteBuffer[] (null if there are no buffers)
     */
    protected WsByteBuffer[] getBuffList() {

        int size = this.pendingBufferStop - this.pendingBufferStart;
        if (0 == size) {
            return null;
        }
        WsByteBuffer[] list = new WsByteBuffer[size];
        System.arraycopy(this.myPendingBuffers, this.pendingBufferStart, list, 0, size);
        clearPendingByteBuffers();
        //now we will start where the last buffer was processed/written
        return list;
    }

    /**
     * Add the list of outgoing buffers, stopping at the input length of
     * that list.
     *
     * @param list
     * @param length
     */
    private void addToPendingByteBuffer(WsByteBuffer[] list, int length) {
        int newsize = this.pendingBufferStop + length;
        if (newsize >= this.myPendingBuffers.length) {
            if (length < PENDING_BUFFER_MIN_GROWTH_SIZE) {
                newsize = this.myPendingBuffers.length + PENDING_BUFFER_MIN_GROWTH_SIZE;
            }
            growPendingArray(newsize);
        }
        System.arraycopy(list, 0, this.myPendingBuffers, this.pendingBufferStop, length);
        this.pendingBufferStop += length;
    }

    /**
     * Add a single outgoing buffer
     *
     * @param WsByteBuffer
     * @param length
     */
    private void addToPendingByteBuffer(WsByteBuffer buf, int length) {
        int newsize = this.pendingBufferStop + length;
        if (newsize >= this.myPendingBuffers.length) {
            if (length < PENDING_BUFFER_MIN_GROWTH_SIZE) {
                newsize = this.myPendingBuffers.length + PENDING_BUFFER_MIN_GROWTH_SIZE;
            }
            growPendingArray(newsize);
        }
        myPendingBuffers[pendingBufferStop] = buf;
        this.pendingBufferStop += length;
    }

    /**
     * Grow and copy the existing the pending output list of buffers to the new
     * input size.
     *
     * @param size
     */
    private void growPendingArray(int size) {
        WsByteBuffer[] tempNew = new WsByteBuffer[size];
        System.arraycopy(this.myPendingBuffers, 0, tempNew, 0, this.pendingBufferStop);
        this.myPendingBuffers = tempNew;
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "growPendingArray", "Increased pending list to " + size);
        }
    }

    /**
     * Clear the array of pending byte buffers.
     *
     */
    private void clearPendingByteBuffers() {

        for (int i = 0; i < this.pendingBufferStop; i++) {
            this.myPendingBuffers[i] = null;
        }
        this.pendingBufferStart = 0;
        this.pendingBufferStop = 0;
    }

    public void processData() {
        WsByteBuffer currentBuffer = readConn.getBuffer();

        currentBuffer.flip();

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "There are " + currentBuffer.limit() + " bytes in this buffer (connection " + this + ").");
        }

        int frameReadStatus = 0;

        boolean server101ResponseReceived = wasServer101ResponseReceived();

        try {
            if (server101ResponseReceived) {

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "currentBuffer hc: " + currentBuffer.hashCode()
                                                                         + " position: " + currentBuffer.position() + " limit: " + currentBuffer.limit());
                }

                frameReadStatus = frameReadProcessor.processNextBuffer(currentBuffer);

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "after calling processNextBuffer, frameReadStatus: " + frameReadStatus);
                }

                if (frameReadStatus != Constants.BP_FRAME_IS_NOT_COMPLETE) {

                    // buffer frame has been read in, position is at the start of the payload
                    // we'll go ahead and set up the frame and process the payload
                    com.ibm.ws.http.channel.h2internal.frames.Frame currentFrame = frameReadProcessor.getCurrentFrame();

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "Processing frame object: currentFrame hc: " + currentFrame.hashCode()
                                                                             + " CurrentFrame toString: \n" + currentFrame.getFrameType());
                    }

                    if (!wasServerFirstConnectReceived()) {
                        if (currentFrame.getFrameType() == FrameTypes.SETTINGS) {
                            if (LOGGER.isLoggable(Level.FINEST)) {
                                LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "Settings frame received for the first time.");
                            }
                            setServerFirstConnectReceived(true);
                        } else {
                            if (LOGGER.isLoggable(Level.SEVERE)) {
                                LOGGER.logp(Level.SEVERE, CLASS_NAME, "processData", "The first frame sent by the server was not a Settings frame.");
                            }
                            reportedExceptions.add(new Exception("The first frame sent by the server was not a Settings frame."));
                        }
                    }

                    //Can't use frameReadProcessor.processCompleteFrame here because we don't use H2InboundLink in the test code
                    currentFrame.processPayload(frameReadProcessor);
                    currentFrame.validate(new H2ConnectionSettings());

                    if (frameReadStatus > 0) {
                        slicedBuffer = currentBuffer.slice();
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "Buffer has been sliced, it has " + slicedBuffer.limit() + " bytes.");
                        }
                    }

                    processFrame(currentFrame);

                    streamResultManager.addResponseFrame(currentFrame);

                    // send back a frame if Data Payload says to do so
                    testFrameForSendBack(currentFrame);

                }
            } else {
                //means we are still reading HTTP 1.1 responses (like the Upgrade 101 response, or something else if not H2 is not supported)
                // since we won't send the magic/preface until reading in the 101 response,
                // and since the server won't send the first SETTINGS frame until the server reads the magic/preface
                // we are safe to assume that not HTTP/2 frames will be attached to the 101 response, so don't worry about any following bytes
                // when reading from the 101 response
                h1_1Helper.storeH1Response(currentBuffer);
            }
        } catch (UnexpectedUpgradeHeader | Http2Exception | IOException | ReceivedFrameAfterEndOfStream
                        | ReceivedHeadersFrameAfterEndOfHeaders | ReceivedUnexpectedGoAwayExcetion e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.logp(Level.SEVERE, CLASS_NAME, "processData", "Exception reported while processing frame: ", e);
            }
            reportedExceptions.add(e);
        } finally {
            //Reset frame read processor here if a complete frame was processed
            if (server101ResponseReceived && frameReadStatus != Constants.BP_FRAME_IS_NOT_COMPLETE) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "Calling frameReadProcessor.reset(true) on frameReadProcessor: " + frameReadProcessor);
                }
                frameReadProcessor.reset(true);
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, "processData", "Calling startAsyncRead()");
        }
        startAsyncRead();
    }

    private void testFrameForSendBack(com.ibm.ws.http.channel.h2internal.frames.Frame f) {
        if (f.getFrameType() == FrameTypes.DATA) {
            FrameData fd = (FrameData) f;

            String s = new String(fd.getData());
            if (s.compareTo(sendBackPriority1) == 0) {
                System.out.println("FOUND: " + sendBackPriority1);

                int streamId = fd.getStreamId();
                int streamDep = 0;
                int weight = 64;
                boolean exclusive = false;
                boolean reserveBit = false;

                FramePriority fp = new FramePriority(streamId, streamDep, weight, exclusive, reserveBit);
                sendBytes(fp.buildFrameForWrite());
            }

            if (s.contains(sendBackWinUpdate1)) {
                //too verbose System.out.println("FOUND: " + sendBackWinUpdate1);

                int streamId = fd.getStreamId();

                //need a big size for large stress testing
                int windowSizeIncrement = Constants.STRESS_WINDOW_UPDATE_STREAM_INC;

                boolean reserveBit = false;

                FrameWindowUpdate fw = new FrameWindowUpdate(streamId, windowSizeIncrement, reserveBit);

                LOGGER.logp(Level.FINEST, CLASS_NAME, "testFrameForSendBack",
                            ":Next Frame: :Writing Out: " + fw.getFrameType() + " H2Conn hc: " + this.hashCode() + " " + fw.toString());

                sendBytes(fw.buildFrameForWrite());
            }

            if (s.contains(sendBackPing1)) {
                System.out.println("FOUND: " + sendBackPing1);

                int streamId = 0; // ping has to be on stream 0 - good test to do it on a different stream soon.

                FramePing fp = new FramePing(streamId, null, false);
                LOGGER.logp(Level.FINEST, CLASS_NAME, "testFrameForSendBack",
                            ":Next Frame: :Writing Out: " + fp.getFrameType() + " H2Conn hc: " + this.hashCode() + " " + fp.toString());
                sendBytes(fp.buildFrameForWrite());
            }
        }
    }

    /**
     * Depending on the kind of frame and its values, modify the connection or streams.
     *
     * @param frameToProcess
     */
    private void processFrame(Frame frameToProcess) {
        if (frameToProcess.isWriteFrame()) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "Processing sent frame: " + frameToProcess.getFrameType());
            }
            if (frameToProcess.getFrameType() == FrameTypes.GOAWAY) {
                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "GoAway frame sent.");
                }
            }
        } else {
            // don't log/trace frame here, caller just did
            if (frameToProcess.getFrameType() == FrameTypes.SETTINGS) {
                if (frameToProcess.flagAckSet()) {
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "Received a Settings frame with ack flag on.");
                } else {
                    //We need to send an empty SETTINGS frame acknowledging that we received a Settings frame.
                    if (LOGGER.isLoggable(Level.FINEST))
                        LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "Received a Settings frame with ack flag off. Sending ack settings frame.");
                    // make sure we have first sent the empty/noAck settings frame before sending this one, wait up to 5 seconds
                    int count = 0;
                    while ((getFirstConnectSent() == false) && (count < 50)) {
                        count++;
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                        }
                    }
                    sendFrame(ackSettingsFrame);
                }
            }
        }
    }

    public Frame frameConverter(Frame frameToConvert) throws CompressionException, IOException {
        return frameConverter(frameToConvert, false);
    }

    /**
     * This method is intended to convert Frames into stuff we can compare more easily. Meaning, we will try
     * to avoid comparing bytes and use simple data structures instead.
     *
     * The method does convert: FrameHeaders.
     *
     * @return a new converted frame (if possible).
     * @throws IOException
     * @throws CompressionException
     */
    public Frame frameConverter(Frame frameToConvert, boolean isExpectedFrame) throws CompressionException, IOException {
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "framerToConvert.getClass(): " + frameToConvert.getClass());
        if (!isExpectedFrame) {
            if (frameToConvert.getClass().isAssignableFrom(FrameGoAway.class)) {
                FrameGoAway frameGoawayToConvert = ((FrameGoAway) frameToConvert);
                FrameGoAwayClient goAway = new FrameGoAwayClient(frameGoawayToConvert.getStreamId(), frameGoawayToConvert.getDebugData(), new int[] { frameGoawayToConvert.getErrorCode() }, new int[] { frameGoawayToConvert.getLastStreamId() });
                return goAway;
            } else if (frameToConvert.getClass().isAssignableFrom(FrameHeaders.class)) {
                FrameHeaders frameHeadersToConvert = ((FrameHeaders) frameToConvert);
                byte[] headerBlockFragment = frameHeadersToConvert.getHeaderBlockFragment();
                FrameHeadersClient testFrameHeaders = new FrameHeadersClient(frameHeadersToConvert.getStreamId(), headerBlockFragment, frameHeadersToConvert.getStreamDependency(), frameHeadersToConvert.getPaddingLength(), frameHeadersToConvert.getWeight(), frameHeadersToConvert.flagEndStreamSet(), frameHeadersToConvert.flagEndHeadersSet(), frameHeadersToConvert.flagPaddingSet(), frameHeadersToConvert.flagPrioritySet(), frameHeadersToConvert.isExclusive(), frameHeadersToConvert.getFrameReserveBit());
                testFrameHeaders.setHeaderFields(getHeadersUtils().decodeHeaders(headerBlockFragment));
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "testFrameHeaders: " + testFrameHeaders);
                return testFrameHeaders;
            } else if (frameToConvert.getClass().isAssignableFrom(FrameHeadersClient.class)) { //we need to convert this to internal FrameHeaders as this is what we will send through the wire
                FrameHeadersClient frameHeadersToConvert = (FrameHeadersClient) frameToConvert;
                byte[] headerBlockFragment = getHeadersUtils().encodeHeaders(frameHeadersToConvert.getHeaderEntries());
                FrameHeaders internalFrameHeadersToConvert = new FrameHeaders(frameHeadersToConvert.getStreamId(), headerBlockFragment, frameHeadersToConvert.getStreamDependency(), frameHeadersToConvert.getPaddingLength(), frameHeadersToConvert.getWeight(), frameHeadersToConvert.flagEndStreamSet(), frameHeadersToConvert.flagEndHeadersSet(), frameHeadersToConvert.flagPaddingSet(), frameHeadersToConvert.flagPrioritySet(), frameHeadersToConvert.isExclusive(), frameHeadersToConvert.getFrameReserveBit());
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "internalFrameHeadersToConvert: " + internalFrameHeadersToConvert);
                return internalFrameHeadersToConvert; //This one need to be converted to a test FrameHeaders
            }

            if (frameToConvert.getClass().isAssignableFrom(FrameContinuation.class)) {
                FrameContinuation frameContinuationToConvert = ((FrameContinuation) frameToConvert);
                byte[] headerBlockFragment = frameContinuationToConvert.getHeaderBlockFragment();
                FrameContinuationClient testFrameContinuation = new FrameContinuationClient(frameContinuationToConvert.getStreamId(), headerBlockFragment, frameContinuationToConvert.flagEndHeadersSet(), frameContinuationToConvert.flagEndStreamSet(), frameContinuationToConvert.getFrameReserveBit());
                testFrameContinuation.setHeaderFields(getHeadersUtils().decodeHeaders(headerBlockFragment));
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "testFrameContinuation: " + testFrameContinuation);
                return testFrameContinuation;
            } else if (frameToConvert.getClass().isAssignableFrom(FrameContinuationClient.class)) { //we need to convert this to internal FrameHeaders as this is what we will send through the wire
                FrameContinuationClient frameContinuationToConvert = (FrameContinuationClient) frameToConvert;
                byte[] headerBlockFragment = getHeadersUtils().encodeHeaders(frameContinuationToConvert.getHeaderEntries());
                FrameContinuation internalFrameContinuationToConvert = new FrameContinuation(frameContinuationToConvert.getStreamId(), headerBlockFragment, frameContinuationToConvert.flagEndHeadersSet(), frameContinuationToConvert.flagEndStreamSet(), frameContinuationToConvert.getFrameReserveBit());
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "internalFrameContinuationToConvert: " + internalFrameContinuationToConvert);
                return internalFrameContinuationToConvert; //This one need to be converted to a test FrameHeaders
            }

            if (frameToConvert.getClass().isAssignableFrom(FramePushPromise.class)) {
                FramePushPromise frameContinuationToConvert = ((FramePushPromise) frameToConvert);
                byte[] headerBlockFragment = frameContinuationToConvert.getHeaderBlockFragment();
                FramePushPromiseClient testFramePushPromise = new FramePushPromiseClient(frameContinuationToConvert.getStreamId(), headerBlockFragment, frameContinuationToConvert.getPromisedStreamId(), frameContinuationToConvert.getPaddingLength(), frameContinuationToConvert.flagEndHeadersSet(), frameContinuationToConvert.flagPaddedSet(), frameContinuationToConvert.getFrameReserveBit());
                testFramePushPromise.setHeaderFields(getHeadersUtils().decodeHeaders(headerBlockFragment));
                if (LOGGER.isLoggable(Level.FINEST))
                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "testFramePushPromise: " + testFramePushPromise);
                return testFramePushPromise;
            } //we won't be sending push promise frames; therefore, we don't need to convert from test to actual frame

            if (frameToConvert.getClass().isAssignableFrom(FrameData.class)) {
                FrameData frameDataToConvert = ((FrameData) frameToConvert);
                //byte[] headerBlockFragment = frameContinuationToConvert.getHeaderBlockFragment();
                FrameDataClient testFrameData = new FrameDataClient(frameDataToConvert.getStreamId(), frameDataToConvert.getData(), frameDataToConvert.getPaddingLength(), frameDataToConvert.flagEndStreamSet(), frameDataToConvert.flagPaddedSet(), frameDataToConvert.getFrameReserveBit());
                if (LOGGER.isLoggable(Level.FINEST)) {
                    // don't add DATA frames the have "DoNotAdd" as part of the data payload
                    if (testFrameData.getFrameType() == FrameTypes.DATA) {
                        FrameData fd = testFrameData;
                        String s = new String(fd.getData());
                        if (s.toLowerCase().contains("donotadd")) {
                            LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "testFrameData: " + "DoNotAdd Data Frame");
                            return testFrameData;
                        }
                    }

                    LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "testFrameData: " + testFrameData.getFrameType());
                }

                return testFrameData;
            }
        } else {

        }

        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "We didn't convert frame: " + frameToConvert.getFrameType());

        return frameToConvert;
    }

    public void close() {
        closeCalled = true;
        if (LOGGER.isLoggable(Level.FINEST))
            LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "Connection close() called in connection " + this + ".");
        //Channel wants to be one to close the connection.
        reportedExceptions.addAll(streamResultManager.compareAllStreamResults());

        if (processRead(1000) >= 0) {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "Server has not closed the connection yet. Checking again in 2 seconds.");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        if (processRead(1000) >= 0) {
            reportedExceptions.add(new ConnectionNotClosedAfterGoAwayException("Connection has not been closed by the server after it sent GOAWAY frame"));
        }
    }

    public boolean wasServer101ResponseReceived() {
        return server101ResponseReceived;
    }

    public void setServer101ResponseReceived(boolean received) {
        this.server101ResponseReceived = received;
    }

    public void setPrefaceSent(boolean x) {
        prefaceSent = true;
    }

    public boolean getPrefaceSent() {
        return prefaceSent;
    }

    public void setFirstConnectSent(boolean x) {
        firstConnectSent = true;
    }

    public boolean getFirstConnectSent() {
        return firstConnectSent;
    }

    public boolean wasServerFirstConnectReceived() {
        return serverFirstConnectReceived;
    }

    public void setServerFirstConnectReceived(boolean received) {
        this.serverFirstConnectReceived = received;
    }

    public void addExpectedFrames(ArrayList<Frame> frames) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {
        streamResultManager.addExpectedFrames(frames);
    }

    public H2StreamResult addExpectedFrame(Frame frame) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {
        return streamResultManager.addExpectedFrame(frame);
    }

    public H2StreamResult addExpectedFrame(FrameTypes type, int stream) throws CompressionException, IOException, ExpectedPushPromiseDoesNotIncludeLinkHeaderException {
        return streamResultManager.addExpectedFrame(type, stream);
    }

    public H2HeadersUtils getHeadersUtils() {
        return headerUtils;
    }

    public List<Exception> getReportedExceptions() {
        return reportedExceptions;
    }

    private class HTTP1_1Helper {
        //List<String> http1_1resposes = new ArrayList<String>();
        StringBuilder response = new StringBuilder();

        public void storeH1Response(WsByteBuffer buffer) throws UnexpectedUpgradeHeader {
            if (LOGGER.isLoggable(Level.FINEST))
                LOGGER.logp(Level.FINEST, CLASS_NAME, "processFrame", "HTTP 1.1 read: " + utils.printByteArrayWithHex(buffer.array(), buffer.limit()));
            response.append(utils.printByteArrayWithHex(buffer.array(), buffer.limit()));
            checkResponseHeaders(response.toString());
        }

        /*
         * HTTP/1.1 101 Switching Protocols<CR>
         * <LF>X-Powered-By: Servlet/3.1<CR>
         * <LF>Upgrade: h2c<CR>
         * <LF>Connection: Upgrade<CR>
         * <LF>Content-Length: 0<CR>
         * <LF>Date: Thu, 09 Mar 2017 04:29:00 GMT<CR>
         * <LF><CR>
         * <LF>
         */
        private void checkResponseHeaders(String responseHeaders) throws UnexpectedUpgradeHeader {
            String[] headerNamesAndValues;
            if (responseHeaders.contains("<CR>\n<LF><CR>\n<LF>")) { //we have the H1.1 headers at this point
                headerNamesAndValues = responseHeaders.split("<CR>\n<LF>");
                boolean switchingProtocols = false, upgradeH2c = false, connectionUpgrade = false;
                for (String header_value : headerNamesAndValues) {
                    if (header_value.equalsIgnoreCase("HTTP/1.1 101 Switching Protocols")) {
                        switchingProtocols = true;
                    } else if (header_value.equalsIgnoreCase("Upgrade: h2c")) {
                        upgradeH2c = true;
                    } else if (header_value.equalsIgnoreCase("Connection: Upgrade")) {
                        connectionUpgrade = true;
                    }
                }
                if (switchingProtocols && upgradeH2c && connectionUpgrade)
                    setServer101ResponseReceived(true);
                else
                    throw new UnexpectedUpgradeHeader(responseHeaders);
            }
        }

    }

    /**
     * @param expectedFrame
     * @return
     */
    public boolean didFrameArrive(Frame expectedFrame) {
        return streamResultManager.didframeArrive(expectedFrame);
    }

    public boolean receivedAllFrames() {
        return streamResultManager.receivedAllFrames();
    }

    public AtomicBoolean getWaitingForACK() {
        return this.waitingForACK;
    }
}