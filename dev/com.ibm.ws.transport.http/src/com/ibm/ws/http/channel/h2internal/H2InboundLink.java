/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.priority.Node;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannel;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public class H2InboundLink extends HttpInboundLink {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2InboundLink.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static enum LINK_STATUS {
        INIT, OPEN, WAIT_TO_SEND_GOAWAY, GOAWAY_SENDING, CLOSING
    };

    public static enum READ_LINK_STATUS {
        NOT_READING, READ_OUTSTANDING, PROCESSING_READ
    };

    public static enum WRITE_LINK_STATUS {
        NOT_WRITING, WRITE_IN_PROGRESS
    };

    // Note - the following objects should only be accessed and examined while holding the linkStatusSync lock
    LINK_STATUS linkStatus = LINK_STATUS.INIT;
    READ_LINK_STATUS readLinkStatus = READ_LINK_STATUS.NOT_READING;
    WRITE_LINK_STATUS writeLinkStatus = WRITE_LINK_STATUS.NOT_WRITING;
    private ScheduledFuture<?> closeFuture = null;
    private H2ConnectionTimeout connTimeout = null;
    Object linkStatusSync = new Object() {};

    // keep track of the highest IDs processed
    private int highestClientStreamId = 0;
    private int highestLocalStreamId = -1; // this moves to 0 when the connection stream is established

    boolean connection_preface_sent = false; // empty SETTINGS frame has been sent
    boolean connection_preface_string_rcvd = false; // MAGIC string has been received
    boolean connection_preface_settings_rcvd = false; // empty SETTINGS frame has been received
    public volatile boolean connection_preface_settings_ack_rcvd = false; // our empty SETTINGS frame has been ACK'd
    public volatile boolean connection_init_failed = false; // the connection initialization failed

    volatile long initialWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE;
    volatile long connectionReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE; // keep track of how much data the client is allowed to send to the us
    volatile long maxReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE; // user-set max window size

    FrameReadProcessor frameReadProcessor = null;

    H2MuxTCPReadCallback h2MuxReadCallback = null;
    TCPReadRequestContext h2MuxTCPReadContext = null;

    H2MuxTCPWriteCallback h2MuxWriteCallback = null;
    TCPWriteRequestContext h2MuxTCPWriteContext = null;

    WsByteBuffer slicedBuffer = null;

    ItemForCompletion readWaitingForCompletion = new ItemForCompletion();
    ItemForCompletion writeWaitingForCompletion = new ItemForCompletion();

    ConcurrentHashMap<Integer, H2StreamProcessor> streamTable = new ConcurrentHashMap<Integer, H2StreamProcessor>();

    ConcurrentHashMap<Integer, H2StreamProcessor> closeTable = new ConcurrentHashMap<Integer, H2StreamProcessor>();
    private static long CLOSE_TABLE_PURGE_TIME = 30L * 1000000000L; // 30 seconds converted to nano-seconds

    HttpInboundLink initialHttpInboundLink = null;
    VirtualConnection initialVC = null;
    HttpInboundChannel httpInboundChannel = null;
    TCPConnectionContext h2MuxTCPConnectionContext = null;
    HttpInboundServiceContextImpl h2MuxServiceContextImpl = null;

    H2ConnectionSettings connectionSettings;
    H2WorkQInterface writeQ = null;

    int h2NextPromisedStreamId = 0;

    private H2HeaderTable readContextTable = null;
    private H2HeaderTable writeContextTable = null;

    HttpChannelConfig config = null;

    private int readStackDepthCount = 0;
    private final static int READ_STACK_DEPTH_LIMIT = 64;

    int hcDebug = 0x0;

    private boolean continuationFrameExpected = false;

    public boolean isContinuationExpected() {
        return continuationFrameExpected;
    }

    public void setContinuationExpected(boolean expected) {
        this.continuationFrameExpected = expected;
    }

    public H2InboundLink(HttpInboundChannel channel, VirtualConnection vc, TCPConnectionContext tcc) {
        super(channel, vc);

        initialVC = vc;
        httpInboundChannel = channel;

        frameReadProcessor = new FrameReadProcessor(this);
        h2MuxReadCallback = new H2MuxTCPReadCallback();
        h2MuxWriteCallback = new H2MuxTCPWriteCallback();
        h2MuxReadCallback.setConnLinkCallback(this);
        h2MuxTCPConnectionContext = tcc;
        h2MuxTCPReadContext = tcc.getReadInterface();
        h2MuxTCPWriteContext = tcc.getWriteInterface();
        connectionSettings = new H2ConnectionSettings();
        config = channel.getHttpConfig();

        // set up the initial connection read window size
        maxReadWindowSize = config.getH2ConnReadWindowSize();
        connectionReadWindowSize = maxReadWindowSize;

        // decide which Q class to use by hand here, for now
        //writeQ = new H2WriteQ();
        writeQ = new H2WriteTree();

        writeQ.init(h2MuxTCPWriteContext, h2MuxWriteCallback);

        readContextTable = new H2HeaderTable();
        writeContextTable = new H2HeaderTable();

        hcDebug = this.hashCode();

    }

    public synchronized long getInitialWindowSize() {
        return initialWindowSize;
    }

    public H2StreamProcessor createNewInboundLink(Integer streamID) {
        if ((streamID & 1) == 0) { // even number, server-initialized stream
            if (streamID > highestLocalStreamId) {
                highestLocalStreamId = streamID;
            }
        } else { // client-initialized stream
            if (streamID > highestClientStreamId) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "highestClientStreamId set to stream-id: " + streamID);
                }
                highestClientStreamId = streamID;
            }
        }

        H2VirtualConnectionImpl h2VC = new H2VirtualConnectionImpl(initialVC);
        // remove the HttpDispatcherLink from the map, so a new one will be created and used by this new H2 stream
        h2VC.getStateMap().remove(HttpDispatcherLink.LINK_ID);
        H2HttpInboundLinkWrap link = new H2HttpInboundLinkWrap(httpInboundChannel, h2VC, streamID, this);
        H2StreamProcessor stream = new H2StreamProcessor(streamID, link, this);

        // for now, assume parent stream ID is root, need to change soon

        writeQ.addNewNodeToQ(streamID, Node.ROOT_STREAM_ID, Node.DEFAULT_NODE_PRIORITY, false);

        streamTable.put(streamID, stream);

        return stream;
    }

    public TCPConnectionContext getTCPConnectionContext() {
        return h2MuxTCPConnectionContext;
    }

    /**
     * Handle the receipt of the MAGIC string from the client: initialize the control stream 0 and and send out a settings frame to
     * acknowledge the MAGIC string
     */
    public void processConnectionPrefaceMagic() {
        connection_preface_string_rcvd = true;
        H2StreamProcessor controlStream = createNewInboundLink(0);
        controlStream.completeConnectionPreface();
        connection_preface_sent = true;
    }

    @Override
    public void ready(VirtualConnection inVC) {
        // ready should not be called on the H2InboundLink, only the HttpInboundLink for the first upgraded request.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ready called illegally!");
        }
    }

    @Override
    protected void processRequest() {
        // ready should not be called on the H2InboundLink, only the HttpInboundLink for the first upgraded request.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processRequest called illegally!");
        }
    }

    public boolean handleHTTP2UpgradeRequest(Map<String, String> headers, HttpInboundLink link) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest entry");
        }

        //1) Send the 101 response
        //2) Setup the new streams and Links
        //3) Place the new stream into the existing links

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, sending 101 response");
        }
        link.getHTTPContext().send101SwitchingProtocol("h2c");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, sent 101 response");
        }

        // if the above call goes async, not sure if we will then have a race condition, need to check further.System.out.println("H2InboundLink.handleHTTP2UpgradeRequest sent out 101");

        Integer streamID = new Integer(1);
        H2VirtualConnectionImpl h2VC = new H2VirtualConnectionImpl(initialVC);
        // remove the HttpDispatcherLink from the map, so a new one will be created and used by this new H2 stream
        h2VC.getStateMap().remove(HttpDispatcherLink.LINK_ID);
        H2HttpInboundLinkWrap wrap = new H2HttpInboundLinkWrap(httpInboundChannel, h2VC, streamID, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, creating stream processor");
        }
        H2StreamProcessor streamProcessor = new H2StreamProcessor(streamID, wrap, this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, created stream processor : " + streamProcessor);
        }

        // first stream on this connection will have the root as parent, exclusive isn't an option
        // not sure yet if priority can be anything other than the default for setting up the first stream on a connection
        writeQ.addNewNodeToQ(streamID, Node.ROOT_STREAM_ID, Node.DEFAULT_NODE_PRIORITY, false);

        streamTable.put(streamID, streamProcessor);

        // pull the settings header out of the request;
        // process it and apply it to the stream
        String settings = headers.get("HTTP2-Settings");
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleHTTP2UpgradeRequest, processing upgrade header settings : " + settings);
            }
            getConnectionSettings().processUpgradeHeaderSettings(settings);
        } catch (ProtocolException e1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleHTTP2UpgradeRequest an error occurred processing the settings during connection initialization");
            }
            return false;
        }

        initialHttpInboundLink = link;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, reinit the link : " + link);
        }
        link.reinit(wrap.getConnectionContext(), wrap.getVirtualConnection(), wrap);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, exit");
        }
        return true;
    }

    public void startAsyncRead(boolean newFrame) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startAsyncRead entry; newframe = " + newFrame);
        }
        // if new frame, reset and reuse the current one
        if (newFrame) {
            frameReadProcessor.reset(true);
        }

        if (slicedBuffer == null) {

            // allocate a byte buffer to read data into
            WsByteBufferPoolManager mgr = HttpDispatcher.getBufferManager();
            WsByteBuffer buf = mgr.allocate(Constants.READ_FRAME_BUFFER_SIZE);

            h2MuxTCPReadContext.setBuffer(buf);

            boolean forceQueue = true;
            int numBytes = 1; // read at least 1 or more bytes
            readStackDepthCount = 0; // reset count

            h2MuxTCPReadContext.read(numBytes, h2MuxReadCallback, forceQueue, TCPRequestContext.NO_TIMEOUT);

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "startAsyncRead reading slicedBuffer length: " + slicedBuffer.limit());
            }
            // need to process end of the last buffer instead of reading for a new buffer
            // call the callback complete to allow the read logic path to execute in full
            // the complete will execute serially on this thread
            slicedBuffer.position(slicedBuffer.limit());
            h2MuxTCPReadContext.setBuffer(slicedBuffer);
            slicedBuffer = null;

            if (readStackDepthCount < READ_STACK_DEPTH_LIMIT) {
                readStackDepthCount++;

                h2MuxReadCallback.complete(initialVC, h2MuxTCPReadContext);
                // the complete will execute serially on this thread - there should be no more logic after this since read could complete right away

            } else {
                // do complete on a new thread, since the stack depth is getting large and we don't want a stack overflow
                ExecutorService executorService = CHFWBundle.getExecutorService();
                AsyncCallback ac = new AsyncCallback();
                readStackDepthCount = 0;

                executorService.execute(ac);
                // the complete will execute async on another thread - there should be no more logic after this since read could complete right away
            }
            return;
        }

    }

    protected class AsyncCallback implements Runnable {

        // A seperate thread for doing the callback without going to the TCP Channel for more data

        protected AsyncCallback() {}

        @Override
        public void run() {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "doing async callback internally on a new thread");
            }

            try {
                h2MuxReadCallback.complete(initialVC, h2MuxTCPReadContext);
            } catch (Throwable t) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "caught a Throwable. log and leave: " + t);
                }
                // ASYNC: handle this better
            }
        }
    }

    public void processRead(VirtualConnection vc, TCPReadRequestContext rrc) throws ProtocolException {

        boolean readForNewFrame = true;

        // see if we can process it at this time
        synchronized (linkStatusSync) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processRead: :linkStatus: " + linkStatus + " writeLinkStatus: " + writeLinkStatus + " H2InboundLink hc: " + this.hashCode());
            }

            if ((writeLinkStatus != WRITE_LINK_STATUS.WRITE_IN_PROGRESS)
                && (linkStatus != LINK_STATUS.CLOSING) && (linkStatus != LINK_STATUS.GOAWAY_SENDING)) {

                readLinkStatus = READ_LINK_STATUS.PROCESSING_READ;
            } else {
                // mark that the write will need to complete this read processing once the write is done
                readWaitingForCompletion.setReadComplete(vc, rrc);
                return;
            }

            if (closeFuture != null) {
                boolean result = closeFuture.cancel(false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead: closeFuture detected while processing a read" + " :close: H2InboundLink hc: " + this.hashCode());
                }
                if (result == false) {
                    // couldn't cancelled, so we are in the process of closing, so return.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processRead: could not cancel closeFuture" + " :close: H2InboundLink hc: " + this.hashCode());
                    }
                    return;
                } else {
                    // cancel worked, so reset the link status
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processRead: cancelled successful, remove closeFuture and reset :linkStatus: to OPEN" + " :close: H2InboundLink hc: " + this.hashCode());
                    }
                    linkStatus = LINK_STATUS.OPEN;
                    closeFuture = null;
                    connTimeout = null;

                }
            }
        }

        int frameReadStatus = 0;

        // keep reading until the whole frame is read in before processing it
        WsByteBuffer nextBuffer = rrc.getBuffer();
        nextBuffer.flip();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processRead: next buffer length: " + nextBuffer.limit());
            }

            frameReadStatus = frameReadProcessor.processNextBuffer(nextBuffer);
            // if return "status" > 0 then current frame is completed, and new frame starts at the "status" value/index
            // return Constants.BP_FRAME_ALREADY_COMPLETE (-3) if the frame was already complete
            // return Constants.BP_FRAME_IS_NOT_COMPLETE (-2) if frame is not complete
            // return Constants.BP_FRAME_EXACTLY_COMPLETED (-1) if frame is complete at the exact end of this buffer

            // keep reading, return after this to unwind stack, careful the callback may be called before returning from async read request
            // so avoid any logic after the startAsyncRead request
            if (frameReadStatus == Constants.BP_FRAME_IS_NOT_COMPLETE) {
                readForNewFrame = false;
            } else {
                if (frameReadStatus > 0) {
                    // remember to release this, if it is never processed.
                    // Also, at this point, we need to make sure the slicedBuffer does not contain the current
                    // frame's payload data, which has not yet been processed.
                    int oldPosition = nextBuffer.position();
                    slicedBuffer = nextBuffer.position(frameReadStatus).slice();
                    nextBuffer.position(oldPosition);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead: process complete frame");
                }
                frameReadProcessor.processCompleteFrame();
            }
        } catch (Http2Exception e) {
            // If we get here we either couldn't determine a frame type, had encountered an error processing a connection-oriented frame.
            // In either case we need to send out a connection error.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processRead: an error occurred processing a frame: " + e.getErrorString());
            }
            try {
                getStreamProcessor(0).sendGOAWAYFrame(e);
            } catch (ProtocolException x) {
                // nothing to do here, since we can't even send the GOAWAY frame.
            }

        } finally {
            // we are done processing this read
            synchronized (linkStatusSync) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead: check to arm read: :linkStatus: " + linkStatus + " H2InboundLink hc: " + this.hashCode());
                }

                readWaitingForCompletion.reset();

                if ((linkStatus != LINK_STATUS.CLOSING) && (linkStatus != LINK_STATUS.GOAWAY_SENDING)) {

                    readLinkStatus = READ_LINK_STATUS.READ_OUTSTANDING;

                    // read for a new frame
                    startAsyncRead(readForNewFrame);

                }
            }
        }
    }

    public H2StreamProcessor getStreamProcessor(Integer sID) {
        H2StreamProcessor p = null;
        p = streamTable.get(sID);
        return p;
    }

    /**
     * A GOAWAY frame has been received; start shutting down this connection
     */
    @Override
    public void destroy() {
        httpInboundChannel.stop(50);

        initialVC = null;
        frameReadProcessor = null;
        h2MuxReadCallback = null;
        h2MuxTCPConnectionContext = null;
        h2MuxTCPReadContext = null;
        h2MuxTCPWriteContext = null;
        connectionSettings = null;
        readContextTable = null;
        writeContextTable = null;

        super.destroy();
    }

    public void writeSync(WsByteBuffer buf, WsByteBuffer bufs[], long numBytes, int timeout, FrameTypes fType, int payloadLength,
                          int streamID) throws IOException, FlowControlException {
        H2WriteQ.WRITE_ACTION action = H2WriteQ.WRITE_ACTION.NOT_SET;

        if ((buf != null) && (bufs != null)) {
            // only allowed one type of input per call
            // add debug statement
            IOException up = new IOException("Internal error - incorrect buffers specified");
            throw up;
        }

        H2WriteQEntry e = new H2WriteQEntry(buf, bufs, numBytes, timeout, H2WriteQEntry.WRITE_TYPE.SYNC, fType, payloadLength, streamID);
        e.armWriteCompleteLatch();

        action = writeQ.writeOrAddToQ(e);

        // will be queued if it didn't complete right away
        if (action == H2WriteQ.WRITE_ACTION.QUEUED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeSync - call entry.waitWriteCompleteLatch");
            }
            e.waitWriteCompleteLatch();
        }
    }

    // return null: it will complete via the callback
    // return the passed in H2TCPConnectionContext: if it finished rigth away
    public H2TCPConnectionContext writeASync(WsByteBuffer buf, WsByteBuffer bufs[], long numBytes,
                                             TCPWriteCompletedCallback callback, boolean forceQueue, int timeout, H2TCPConnectionContext connCtx,
                                             FrameTypes fType, int payloadLength, int streamID) throws IOException, FlowControlException {

        H2WriteQ.WRITE_ACTION action = H2WriteQ.WRITE_ACTION.NOT_SET;

        if ((buf != null) && (bufs != null)) {
            // only allowed one type of input per call
            // add debug statement
            IOException up = new IOException("too many buffer parameters set");
            throw up;
        }

        H2WriteQEntry e = new H2WriteQEntry(buf, bufs, numBytes, callback, forceQueue, timeout, connCtx, H2WriteQEntry.WRITE_TYPE.ASYNC, fType, payloadLength, streamID);

        action = writeQ.writeOrAddToQ(e);

        if (action == H2WriteQ.WRITE_ACTION.COMPLETED) {
            return connCtx;
        }

        return null;
    }

    public synchronized int getNextPromisedStreamId() {
        h2NextPromisedStreamId = h2NextPromisedStreamId + 2;
        return (h2NextPromisedStreamId);
    }

    public H2HeaderTable getReadTable() {
        return this.readContextTable;
    }

    public H2HeaderTable getWriteTable() {
        return this.writeContextTable;
    }

    public H2WorkQInterface getWorkQ() {
        return this.writeQ;
    }

    public void incrementConnectionWindowUpdateLimit(int x) throws FlowControlException {
        writeQ.incrementConnectionWindowUpdateLimit(x);
    }

    public synchronized void changeInitialWindowSizeAllStreams(int newSize) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "changeInitialWindowSizeAllStreams entry: newSize: " + newSize);
        }
        this.initialWindowSize = newSize;

        H2StreamProcessor stream;
        for (Integer i : streamTable.keySet()) {
            stream = streamTable.get(i);
            stream.updateInitialWindowsUpdateSize(newSize);
        }
    }

    /**
     * @return the connectionSettings
     */
    public H2ConnectionSettings getConnectionSettings() {
        return connectionSettings;
    }

    public void cleanupStream(int streamID) {
        streamTable.remove(streamID);
        writeQ.removeNodeFromQ(streamID);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.inbound.HttpInboundLink#destroy(java.lang.Exception)
     */
    @Override
    public void destroy(Exception e) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "destroy entry");
        }

        H2StreamProcessor stream;
        for (Integer i : streamTable.keySet()) {
            stream = streamTable.get(i);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "destroying " + stream + ", " + stream.myID);
            }
            if (stream.myID != 0) {
                stream.h2HttpInboundLinkWrap.destroy(e);
            }
        }

        initialVC = null;
        frameReadProcessor = null;
        h2MuxReadCallback = null;
        h2MuxTCPConnectionContext = null;
        h2MuxTCPReadContext = null;
        h2MuxTCPWriteContext = null;
        connectionSettings = null;
        readContextTable = null;
        writeContextTable = null;

        super.destroy(e);
    }

    public boolean setStatusLinkToGoAwaySending() {

        synchronized (linkStatusSync) {

            if (linkStatus != LINK_STATUS.CLOSING) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "setStatusLinkToGoAwaySending: setting :linkStatus: to GOAWAY_SENDING" + ":close: H2InboundLink hc: " + this.hashCode());
                }
                linkStatus = LINK_STATUS.GOAWAY_SENDING;

                return true;

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "setStatusLinkToGoAwaySending: return without setting status since :linkstatus: is " + linkStatus + " :close: H2InboundLink hc: " + this.hashCode());
                }

                return false;
            }
        }
    }

    public void goAway() {
        boolean closeFromHere = false;
        Exception exceptionForCloseFromHere = null;

        synchronized (linkStatusSync) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "goAway: :linkstatus: is: " + linkStatus + " :close: H2InboundLink hc: " + this.hashCode());
            }

            if (linkStatus == LINK_STATUS.CLOSING) {
                return;
            }

            if (closeFuture == null) {
                closeFromHere = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "goAway: no closeFuture, so closing down from here" + " :close: H2InboundLink hc: " + this.hashCode());
                }
            } else if (closeFuture.isDone() == false) {
                closeFromHere = closeFuture.cancel(false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "goAway: closeFuture.cancel returned: " + closeFromHere + " :close: H2InboundLink hc: " + this.hashCode());
                }
                // if closeFromHere is true now, then cancelled worked, and this thread needs to close
                // otherwise another thread is tasked with closing, and this thread can return
            }

            if (closeFromHere) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "goAway: close the device link now. setting :linkStatus: to CLOSING" + " :close: H2InboundLink hc: " + this.hashCode());
                }
                // we are tasked with closing the device link, and now no more frames should be written or read by the H2 code.
                linkStatus = LINK_STATUS.CLOSING;
                if (connTimeout != null) {
                    exceptionForCloseFromHere = connTimeout.e;
                }

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "goAway: another thread will close" + " :close: H2InboundLink hc: " + this.hashCode());
                }
            }
        } // end sync, close the deviceLink outside the link

        if (closeFromHere) {
            ConnectionLink deviceLink = initialHttpInboundLink.getDeviceLink();
            if (deviceLink != null) {
                try {
                    initialHttpInboundLink.getDeviceLink().close(initialVC, exceptionForCloseFromHere);
                } catch (Throwable x) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "goAway: could not close, :close: H2InboundLink hc: " + this.hashCode() + " device link close caught: " + x);

                        StringBuffer sb = new StringBuffer();
                        StackTraceElement[] trace = x.getStackTrace();
                        for (int i = 0; i < trace.length; i++) {
                            sb.append(" " + trace[i] + "\r\n");
                        }
                        sb.append("");
                        String s = sb.toString();

                        Tr.debug(tc, "goAway: " + s);
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "goAway: could not close, device link was null" + " :close: H2InboundLink hc: " + this.hashCode());
                }
            }
        }
    }

    public boolean checkIfGoAwaySending() {

        synchronized (linkStatusSync) {

            if (linkStatus != LINK_STATUS.GOAWAY_SENDING) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkifGoAwaySending() returning false :linkstatus: " + linkStatus);
                }
                return false;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "checkifGoAwaySending() returning true :linkstatus: " + linkStatus);
            }
            return true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.http.channel.internal.inbound.HttpInboundLink#close(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Exception)
     */
    @Override
    public void close(VirtualConnection inVC, Exception e) {

        // This H2InboundLink.close method should only get called from the H2HttpInboundLinkWrap.close method.
        // for this reason, if we sync this method, then if a stream changes state while we are looking at it, we should
        // be able to do the close when that stream closing causes this close method to be called.

        // the device link close should always use the initial VC that this object was created with, so inVC will be ignored.

        synchronized (linkStatusSync) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close(vc,e): :linkstatus: is: " + linkStatus + " :close: H2InboundLink hc: " + this.hashCode());
            }

            if ((linkStatus == LINK_STATUS.CLOSING) || (linkStatus == LINK_STATUS.GOAWAY_SENDING)
                || (linkStatus == LINK_STATUS.WAIT_TO_SEND_GOAWAY)) {
                // another thread is in charge of closing, or another thread has already armed the future to close
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close(vc,e): returning: close of muxLink is being done on a differnt thread" + " :close: H2InboundLink hc: " + this.hashCode());
                }
                return;
            }

            //Determine if all streams are in half closed or closed state
            //If not, do nothing and return
            //If so, look to see if the GoAway frame has been sent
            //If not, trigger a timer and wait to send the GOAWAY frame
            //If so, call close on the TCP-Channel/Device-Channel below us

            H2StreamProcessor stream;
            for (Integer i : streamTable.keySet()) {
                stream = streamTable.get(i);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close(vc,e): looking at stream: " + stream.myID);
                }

                if (stream.myID != 0 && !stream.isHalfClosed() && !stream.isStreamClosed() && highestLocalStreamId > -1) {
                    continue;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "close(vc,e): stream not ready to close: " + stream.myID + " :close: H2InboundLink hc: " + this.hashCode());
                    }
                    return;
                }
            }

            //All streams are either closed or in half closed, and a GOAWAY frame needs to be sent
            //Wait the timeout time and then send the GOAWAY frame with the last good stream

            linkStatus = LINK_STATUS.WAIT_TO_SEND_GOAWAY;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close(vc,e): loading up the wait to close timeout" + " :close: H2InboundLink hc: " + this.hashCode());
            }

            ScheduledExecutorService scheduler = CHFWBundle.getScheduledExecutorService();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close : scheduler : " + scheduler + " config : " + config);
            }
            connTimeout = new H2ConnectionTimeout(initialVC, e);

            //Save the future so we can cancel it later on
            closeFuture = scheduler.schedule(connTimeout, config.getH2ConnCloseTimeout(), TimeUnit.SECONDS);
        }
    }

    private class H2ConnectionTimeout implements Runnable {
        private final VirtualConnection vc;
        private final Exception e;

        public H2ConnectionTimeout(VirtualConnection inVC, Exception inE) {
            vc = inVC;
            e = inE;
        }

        @Override
        public void run() {

            synchronized (linkStatusSync) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2ConnectionTimeout-run: timeout has elapsed, look to close connection. :linkStatus: " + linkStatus + " :close: H2InboundLink hc: "
                                 + hcDebug);
                }

                if (linkStatus != LINK_STATUS.WAIT_TO_SEND_GOAWAY) {
                    // another thread is in charge of closing
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "H2ConnectionTimeout-run: timeout too late - close being handled on another thread" + " :close: H2InboundLink hc: " + hcDebug);
                    }
                    return;
                }

                // this thread is in charge of closing, it will send a GOAWAY first.
                linkStatus = LINK_STATUS.GOAWAY_SENDING;
            }

            try {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2ConnectionTimeout-run: sending GOAWAY Frame" + " :close: H2InboundLink hc: " + hcDebug);
                }

                streamTable.get(0).sendGOAWAYFrame(new Http2Exception("the http2 connection has timed out"));

            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2ConnectionTimeout-run: exeception received while sending GOAWAY: " + " :close: H2InboundLink hc: " + hcDebug + " " + e);
                }
            } finally {

                boolean closeFromHere = false;

                synchronized (linkStatusSync) {
                    if (linkStatus != LINK_STATUS.CLOSING) {
                        linkStatus = LINK_STATUS.CLOSING;
                        closeFromHere = true;
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2ConnectionTimeout-run: closefromeHere: " + closeFromHere + " :close: H2InboundLink hc: " + hcDebug);
                }

                if (closeFromHere) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "H2ConnectionTimeout-run: set :linkStatus: to CLOSING and close the device link" + " :close: H2InboundLink hc: " + hcDebug);
                    }

                    ConnectionLink deviceLink = initialHttpInboundLink.getDeviceLink();
                    if (deviceLink != null) {
                        initialHttpInboundLink.getDeviceLink().close(vc, e);
                    }
                }
            }
        }
    }

    public void triggerLinkClose(VirtualConnection inVC, Exception inE) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "triggerLinkClose: linkStatus: " + linkStatus + " :close: H2InboundLink hc: " + this.hashCode());
        }

        synchronized (linkStatusSync) {

            if ((linkStatus == LINK_STATUS.CLOSING) || (linkStatus == LINK_STATUS.GOAWAY_SENDING)) {
                // close is being handled on a different thread, so do nothing
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "triggerLinkClose: close is being handled on a different thread" + " :close: H2InboundLink hc: " + this.hashCode());
                }
                return;
            }

            // we are tasked with closing the device link, and now no more frames should be written or read by the H2 code.
            linkStatus = LINK_STATUS.CLOSING;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "triggerLinkClose: close from him, change :linkStatus: to CLOSING" + " :close: H2InboundLink hc: " + this.hashCode());
            }
        }

        ConnectionLink deviceLink = initialHttpInboundLink.getDeviceLink();
        if (deviceLink != null) {
            initialHttpInboundLink.getDeviceLink().close(inVC, inE);
        }
    }

    public void triggerStreamClose(H2StreamProcessor streamProcessor) {

        if (closeTable.size() >= 512) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "triggerStreamClose : close table size greater than or equal to 512, purge the table of old entries");
            }
            purgeCloseTable();
        }

        streamProcessor.setCloseTime(System.nanoTime());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "triggerStreamClose : move stream into close table.  stream-id: " + streamProcessor.myID);
        }

        closeTable.put(streamProcessor.myID, streamProcessor);
        streamTable.remove(streamProcessor.myID);
    }

    public H2StreamProcessor getStream(int streamID) {
        H2StreamProcessor streamProcessor = null;

        streamProcessor = streamTable.get(streamID);
        if (streamProcessor == null) {
            streamProcessor = closeTable.get(streamID);
        }

        return streamProcessor;
    }

    public boolean significantlyPastCloseTime(int streamID) {
        if (streamTable.contains(streamID))
            return false;
        if (closeTable.containsKey(streamID)) {
            H2StreamProcessor streamProcessor = closeTable.get(streamID);
            if (streamProcessor.getCloseTime() != Constants.INITIAL_CLOSE_TIME) {
                long diff = System.nanoTime() - streamProcessor.getCloseTime();
                if (diff > CLOSE_TABLE_PURGE_TIME) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "stream-id: " + streamID + " closed and significantly past the close time, close time: " + streamProcessor.getCloseTime()
                                     + " now: " + System.nanoTime() + " diff: " + diff);
                    }
                    closeTable.remove(streamID);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Stream ID: " + streamID + " not in stream or close table");
        }
        return true;
    }

    public void purgeCloseTable() {
        long now = System.nanoTime();
        for (Map.Entry<Integer, H2StreamProcessor> entry : closeTable.entrySet()) {
            if (entry.getValue().getCloseTime() + CLOSE_TABLE_PURGE_TIME < now) {
                // old closed stream, so remove
                closeTable.remove(entry.getKey());
            }
        }
    }

    public int getHighestClientStreamId() {
        return highestClientStreamId;
    }

}
