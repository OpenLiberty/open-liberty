/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
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
import com.ibm.ws.http.channel.h2internal.exceptions.StreamClosedException;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.priority.Node;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannel;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundLink;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.transport.access.TransportConstants;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
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
        NOT_READING, READ_OUTSTANDING
    };

    // Note - the following objects should only be accessed and examined while holding the linkStatusSync lock
    LINK_STATUS linkStatus = LINK_STATUS.INIT;
    private ScheduledFuture<?> closeFuture = null;
    private H2ConnectionTimeout connTimeout = null;
    Object linkStatusSync = new Object() {
    };

    READ_LINK_STATUS readLinkStatus = READ_LINK_STATUS.NOT_READING;
    Object readLinkStatusSync = new Object() {
    };

    private int configuredInactivityTimeout = 0; // in milleseconds;
    private long lastWriteTime = 0;
    private int OutstandingWriteCount = 0;
    private final Object OutstandingWriteCountSync = new Object() {
    };
    private final int closeWaitForWritesWatchDogTimer = 5000;
    private final int closeWaitForReadWatchDogTimer = 5000;
    private final int STREAM_CLOSE_DELAY = 2000;

    // keep track of the highest IDs processed
    private int highestClientStreamId = 0;
    private int highestLocalStreamId = -1; // this moves to 0 when the connection stream is established
    private int goawayPromisedStreamId = 0; // keeps track of the ID used for a GOAWAY promised-stream-id
    private int openPushStreams = 0;
    private final Object streamOpenCloseSync = new Object() {
    };
    private int activeClientStreams = 0;
    private final Object streamCounterSync = new Object() {
    };

    boolean connection_preface_sent = false; // empty SETTINGS frame has been sent
    boolean connection_preface_string_rcvd = false; // MAGIC string has been received
    public volatile CountDownLatch initLock = new CountDownLatch(1) {
    };

    volatile long initialWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE;
    volatile long connectionReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE; // keep track of how much data the client is allowed to send to the us
    volatile long maxReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE; // user-set max window size

    FrameReadProcessor frameReadProcessor = null;

    H2MuxTCPReadCallback h2MuxReadCallback = null;
    TCPReadRequestContext h2MuxTCPReadContext = null;

    H2MuxTCPWriteCallback h2MuxWriteCallback = null;
    TCPWriteRequestContext h2MuxTCPWriteContext = null;

    WsByteBuffer slicedBuffer = null;

    ConcurrentHashMap<Integer, H2StreamProcessor> streamTable = new ConcurrentHashMap<Integer, H2StreamProcessor>();
    ConcurrentLinkedQueue<H2StreamProcessor> closedStreams = new ConcurrentLinkedQueue<H2StreamProcessor>();

    HttpInboundLink initialHttpInboundLink = null;
    VirtualConnection initialVC = null;
    HttpInboundChannel httpInboundChannel = null;
    TCPConnectionContext h2MuxTCPConnectionContext = null;
    HttpInboundServiceContextImpl h2MuxServiceContextImpl = null;

    private H2ConnectionSettings localConnectionSettings;
    private H2ConnectionSettings remoteConnectionSettings;

    H2WorkQInterface writeQ = null;

    int h2NextPromisedStreamId = 0;

    private String authority = null;

    private H2HeaderTable readContextTable = null;
    private H2HeaderTable writeContextTable = null;

    HttpChannelConfig config = null;

    private int readStackDepthCount = 0;
    private final static int READ_STACK_DEPTH_LIMIT = 64;

    int hcDebug = 0x0;

    private boolean continuationFrameExpected = false;
    private boolean writeContinuationFrameExpected = false;

    private final Object oneTimeEntrySync = new Object() {
    };
    private boolean oneTimeEntry = false;

    private final H2RateState rateState = new H2RateState();

    public H2RateState getH2RateState() {
        return this.rateState;
    }

    public boolean isContinuationExpected() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isContinuationExpected: " + continuationFrameExpected);
        }
        return continuationFrameExpected;
    }

    public void setContinuationExpected(boolean expected) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setContinuationExpected: " + expected);
        }
        this.continuationFrameExpected = expected;
    }

    public boolean isWriteContinuationExpected() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isWriteContinuationExpected: " + writeContinuationFrameExpected);
        }
        return writeContinuationFrameExpected;
    }

    public void setWriteContinuationExpected(boolean expected) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setWriteContinuationExpected: " + expected);
        }
        this.writeContinuationFrameExpected = expected;
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
        config = channel.getHttpConfig();
        localConnectionSettings = new H2ConnectionSettings();
        localConnectionSettings.setMaxConcurrentStreams(this.config.getH2MaxConcurrentStreams());
        localConnectionSettings.setMaxFrameSize(this.config.getH2MaxFrameSize());
        configuredInactivityTimeout = this.config.getH2ConnectionIdleTimeout();
        remoteConnectionSettings = new H2ConnectionSettings();

        h2MuxServiceContextImpl = (HttpInboundServiceContextImpl) this.getChannelAccessor();

        // set up the initial connection read window size
        maxReadWindowSize = config.getH2ConnReadWindowSize();
        connectionReadWindowSize = maxReadWindowSize;

        writeQ = new H2WriteTree();
        writeQ.init(h2MuxTCPWriteContext, h2MuxWriteCallback);

        readContextTable = new H2HeaderTable();
        writeContextTable = new H2HeaderTable();

        hcDebug = this.hashCode();

        initialVC.getStateMap().put(TransportConstants.UPGRADED_WEB_CONNECTION_NEEDS_CLOSE, "true");
        initialVC.getStateMap().put("h2_frame_size", getRemoteConnectionSettings().getMaxFrameSize());
    }

    public synchronized long getInitialWindowSize() {
        return initialWindowSize;
    }

    /**
     * Create a new stream and add it to this link. If the stream ID is even, check to make sure this link has not exceeded
     * the maximum number of concurrent streams (as set by the client); if too many streams are open, don't open a new one.
     *
     * @param streamID
     * @return null if creating this stream would exceed the maximum number locally-opened streams
     */
    public H2StreamProcessor createNewInboundLink(Integer streamID) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createNewInboundLink entry: stream-id: " + streamID);
        }
        if ((streamID % 2 == 0) && (streamID != 0)) {
            synchronized (streamOpenCloseSync) {
                int maxPushStreams = getRemoteConnectionSettings().getMaxConcurrentStreams();
                // if there are too many locally-open active streams, don't open a new one
                if (maxPushStreams >= 0 && openPushStreams > maxPushStreams) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "createNewInboundLink cannot open a new push stream; maximum number of open push streams reached" + openPushStreams);
                    }
                    return null;
                }
                openPushStreams++;

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
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createNewInboundLink exit: returning stream: " + streamID + " " + stream);
        }
        return stream;
    }

    public TCPConnectionContext getTCPConnectionContext() {
        return h2MuxTCPConnectionContext;
    }

    /**
     * Handle the receipt of the MAGIC string from the client: initialize the control stream 0 and and send out a settings frame to
     * acknowledge the MAGIC string
     *
     * @throws StreamClosedException
     */
    public void processConnectionPrefaceMagic() throws Http2Exception {
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

    /**
     * Handle a connection initiated via ALPN "h2", or directly via h2-with-prior-knowledge
     *
     * @param link the initial inbound link
     * @return true if the upgrade was sucessful
     */
    public boolean handleHTTP2DirectConnect(HttpInboundLink link) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2DirectConnect entry");
        }

        initialHttpInboundLink = link;
        Integer streamID = new Integer(0);

        H2VirtualConnectionImpl h2VC = new H2VirtualConnectionImpl(initialVC);

        // remove the HttpDispatcherLink from the map, so a new one will be created and used by this new H2 stream
        h2VC.getStateMap().remove(HttpDispatcherLink.LINK_ID);
        H2HttpInboundLinkWrap wrap = new H2HttpInboundLinkWrap(httpInboundChannel, h2VC, streamID, this);

        // create the initial stream processor, add it to the link stream table, and add it to the write queue
        H2StreamProcessor streamProcessor = new H2StreamProcessor(streamID, wrap, this, StreamState.OPEN);
        streamTable.put(streamID, streamProcessor);
        writeQ.addNewNodeToQ(streamID, Node.ROOT_STREAM_ID, Node.DEFAULT_NODE_PRIORITY, false);
        this.setDeviceLink((ConnectionLink) myTSC);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2DirectConnect, exit");
        }
        return true;
    }

    /**
     * Handle an h2c upgrade request
     *
     * @param headers a map of the headers for this request
     * @param link    the initial inbound link
     * @return true if the http2 upgrade was successful
     */
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
        H2StreamProcessor streamProcessor = new H2StreamProcessor(streamID, wrap, this, StreamState.HALF_CLOSED_REMOTE);
        incrementActiveClientStreams();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleHTTP2UpgradeRequest, created stream processor : " + streamProcessor);
        }

        // first stream on this connection will have the root as parent, exclusive isn't an option
        // not sure yet if priority can be anything other than the default for setting up the first stream on a connection
        writeQ.addNewNodeToQ(streamID, Node.ROOT_STREAM_ID, Node.DEFAULT_NODE_PRIORITY, false);

        streamTable.put(streamID, streamProcessor);
        highestClientStreamId = streamID;
        goawayPromisedStreamId = streamID;

        // add stream 0 to the table, in case we need to write out any control frames prior to initialization completion
        streamID = 0;
        streamProcessor = new H2StreamProcessor(streamID, wrap, this, StreamState.OPEN);
        streamTable.put(streamID, streamProcessor);

        // pull the settings header out of the request;
        // process it and apply it to the stream
        String settings = headers.get("HTTP2-Settings");
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleHTTP2UpgradeRequest, processing upgrade header settings : " + settings);
            }
            getRemoteConnectionSettings().processUpgradeHeaderSettings(settings);
        } catch (Http2Exception e1) {
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

    /**
     * Keep track of the highest-valued local and remote stream IDs for this connection
     *
     * @param proposedHighestStreamId
     * @throws ProtocolException if the proposed stream ID is lower than a previous streams'
     */
    protected void updateHighestStreamId(int proposedHighestStreamId) throws ProtocolException {
        if ((proposedHighestStreamId & 1) == 0) { // even number, server-initialized stream
            if (proposedHighestStreamId > highestLocalStreamId) {
                highestLocalStreamId = proposedHighestStreamId;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "highestLocalStreamId set to stream-id: " + proposedHighestStreamId);
                }
            } else if (proposedHighestStreamId < highestLocalStreamId) {
                throw new ProtocolException("received a new stream with a lower ID than previous; "
                                            + "current stream-id: " + proposedHighestStreamId + " highest stream-id: "
                                            + highestLocalStreamId);
            }
        } else {
            if (proposedHighestStreamId > highestClientStreamId) {
                highestClientStreamId = proposedHighestStreamId;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "highestClientStreamId set to stream-id: " + proposedHighestStreamId);
                }
            } else if (proposedHighestStreamId < highestClientStreamId) {
                throw new ProtocolException("received a new stream with a lower ID than previous; "
                                            + "current stream-id: " + proposedHighestStreamId + " highest stream-id: "
                                            + highestClientStreamId);
            }
        }
    }

    /**
     * Update the highest processed stream ID, used for the GOAWAY promised ID
     */
    protected void updateGoawayPromisedStreamId(int id) {
        goawayPromisedStreamId = id;
    }

    /**
     * Get the highest processed stream ID, to be used for the GOAWAY promised ID
     */
    protected int getGoawayPromisedStreamId() {
        return goawayPromisedStreamId;
    }

    public void startAsyncRead(boolean newFrame) {
        // start the read with the configured read timeout
        startAsyncRead(newFrame, configuredInactivityTimeout);
    }

    private boolean freeBufferOnError = false;

    protected boolean getFreeBufferOnError() {
        return freeBufferOnError;
    }

    public void startAsyncRead(boolean newFrame, int readTimeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startAsyncRead entry; newframe = " + newFrame + " readTimeout: " + readTimeout);
        }
        // if new frame, reset and reuse the current one
        if (newFrame) {
            frameReadProcessor.reset(true);
        }

        freeBufferOnError = false;

        if (slicedBuffer == null) {

            // allocate a byte buffer to read data into
            WsByteBufferPoolManager mgr = HttpDispatcher.getBufferManager();
            WsByteBuffer buf = mgr.allocate(Constants.READ_FRAME_BUFFER_SIZE);

            h2MuxTCPReadContext.setBuffer(buf);
            freeBufferOnError = true;

            boolean forceQueue = true;
            int numBytes = 1; // read at least 1 or more bytes
            readStackDepthCount = 0; // reset count

            synchronized (readLinkStatusSync) {
                readLinkStatus = READ_LINK_STATUS.READ_OUTSTANDING;
            }
            try {
                int timeout = TCPRequestContext.NO_TIMEOUT;
                if (readTimeout != 0) {
                    timeout = readTimeout;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "startAsyncRead do read on channel below ");
                }

                h2MuxTCPReadContext.read(numBytes, h2MuxReadCallback, forceQueue, timeout);

            } catch (Throwable up) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "startAsyncRead read caught throwable: " + up);
                }

                buf.release();
                setReadLinkStatusToNotReadingAndNotify();

                throw up;
            }

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
            // return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startAsyncRead exit");
        }

    }

    protected void setReadLinkStatusToNotReadingAndNotify() {
        synchronized (readLinkStatusSync) {
            readLinkStatus = READ_LINK_STATUS.NOT_READING;
            readLinkStatusSync.notify();
        }
    }

    protected class AsyncCallback implements Runnable {

        // A seperate thread for doing the callback without going to the TCP Channel for more data

        protected AsyncCallback() {
        }

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

    public void processRead(VirtualConnection vc, TCPReadRequestContext rrc) {
        // use the configured timeout
        processRead(vc, rrc, configuredInactivityTimeout);
    }

    public void processRead(VirtualConnection vc, TCPReadRequestContext rrc, int readTimeout) {

        boolean readForNewFrame = true;

        // see if we can process it at this time
        synchronized (linkStatusSync) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processRead: :linkStatus: " + linkStatus + " H2InboundLink hc: " + this.hashCode());
            }

            if ((linkStatus == LINK_STATUS.CLOSING) || (linkStatus == LINK_STATUS.GOAWAY_SENDING)) {
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
                        Tr.debug(tc, "processRead: cancelled successful, remove closeFuture" + " :close: H2InboundLink hc: " + this.hashCode());
                    }
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
            close(vc, e);

        } finally {

            boolean doRead = false;

            // we are done processing this read
            synchronized (linkStatusSync) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processRead: check to arm read: :linkStatus: " + linkStatus + " H2InboundLink hc: " + this.hashCode());
                }

                if ((linkStatus != LINK_STATUS.CLOSING) && (linkStatus != LINK_STATUS.GOAWAY_SENDING)) {
                    doRead = true;
                }

            }

            if (doRead) {
                // read for a new frame
                // read outside of synchronized to avoid thread deadlock
                startAsyncRead(readForNewFrame, readTimeout);
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
        localConnectionSettings = null;
        remoteConnectionSettings = null;
        readContextTable = null;
        writeContextTable = null;

        super.destroy();
    }

    private void waitForReadsAndWritesToClear() {
        // Note: this method should only be called if the LINK_STATUS has been set to CLOSING, otherwise the write count can bounce off 0

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "waitForReadsAndWritesToClear - wait for reads to clear. :close: H2InboundLink hc: " + this.hashCode());
        }
        synchronized (readLinkStatusSync) {
            if (readLinkStatus == READ_LINK_STATUS.READ_OUTSTANDING) {
                // attempt to cancel the outstanding read
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "waitForReadsAndWritesToClear - cancel outstanding read. :close: H2InboundLink hc: " + this.hashCode());
                }
                h2MuxTCPReadContext.read(1, null, true, TCPRequestContext.IMMED_TIMEOUT);
                try {
                    readLinkStatusSync.wait(closeWaitForReadWatchDogTimer);
                } catch (InterruptedException e) {
                    // proceed if something is wrong here
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "waitForReadsAndWritesToClear - wait for writes to clear. :close: H2InboundLink hc: " + this.hashCode());
        }

        synchronized (OutstandingWriteCountSync) {
            if (OutstandingWriteCount > 0) {
                try {
                    OutstandingWriteCountSync.wait(closeWaitForWritesWatchDogTimer);
                } catch (InterruptedException e) {
                    // proceed if something is wrong here
                }
            }
        }
    }

    public void writeSync(WsByteBuffer buf, WsByteBuffer bufs[], long numBytes, int timeout, FrameTypes fType, int payloadLength,
                          int streamID) throws IOException, FlowControlException {
        H2WorkQInterface.WRITE_ACTION action = H2WorkQInterface.WRITE_ACTION.NOT_SET;

        if ((buf != null) && (bufs != null)) {
            // only allowed one type of input per call
            // add debug statement
            IOException up = new IOException("Internal error - incorrect buffers specified");
            throw up;
        }

        synchronized (linkStatus) {
            if (linkStatus == LINK_STATUS.CLOSING) {
                FlowControlException up = new FlowControlException("Connection Closing");
                throw up;
            } else {
                synchronized (OutstandingWriteCountSync) {
                    OutstandingWriteCount++;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "writeSync - incremented write count to: " + OutstandingWriteCount + " H2InboundLink hc: " + this.hashCode());
                    }
                }
            }
        }

        try {
            H2WriteQEntry e = new H2WriteQEntry(buf, bufs, numBytes, timeout, fType, payloadLength, streamID);
            e.armWriteCompleteLatch();

            action = writeQ.writeOrAddToQ(e);

            // will be queued if it didn't complete right away
            if (action == H2WorkQInterface.WRITE_ACTION.QUEUED) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeSync - call entry.waitWriteCompleteLatch");
                }
                e.waitWriteCompleteLatch();
            }

            // write worked, update time if we are current tracking it for inactivity
            if (configuredInactivityTimeout != 0) {
                synchronized (OutstandingWriteCountSync) {
                    lastWriteTime = System.nanoTime();
                }
            }

            if (e.getIOException() != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeSync - IOException recived while writing: " + e);
                }
                throw (e.getIOException());
            }

        } finally {
            synchronized (OutstandingWriteCountSync) {
                OutstandingWriteCount--;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeSync - decremented write count to: " + OutstandingWriteCount + " lastWriteTime: " + lastWriteTime + " H2InboundLink hc: " + this.hashCode());
                }
                if (OutstandingWriteCount == 0) {
                    OutstandingWriteCountSync.notify();
                }
            }
        }
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

    /**
     * Increment the connection window limit but the given amount
     *
     * @param int amount to increment connection window
     * @throws FlowControlException
     */
    public void incrementConnectionWindowUpdateLimit(int x) throws FlowControlException {
        if (!checkIfGoAwaySendingOrClosing()) {
            writeQ.incrementConnectionWindowUpdateLimit(x);
            H2StreamProcessor stream;
            for (Integer i : streamTable.keySet()) {
                stream = streamTable.get(i);
                if (stream != null) {
                    stream.connectionWindowSizeUpdated();
                }
            }
        }
    }

    /**
     * Update the initial window size for all open streams. Additionally, call updateInitialWindowsUpdateSize()
     * on each stream to notify it of the increase (and possible write out queued data)
     *
     * @param int newSize
     * @throws FlowControlException
     */
    public synchronized void changeInitialWindowSizeAllStreams(int newSize) throws FlowControlException {
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
     * Returns the connection settings as specified by the local http endpoint
     *
     * @return the localConnectionSettings
     */
    public H2ConnectionSettings getLocalConnectionSettings() {
        return localConnectionSettings;
    }

    /**
     * Returns the connection settings as specified by the remote http endpoint
     *
     * @return the remoteConnectionSettings
     */
    public H2ConnectionSettings getRemoteConnectionSettings() {
        return remoteConnectionSettings;
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
            // notify streams waiting for a window update
            synchronized (stream) {
                stream.notifyAll();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "destroying " + stream + ", " + stream.getId());
            }
            if (stream.getId() != 0) {
                stream.getWrappedInboundLink().destroy(e);
            }
        }

        initialVC = null;
        frameReadProcessor = null;
        h2MuxReadCallback = null;
        h2MuxTCPConnectionContext = null;
        h2MuxTCPReadContext = null;
        h2MuxTCPWriteContext = null;
        localConnectionSettings = null;
        remoteConnectionSettings = null;
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

    public void closeConnectionLink(Exception exceptionForCloseFromHere) {
        closeConnectionLink(exceptionForCloseFromHere, false);
    }

    public void closeConnectionLink(Exception exceptionForCloseFromHere, boolean attemptGoAway) {

        // can only enter this routine once per lifecycle of this object
        synchronized (oneTimeEntrySync) {
            if (oneTimeEntry) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: re-entered so exiting :close: H2InboundLink hc: " + this.hashCode());
                }
                return;
            }
            oneTimeEntry = true;
        }

        synchronized (linkStatusSync) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "closeConnectionLink: :linkstatus: is: " + linkStatus + " :close: H2InboundLink hc: " + this.hashCode());
            }

            if (linkStatus == LINK_STATUS.CLOSING) {
                return;
            }
        }

        // Outside of holding the linkStatusSync, try to cleanly close the connection at the dispatcher link, in
        // case there is processing currently in flight.
        HttpDispatcherLink hdLink = (HttpDispatcherLink) initialVC.getStateMap().get(HttpDispatcherLink.LINK_ID);
        if (hdLink != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HttpDispatcherLink found: " + hdLink);
            }
            try {
                hdLink.close(initialVC, exceptionForCloseFromHere);
            } catch (Exception consume) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: consuming exception: " + consume);
                }
            }
        }

        synchronized (linkStatusSync) {

            if ((closeFuture != null) && (closeFuture.isDone() == false)) {
                boolean closeFutureCancel = closeFuture.cancel(false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: closeFuture.cancel returned: " + closeFutureCancel + " :close: H2InboundLink hc: " + this.hashCode());
                }
            }

            if (!attemptGoAway) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: close the device link now. setting :linkStatus: to CLOSING" + " :close: H2InboundLink hc: " + this.hashCode());
                }
                // we are tasked with closing the device link, and now no more frames should be written or read by the H2 code on this connection.
                linkStatus = LINK_STATUS.CLOSING;
            }

        } // end sync, close the deviceLink outside the sync lock

        if (attemptGoAway) {

            try {
                streamTable.get(0).sendGOAWAYFrame(new Http2Exception(exceptionForCloseFromHere.getMessage()));
            } catch (Http2Exception x) {
                // just keep closing down
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: exeception received while sending GOAWAY :close: H2InboundLink hc: " + hcDebug + " " + x);
                }
            }

            synchronized (linkStatusSync) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: close the device link now after sending GOAWAY. setting :linkStatus: to CLOSING" + " :close: H2InboundLink hc: "
                                 + this.hashCode());
                }
                // we are tasked with closing the device link, and now no more frames should be written or read by the H2 code on this connection.
                linkStatus = LINK_STATUS.CLOSING;
            }

        }

        // tell the write tree queue to quit.  wait for the queue to drain, so no writes will be outstanding when closing
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "closeConnectionLink: tell WriteQ to Draing an quit. :close: H2InboundLink hc: " + this.hashCode());
        }
        writeQ.setToQuit(true);

        waitForReadsAndWritesToClear();

        ConnectionLink deviceLink = initialHttpInboundLink.getDeviceLink();
        if (deviceLink != null) {
            try {
                deviceLink.close(initialVC, exceptionForCloseFromHere);
            } catch (Exception x) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "closeConnectionLink: could not close, :close: H2InboundLink hc: " + this.hashCode() + " device link close caught: " + x);

                    StringBuffer sb = new StringBuffer();
                    StackTraceElement[] trace = x.getStackTrace();
                    for (int i = 0; i < trace.length; i++) {
                        sb.append(" " + trace[i] + "\r\n");
                    }
                    sb.append("");
                    String s = sb.toString();

                    Tr.debug(tc, "closeConnectionLink: " + s);
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "closeConnectionLink: could not close, device link was null" + " :close: H2InboundLink hc: " + this.hashCode());
            }
        }
    }

    public boolean checkIfGoAwaySendingOrClosing() {

        synchronized (linkStatusSync) {

            if ((linkStatus != LINK_STATUS.CLOSING) && (linkStatus != LINK_STATUS.GOAWAY_SENDING)) {
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

    /**
     * Check to see if the connection is still initializing (in INIT state). If it is, update the state to OPEN.
     *
     * @return true if the link status is in INIT state
     */
    public boolean checkInitAndOpen() {
        synchronized (linkStatusSync) {
            if (linkStatus == LINK_STATUS.INIT) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "checkInitAndOpen: connection preface completed, set :linkStatus: to OPEN" + " H2InboundLink hc: " + this.hashCode());
                }
                linkStatus = LINK_STATUS.OPEN;
                return true;
            }
            return false;
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
                Tr.debug(tc, "close(vc,e): :linkstatus: is: " + linkStatus + " :close: H2InboundLink hc: " + this.hashCode() + "exception: " + e);
            }

            if ((linkStatus == LINK_STATUS.CLOSING) || (linkStatus == LINK_STATUS.GOAWAY_SENDING)
                || (linkStatus == LINK_STATUS.WAIT_TO_SEND_GOAWAY)) {
                // another thread is in charge of closing, or another thread has already armed the future to close
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close(vc,e): returning: close of muxLink is being done on a differnt thread" + " :close: H2InboundLink hc: " + this.hashCode());
                }
                return;
            }

            if (e == null) {
                // Attempt to close down cleanly if all streams are closed.

                //Determine if all streams are in half closed or closed state
                //If not, do nothing and return
                //If so, look to see if the GoAway frame has been sent
                //If not, trigger a timer and wait to send the GOAWAY frame
                //If so, call close on the TCP-Channel/Device-Channel below us

                H2StreamProcessor stream;
                for (Integer i : streamTable.keySet()) {
                    stream = streamTable.get(i);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "close(vc,e): looking at stream: " + stream.getId());
                    }

                    if (stream.getId() != 0 && !stream.isHalfClosed() && !stream.isStreamClosed() && highestLocalStreamId > -1) {
                        continue;
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "close(vc,e): stream not ready to close: " + stream.getId() + " :close: H2InboundLink hc: " + this.hashCode());
                        }
                        return;
                    }
                }
            }

            // All streams are either closed or in half closed, and a GOAWAY frame needs to be sent
            // Wait the timeout time and then send the GOAWAY frame with the last good stream

            linkStatus = LINK_STATUS.WAIT_TO_SEND_GOAWAY;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close(vc,e): loading up the wait to close timeout" + " :close: H2InboundLink hc: " + this.hashCode());
            }

            ScheduledExecutorService scheduler = CHFWBundle.getScheduledExecutorService();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close : scheduler : " + scheduler + " config : " + config);
            }
            connTimeout = new H2ConnectionTimeout(e);

            if (e == null) {
                // close cleanly if no other traffic has been received for this H2 connection within the timeout
                // Save the future so we can cancel it later on
                closeFuture = scheduler.schedule(connTimeout, config.getH2ConnCloseTimeout(), TimeUnit.SECONDS);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close(vc,e): close on link called with exception: " + e);
                }

                // do the close immediately on this thread
                connTimeout.run();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "close(vc,e): initLock count is : " + initLock.getCount());
                }

                // if we are waiting on connection initialization and an error occurred, release the latch
                if (0 < initLock.getCount()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "close(vc,e): wake up the initLock countDownLatch");
                    }
                    initLock.countDown();
                }
            }
        }
    }

    private class H2ConnectionTimeout implements Runnable {
        private final Exception e;

        public H2ConnectionTimeout(Exception inE) {
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
                if (e == null) {
                    streamTable.get(0).sendGOAWAYFrame(new Http2Exception("the http2 connection has timed out"));
                } else if (e instanceof Http2Exception) {
                    streamTable.get(0).sendGOAWAYFrame((Http2Exception) e);
                } else {
                    streamTable.get(0).sendGOAWAYFrame(new Http2Exception(e.getMessage()));
                }

            } catch (Exception x) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2ConnectionTimeout-run: exeception received while sending GOAWAY: " + " :close: H2InboundLink hc: " + hcDebug + " " + x);
                }
            } finally {

                closeConnectionLink(e);
            }
        }
    }

    public int getActiveClientStreams() {
        synchronized (streamCounterSync) {
            return this.activeClientStreams;
        }
    }

    public void incrementActiveClientStreams() {
        synchronized (streamCounterSync) {
            this.activeClientStreams++;
        }
    }

    public void decrementActiveClientStreams() {
        synchronized (streamCounterSync) {
            this.activeClientStreams--;
        }
    }

    /**
     * Remove the stream matching the given ID from the write tree, and decrement the number of open streams.
     *
     * @param int streamID
     */
    public void closeStream(H2StreamProcessor p) {
        // only place that should be dealing with the closed stream table,
        // be called by multiple stream objects at the same time, so sync access
        synchronized (streamOpenCloseSync) {
            if (p.getId() != 0) {
                writeQ.removeNodeFromQ(p.getId());
                this.closedStreams.add(p);
                if (p.getId() % 2 == 0) {
                    this.openPushStreams--;
                } else {
                    decrementActiveClientStreams();
                }
            }

            // Removes all streams that are older than STREAM_CLOSE_DELAY from the streamTable
            long currentTime = System.currentTimeMillis();
            while (closedStreams.peek() != null &&
                   currentTime - closedStreams.peek().getCloseTime() > STREAM_CLOSE_DELAY) {
                streamTable.remove(closedStreams.remove().getId());
            }
        }
    }

    /**
     * Get the stream processor for a given stream ID, if it exists
     *
     * @param streamID of the desired stream
     * @return a stream object if it's in the open stream table, or null if the
     *         ID is new or has already been removed from the stream table
     */
    public H2StreamProcessor getStream(int streamID) {
        H2StreamProcessor streamProcessor = null;

        streamProcessor = streamTable.get(streamID);
        return streamProcessor;
    }

    public int getHighestClientStreamId() {
        return highestClientStreamId;
    }

    public int getHighestServerStreamId() {
        return highestLocalStreamId;
    }

    /**
     * Set the authority string to use for the :authority header
     *
     * @param String
     */
    protected void setAuthority(String a) {
        this.authority = a;
    }

    /**
     * Get the server authority string to use for the :authority header. Needed for push promise frames.
     *
     * @return authority String
     */
    public String getAuthority() {
        return this.authority;
    }

    protected long getLastWriteTime() {
        synchronized (OutstandingWriteCountSync) {
            return lastWriteTime;
        }
    }

    protected int getconfiguredInactivityTimeout() {
        return configuredInactivityTimeout;
    }
}
