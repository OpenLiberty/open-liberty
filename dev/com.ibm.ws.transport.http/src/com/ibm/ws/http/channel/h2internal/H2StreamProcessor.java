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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.http.channel.h2internal.Constants.Direction;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.ws.http.channel.h2internal.exceptions.RefusedStreamException;
import com.ibm.ws.http.channel.h2internal.exceptions.StreamClosedException;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameContinuation;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePPHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.frames.FramePriority;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.frames.FrameWindowUpdate;
import com.ibm.ws.http.channel.h2internal.frames.utils;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * Represents an independent HTTP/2 stream
 * Thread safety is guaranteed via processNextFrame(), which handles new read or write frames on this stream
 */
public class H2StreamProcessor {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2StreamProcessor.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    // connection objects we're interested in
    private H2HttpInboundLinkWrap h2HttpInboundLinkWrap = null;
    private H2InboundLink muxLink = null;

    // bytes buffered by this stream
    private ArrayList<byte[]> headerBlock;
    private ArrayList<byte[]> dataPayload;

    // general stream settings
    private int myID = -1;
    private int weight = 0;
    private boolean exclusive = false;
    private int streamDependency = 0;
    private FrameTypes frameType;

    // keep track of the stream state
    private StreamState state = StreamState.IDLE;
    private Frame currentFrame;
    private boolean endStream = false;
    private long closeTime = Constants.INITIAL_CLOSE_TIME;

    // keep track of the state of this stream's headers
    private boolean headersCompleted = false;

    // the anticipated content length, as passed in from a content-length header
    private int expectedContentLength = -1;

    // set to true if this stream services a request with the CONNECT method
    private boolean isConnectStream = false;

    // objects to track http2 connection initialization
    private boolean connection_preface_settings_ack_rcvd = false;
    private boolean connection_preface_settings_rcvd = false;

    public static enum PROCESS_TYPE {
        DEFAULT, PAYLOAD_FIRST
    };

    public static enum ADDITIONAL_FRAME {
        FIRST_TIME, NO, RESET, GOAWAY, DATA
    };

    // the remote window, which we're keeping track of as a sender
    private long streamWindowUpdateWriteInitialSize;
    private long streamWindowUpdateWriteLimit;

    //change to 8192 to track better if this is occurring
    private final int MAX_TIME_TO_WAIT_FOR_WINDOW_UPDATE_MS = 8192;

    // the local window, which we're keeping track of as a receiver
    private long streamReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE;

    // a list of buffers to be read in by the WebContainer
    private volatile Queue<WsByteBuffer> streamReadReady = new ConcurrentLinkedQueue<WsByteBuffer>();
    private int streamReadSize = 0;
    private long actualReadCount = 0;
    // TODO: investigate if GRPC changes means we can remove this readLatch logic.
    private final CountDownLatch readLatch = new CountDownLatch(1);

    // latch used to block processing new data until the "first" data buffers from this stream are read by the application
    private CountDownLatch firstReadLatch = null;

    // handle various stream close conditions
    private boolean rstStreamSent = false;

    // keep track of how many empty data frames have been received
    private int emptyFrameReceivedCount = 0;

    /**
     * Create a stream processor initialized in idle state
     *
     * @param ID
     * @param H2HttpInboundLinkWrap
     * @param H2InboundLink
     */
    public H2StreamProcessor(Integer id, H2HttpInboundLinkWrap link, H2InboundLink m) {
        this(id, link, m, StreamState.IDLE);
    }

    /**
     * Create a stream processor initialized to a given http2 stream state
     *
     * @param ID
     * @param H2HttpInboundLinkWrap
     * @param H2InboundLink
     * @param StreamState
     */
    public H2StreamProcessor(Integer id, H2HttpInboundLinkWrap link, H2InboundLink m, StreamState state) {
        myID = id.intValue();
        h2HttpInboundLinkWrap = link;
        muxLink = m;
        // init the stream read window size to the max
        streamReadWindowSize = muxLink.maxReadWindowSize;
        updateStreamState(state);
        streamWindowUpdateWriteInitialSize = muxLink.getInitialWindowSize();
        streamWindowUpdateWriteLimit = muxLink.getInitialWindowSize();
    }

    /**
     * Complete the connection preface. At this point, we should have received the client connection preface string.
     * Now we need to make sure that the client sent a settings frame along with the preface, update our settings,
     * and send an empty settings frame in response to the client preface.
     *
     * @throws Http2Exception
     */
    protected void completeConnectionPreface() throws Http2Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "completeConnectionPreface entry: about to send preface SETTINGS frame");
        }
        FrameSettings settings;
        // send out a settings frame with any HTTP2 settings that the user may have changed
        if (Constants.SPEC_INITIAL_WINDOW_SIZE != this.streamReadWindowSize) {
            settings = new FrameSettings(0, -1, -1, this.muxLink.config.getH2MaxConcurrentStreams(), (int) this.streamReadWindowSize, this.muxLink.config.getH2MaxFrameSize(), -1, false);
        } else {
            settings = new FrameSettings(0, -1, -1, this.muxLink.config.getH2MaxConcurrentStreams(), -1, this.muxLink.config.getH2MaxFrameSize(), -1, false);
        }

        this.frameType = FrameTypes.SETTINGS;
        this.processNextFrame(settings, Direction.WRITING_OUT);

        if (Constants.SPEC_INITIAL_WINDOW_SIZE != muxLink.maxReadWindowSize) {
            // the user has changed the max connection read window, so we'll update that now
            FrameWindowUpdate wup = new FrameWindowUpdate(0, (int) muxLink.maxReadWindowSize, false);
            this.processNextFrame(wup, Direction.WRITING_OUT);
        }
    }

    /**
     * Update the state of this stream by passing in a given Frame to read or write. This method handles state validation, state transitions,
     * connection setting updates, frame responses, and error processing. Note this method is synchronized.
     *
     * @param Frame
     * @param Direction.WRITING_OUT or Direction.READING_IN
     * @throws ProtocolException
     * @throws StreamClosedException
     */
    public synchronized void processNextFrame(Frame frame, Constants.Direction direction) throws ProtocolException, StreamClosedException, FlowControlException {

        // Make it easy to follow frame processing in the trace by searching for "processNextFrame-" to see all frame processing
        boolean doDebugWhile = false;
        FlowControlException FCEToThrow = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processNextFrame-entry:  stream: " + myID + " frame type: " + frame.getFrameType().toString() + " direction: " + direction.toString()
                         + " H2InboundLink hc: " + muxLink.hashCode());
        }

        // if we've already sent a reset frame on this stream , process any window size changes then ignore the new frame
        if (rstStreamSent) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "{0} frame received on stream {1} after RST_STREAM sent", frame.getFrameType(), myID);
            }
            try {
                updateStreamReadWindow();
            } catch (Http2Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "hit an exception updating the read window on stream {0}", myID);
                }
            }
            return;
        }
        if (isStreamClosed()) {
            // stream is already closed
            if (direction.equals(Constants.Direction.WRITING_OUT) && !muxLink.isWriteContinuationExpected()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: stream is closed - cannot write out anything else on stream-id: " + myID);
                }
                if (frame.getFrameType().equals(FrameTypes.DATA)) {
                    // pass an exception to whatever servlet is writing
                    throw new StreamClosedException("stream was already closed!");
                } else {
                    return;
                }
            } else if (direction.equals(Constants.Direction.READ_IN) &&
                       !frame.getFrameType().equals(FrameTypes.PUSH_PROMISE)) {
                // handle a frame recieved after stream closure
                if (frame.getFrameType() == FrameTypes.PRIORITY || frame.getFrameType() == FrameTypes.RST_STREAM
                    || frame.getFrameType() == FrameTypes.WINDOW_UPDATE) {
                    // Ignore PRIORITY RST_STREAM and WINDOW_UPDATE in all closed situations
                    return;
                }
                if (muxLink.streamTable.containsKey(this.myID)) {
                    throw new StreamClosedException(frame.getFrameType() + " frame received on a closed stream");
                } else {
                    throw new ProtocolException(frame.getFrameType() + " frame received on a closed stream");

                }

            }
        }

        ADDITIONAL_FRAME addFrame = ADDITIONAL_FRAME.FIRST_TIME;
        Http2Exception addFrameException = null;
        H2RateState h2rs = muxLink.getH2RateState();

        while (addFrame != ADDITIONAL_FRAME.NO) {

            currentFrame = frame;

            // skip only first debug here, since it was done on entry
            if (doDebugWhile) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame- in while: stream-id: " + myID + " frame type: " + frame.getFrameType().toString() + " direction: " + direction.toString());
                }
            } else {
                doDebugWhile = true;
            }

            // if looping to RESET, then load it up now
            if (addFrame == ADDITIONAL_FRAME.RESET) {
                updateStreamState(StreamState.HALF_CLOSED_LOCAL);
                currentFrame = new FrameRstStream(myID, addFrameException.getErrorCode(), false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: exception encountered.  Sending RST_STREAM on stream "
                                 + myID + " with the error code " + addFrameException.getErrorString());
                }
                direction = Constants.Direction.WRITING_OUT;
            }

            // if looping to GOAWAY, then load it up now
            if (addFrame == ADDITIONAL_FRAME.GOAWAY) {
                updateStreamState(StreamState.HALF_CLOSED_LOCAL);

                // set link status, since we are in processNextFrame, no one else should be processing the link status
                muxLink.setStatusLinkToGoAwaySending();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: addFrame GOAWAY: HighestClientStreamId: " + muxLink.getGoawayPromisedStreamId());
                }
                currentFrame = new FrameGoAway(0, addFrameException.getMessage().getBytes(), addFrameException.getErrorCode(), muxLink.getGoawayPromisedStreamId(), false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: exception encountered.  Sending a GOAWAY frame with the error code " + addFrameException.getErrorString());
                }

                direction = Constants.Direction.WRITING_OUT;
            }

            frameType = currentFrame.getFrameType();

            // play with the flags so we don't try to reset or goaway twice
            if (frameType == FrameTypes.RST_STREAM && addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                addFrame = ADDITIONAL_FRAME.RESET;
            } else if (frameType == FrameTypes.GOAWAY && addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                addFrame = ADDITIONAL_FRAME.GOAWAY;
            }

            // validate the current frame.  If it's not valid, send a RESET if applicable otherwise close the connection
            try {
                //If the frame is being read, validate against the server's connection settings
                if (direction == Constants.Direction.READ_IN) {
                    currentFrame.validate(muxLink.getLocalConnectionSettings());
                } else {
                    //If the frame is being written, validate against the client's connection settings
                    currentFrame.validate(muxLink.getRemoteConnectionSettings());
                }

            } catch (Http2Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: " + currentFrame.getFrameType() + " received on stream " + this.myID +
                                 " is not a valid frame: " + e.getErrorString());
                }
                if (addFrame == ADDITIONAL_FRAME.FIRST_TIME || addFrame == ADDITIONAL_FRAME.RESET ||
                    addFrame == ADDITIONAL_FRAME.GOAWAY) {
                    if (e.isConnectionError()) {
                        addFrame = ADDITIONAL_FRAME.GOAWAY;
                    } else {
                        addFrame = ADDITIONAL_FRAME.RESET;
                    }
                    addFrameException = e;
                } else {
                    // writing or reading of a Reset or Goaway has failed.
                    // if it is reading of Reset that failed, then send a Reset.
                    if (direction == Constants.Direction.READ_IN) {
                        addFrame = ADDITIONAL_FRAME.RESET;
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                }
                continue;
            }

            if (direction == Constants.Direction.READ_IN) {

                if (muxLink.checkIfGoAwaySendingOrClosing()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processNextFrame: " + currentFrame.getFrameType() + " received on stream " + this.myID +
                                     " after a GOAWAY was sent or Closing invoked.  This frame will be ignored.");
                    }
                    return;
                }
                if (addFrame == null || addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                    // check to see if this connection is misbehaving
                    if (isControlFrame(frame)) {
                        h2rs.incrementReadControlFrameCount();
                    } else {
                        h2rs.incrementReadNonControlFrameCount();
                    }
                    // check to see if this connection is misbehaving
                    if (h2rs.isControlRatioExceeded() || h2rs.isStreamMisbehaving(emptyFrameReceivedCount)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "processNextFrame: too many no-op frames received, sending GOAWAY");
                        }
                        addFrame = ADDITIONAL_FRAME.GOAWAY;
                        if (h2rs.isStreamMisbehaving(emptyFrameReceivedCount)) {
                            addFrameException = new ProtocolException("too many empty frames generated");
                        } else {
                            addFrameException = new ProtocolException("too many control frames generated");
                        }
                        continue;
                    }
                }

                // This frame type is artificially generated, process it as a headers frame,
                // as if it had come in off the wire
                if (frameType == FrameTypes.PUSHPROMISEHEADERS) {
                    getHeadersFromFrame();
                    setHeadersComplete();
                    try {
                        processCompleteHeaders(true);
                        setReadyForRead();
                    } catch (Http2Exception he) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "H2StreamProcessor.sendRequestToWc(): ProcessCompleteHeaders Exception: " + he);
                        }

                        //Send a reset so that the client knows the push_promise is dead
                        addFrame = ADDITIONAL_FRAME.RESET;
                        addFrameException = he;
                        continue;

                    }

                    return;

                }

                // Header frames must be received in a contiguous chunk; cannot interleave across streams
                if (muxLink.isContinuationExpected() && (frameType != FrameTypes.CONTINUATION || !muxLink.isContinuationExpected())) {
                    addFrame = ADDITIONAL_FRAME.GOAWAY;
                    addFrameException = new ProtocolException("Did not receive the expected continuation frame");
                    continue;
                }

                if (frameType == FrameTypes.SETTINGS ||
                    frameType == FrameTypes.GOAWAY || frameType == FrameTypes.PING) {

                    switch (frameType) {
                        case SETTINGS:
                            try {
                                processSETTINGSFrame();
                            } catch (Http2Exception e) {
                                if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                                    addFrame = ADDITIONAL_FRAME.GOAWAY;
                                    addFrameException = e;
                                }
                                continue;
                            }
                            break;

                        case GOAWAY:
                            processGOAWAYFrame();
                            updateStreamState(StreamState.CLOSED);
                            break;

                        case PING:
                            try {
                                processPINGFrame();
                            } catch (Http2Exception e) {
                                if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                                    addFrame = ADDITIONAL_FRAME.GOAWAY;
                                    addFrameException = e;
                                }
                                continue;
                            }
                            break;

                        default:
                            break;
                    }
                    return;
                }

                // Stream oriented frame to process
                try {
                    verifyReadFrameSequence();
                } catch (Http2Exception e) {
                    if (e.isConnectionError()) {
                        addFrame = ADDITIONAL_FRAME.GOAWAY;
                    } else if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                        addFrame = ADDITIONAL_FRAME.RESET;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    addFrameException = e;
                    continue;
                }

                // made it here, so frame seems to be legit given the frame state.

                // since we have already verify priority frame or window update frame  is allowed,
                // and since it will not change the state of the connection, process
                // the priority and window_update frame outside of state specific processing
                if (frameType == FrameTypes.PRIORITY) {
                    processPriorityFrame();
                    return;
                }
                if (frameType == FrameTypes.RST_STREAM) {
                    processRstFrame();
                    synchronized (this) {
                        this.notifyAll();
                    }
                    return;
                }

                try {
                    if (frameType == FrameTypes.WINDOW_UPDATE) {
                        processWindowUpdateFrame();
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                } catch (Http2Exception e) {
                    if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                        if (e.isConnectionError()) {
                            addFrame = ADDITIONAL_FRAME.GOAWAY;
                        } else {
                            addFrame = ADDITIONAL_FRAME.RESET;
                        }
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    continue;
                }

                // check to see if client is sending us data with a payload, so we can update the local frame control window
                // after processing it
                try {
                    updateStreamReadWindow();
                } catch (Http2Exception e) {
                    if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                        if (e.isConnectionError()) {
                            addFrame = ADDITIONAL_FRAME.GOAWAY;
                        } else {
                            addFrame = ADDITIONAL_FRAME.RESET;
                        }
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    continue;
                }

                try {
                    readWriteTransitionState(direction);
                } catch (CompressionException e) {
                    // if this is a compression exception, something has gone very wrong and the connection is hosed
                    if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                        addFrame = ADDITIONAL_FRAME.GOAWAY;
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    continue;
                } catch (Http2Exception e) {
                    if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                        if (e.isConnectionError()) {
                            addFrame = ADDITIONAL_FRAME.GOAWAY;
                        } else {
                            addFrame = ADDITIONAL_FRAME.RESET;
                        }
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    continue;
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame Writing out data");
                }
                try {
                    verifyWriteFrameSequence();

                    // check to see if this connection is misbehaving
                    if (addFrame == null || addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
                        if (h2rs.isControlRatioExceeded()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "processNextFrame: too many control processed, sending GOAWAY");
                            }
                            addFrame = ADDITIONAL_FRAME.GOAWAY;
                            addFrameException = new ProtocolException("too many control frames generated");
                            addFrameException.setConnectionError(false);
                            continue;
                        }
                    }
                    // If there's a connection error exception, pass it along so that when
                    // the close connection is processed, we clean up any outstanding requests/responses
                    if (addFrameException != null && addFrameException.isConnectionError()) {
                        readWriteTransitionState(direction, addFrameException);
                    } else {
                        readWriteTransitionState(direction);
                    }

                } catch (CompressionException e) {
                    // if this is a compression exception, something has gone very wrong and the connection is hosed
                    if ((addFrame == ADDITIONAL_FRAME.FIRST_TIME) || (addFrame == ADDITIONAL_FRAME.RESET)) {
                        Tr.info(tc, "A Header compression error occurred!  This connection is no longer valid.");
                        addFrame = ADDITIONAL_FRAME.GOAWAY;
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    continue;
                } catch (Http2Exception e) {
                    if ((addFrame == ADDITIONAL_FRAME.FIRST_TIME) || (addFrame == ADDITIONAL_FRAME.RESET)) {
                        if ((frameType == FrameTypes.DATA) && (e instanceof FlowControlException)) {
                            FCEToThrow = (FlowControlException) e;
                        }
                        if (e.isConnectionError()) {
                            addFrame = ADDITIONAL_FRAME.GOAWAY;
                        } else {
                            addFrame = ADDITIONAL_FRAME.RESET;
                        }
                        addFrameException = e;
                    } else {
                        addFrame = ADDITIONAL_FRAME.NO;
                    }
                    continue;
                }
            }
            addFrame = ADDITIONAL_FRAME.NO;
        }
        // will only throw FCE if we were writing DATA frame and got an FCE
        if (FCEToThrow != null) {
            throw FCEToThrow;
        }
    }

    /**
     * Transitions the stream state, give the previous state and current frame. Handles writes and error processing as needed.
     *
     * @param Direction.WRITING_OUT or Direction.READING_IN
     * @throws Http2Exception
     */
    private void readWriteTransitionState(Constants.Direction direction) throws Http2Exception {
        readWriteTransitionState(direction, null);
    }

    /**
     * Transitions the stream state, give the previous state and current frame. Handles writes and error processing as needed.
     *
     * @param Direction.WRITING_OUT or Direction.READING_IN
     * @throws Http2Exception
     */
    private void readWriteTransitionState(Constants.Direction direction, Exception e) throws Http2Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readWriteTransitionState: entry: frame type: " + currentFrame.getFrameType() + " state: " + state);
        }

        if (currentFrame.getFrameType() == FrameTypes.GOAWAY
            || currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
            try {
                writeFrameSync();
            } finally {
                rstStreamSent = true;
                muxLink.getH2RateState().setStreamReset();
                this.updateStreamState(StreamState.CLOSED);
                if (currentFrame.getFrameType() == FrameTypes.GOAWAY) {
                    muxLink.closeConnectionLink(e);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "readWriteTransitionState: return: state: " + state);
                }
            }
            return;

        }

        switch (state) {
            case IDLE:
                processIdle(direction);
                break;

            case RESERVED_LOCAL:
                processReservedLocal(direction);
                break;

            case RESERVED_REMOTE:
                processReservedRemote(direction);
                break;

            case OPEN:
                processOpen(direction);
                break;

            case HALF_CLOSED_REMOTE:
                processHalfClosedRemote(direction);
                break;

            case HALF_CLOSED_LOCAL:
                processHalfClosedLocal(direction);
                break;

            case CLOSED:
                processClosed(direction);
                break;

            default:
                break;

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readWriteTransitionState: exit: state: " + state);
        }
    }

    /**
     * Update the stream state and provide logging, if enabled
     *
     * @param state
     */
    private void updateStreamState(StreamState state) {
        this.state = state;
        if (StreamState.CLOSED.equals(state)) {
            setCloseTime(System.currentTimeMillis());
            muxLink.closeStream(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "current stream state for stream " + this.myID + " : " + this.state);
        }
    }

    /**
     * Update the promised stream state to Reserved_Local
     *
     */
    public void initializePromisedStream() {
        this.updateStreamState(StreamState.RESERVED_LOCAL);
    }

    /**
     * Helper method to process a SETTINGS frame received from the client. Since the protocol utilizes SETTINGS frames for
     * initialization, some special logic is needed.
     *
     * @throws FlowControlException
     */
    private void processSETTINGSFrame() throws FlowControlException, Http2Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processSETTINGSFrame entry:\n" + currentFrame.toString());
        }
        // check if this is the first non-ACK settings frame received; if so, update connection init state
        if (!connection_preface_settings_rcvd && !((FrameSettings) currentFrame).flagAckSet()) {
            connection_preface_settings_rcvd = true;
        }
        if (((FrameSettings) currentFrame).flagAckSet()) {
            // if this is the first ACK frame, update connection init state
            if (!connection_preface_settings_ack_rcvd) {
                connection_preface_settings_ack_rcvd = true;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "received a new settings frame: updating connection settings and sending out SETTINGS ACK");
            }

            // if the INITIAL_WINDOW_SIZE has changed, need to change it for this streams, and all the other streams on this connection
            if (((FrameSettings) currentFrame).getInitialWindowSize() != -1) {
                int newSize = ((FrameSettings) currentFrame).getInitialWindowSize();
                muxLink.changeInitialWindowSizeAllStreams(newSize);
            }

            // update the SETTINGS for this connection
            muxLink.getRemoteConnectionSettings().updateSettings((FrameSettings) currentFrame);
            muxLink.getVirtualConnection().getStateMap().put("h2_frame_size", muxLink.getRemoteConnectionSettings().getMaxFrameSize());

            // immediately send out ACK (an empty SETTINGS frame with the ACK flag set)
            currentFrame = new FrameSettings(0, -1, -1, -1, -1, -1, -1, false);
            currentFrame.setAckFlag();

            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processSETTINGSFrame:  stream: " + myID + " frame type: " + currentFrame.getFrameType().toString() + " direction: "
                                 + Direction.WRITING_OUT
                                 + " H2InboundLink hc: " + muxLink.hashCode());
                }

                writeFrameSync();

            } catch (FlowControlException e) {
                // FlowControlException cannot occur for FrameTypes.SETTINGS, so do nothing here but debug
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeSync caught (logically unexpected) FlowControlException: " + e);
                }
            }
        }

        // check to see if the current connection should be marked as initialized; if so, notify stream 1 to stop waiting
        if (connection_preface_settings_rcvd && connection_preface_settings_ack_rcvd) {
            if (muxLink.checkInitAndOpen()) {
                muxLink.initLock.countDown();
            }
        }

    }

    private void processPriorityFrame() {
        FramePriority castFrame = (FramePriority) currentFrame;

        /*
         * The PRIORITY frame always identifies a stream. If a PRIORITY frame is received with a stream identifier of 0x0,
         * the recipient MUST respond with a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
         *
         * Weight: An unsigned 8-bit integer representing a priority weight for the stream (see Section 5.3).
         * Add one to the value to obtain a weight between 1 and 256.
         *
         */

        exclusive = castFrame.isExclusive();
        streamDependency = castFrame.getStreamDependency();
        weight = castFrame.getWeight();

        muxLink.getWorkQ().updateNodeFrameParameters(this.myID, weight, streamDependency, exclusive);
    };

    private void processWindowUpdateFrame() throws FlowControlException {

        FrameWindowUpdate castFrame = (FrameWindowUpdate) currentFrame;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processWindowUpdateFrame: streamID: " + castFrame.getStreamId() + " desired increment: " + castFrame.getWindowSizeIncrement());
        }

        if (myID == 0) {
            // this WindowUpdate frame is for the connection, not the stream
            // this call does not need to be synchronized, since frame processing per connection is serial at this point in the code flow for the update frame
            muxLink.incrementConnectionWindowUpdateLimit(castFrame.getWindowSizeIncrement());
        } else {
            // Increment size is 31 bits, max.   make sure adding it to Write Limit does go over 0x7FFFFFFF
            long temp = streamWindowUpdateWriteLimit + castFrame.getWindowSizeIncrement();
            temp = temp & Constants.LONG_31BIT_FILTER;
            if (temp != 0) {
                // number would be bigger that 2^31 - 1, which it can't be
                String s = "processWindowUpdateFrame: out of bounds increment, current stream write limit: " + streamWindowUpdateWriteLimit
                           + " total would have been: " + temp;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, s);
                }
                FlowControlException e = new FlowControlException(s);
                // since the ID for this stream is 0, this is a stream error rather than a connection error
                e.setConnectionError(false);
                throw e;
            }

            streamWindowUpdateWriteLimit += castFrame.getWindowSizeIncrement();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processWindowUpdateFrame: new write limit is: " + streamWindowUpdateWriteLimit);
            }
        }

    }

    /**
     * If this stream is receiving a DATA frame, the local read window needs to be updated. Additionally,
     * a WINDOW_UPDATE frame will be sent for both the connection and stream to update the windows.
     */
    private void updateStreamReadWindow() throws Http2Exception {

        if (currentFrame instanceof FrameData) {
            long frameSize = currentFrame.getPayloadLength();
            if (frameSize > 0) {
                synchronized (muxLink.getReadWindowSync()) {
                    // given the current data frame size, update the read windows
                    streamReadWindowSize -= frameSize; // decrement stream read window
                    muxLink.connectionReadWindowSize -= frameSize; // decrement connection read window

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "updateStreamReadWindow: stream read limit: " + streamReadWindowSize + " connection limit:"
                                     + muxLink.connectionReadWindowSize);
                    }
                    if (streamReadWindowSize < 0 || muxLink.connectionReadWindowSize < 0) {
                        throw new FlowControlException("Too much data received from the remote client");
                    }

                    // update the connection read limit to its max
                    int windowChange = (int) (muxLink.maxReadWindowSize - muxLink.connectionReadWindowSize);
                    FrameWindowUpdate wuf = new FrameWindowUpdate(0, windowChange, false);
                    muxLink.getStream(0).processNextFrame(wuf, Direction.WRITING_OUT);
                    muxLink.connectionReadWindowSize += windowChange;

                    // update the stream read limit to its max
                    windowChange = (int) (muxLink.maxReadWindowSize - this.streamReadWindowSize);
                    Frame savedFrame = currentFrame; // save off the current frame
                    if (!currentFrame.flagEndStreamSet()) {
                        currentFrame = new FrameWindowUpdate(myID, windowChange, false);
                        writeFrameSync();
                        streamReadWindowSize += windowChange;
                        currentFrame = savedFrame;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "updateStreamReadWindow: window updates sent; new stream read limit: " + streamReadWindowSize
                                     + " connection limit:" + muxLink.connectionReadWindowSize);
                    }
                }
            }
        }
    }

    /**
     * Tell this stream to attempt to start writing out data frames
     */
    private void flushDataWaitingForWindowUpdate() {
        synchronized (this) {
            this.notifyAll();
        }
    }

    protected void connectionWindowSizeUpdated() {
        flushDataWaitingForWindowUpdate();
    }

    /**
     * Updates the initial window size for this stream. If any data frames are waiting for an increased window size,
     * write them out if the new window size allows it.
     *
     * @param newSize - new window size
     * @throws FlowControlException
     */
    protected void updateInitialWindowsUpdateSize(int newSize) throws FlowControlException {
        // this method should only be called by the thread that came in on processNewFrame.
        // newSize should be treated as an unsigned 32-bit int

        if (myID == 0) {
            // the control stream doesn't care about initial window size updates
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateInitialWindowsUpdateSize entry: stream {0} newSize: {1}", myID, newSize);
        }

        long diff = newSize - streamWindowUpdateWriteInitialSize;

        streamWindowUpdateWriteInitialSize = newSize;
        streamWindowUpdateWriteLimit += diff;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "streamWindowUpdateWriteInitialSize updated to: " + streamWindowUpdateWriteInitialSize);
            Tr.debug(tc, "streamWindowUpdateWriteLimit updated to: " + streamWindowUpdateWriteLimit);
        }

        // if any data frames were waiting for a window update, write them out now
        flushDataWaitingForWindowUpdate();
    }

    public void sendGOAWAYFrame(Http2Exception e) throws Http2Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendGOAWAYFrame: " + " :close: H2InboundLink hc: " + muxLink.hashCode());
        }
        boolean doGoAwayFromHere = muxLink.setStatusLinkToGoAwaySending();
        if (!doGoAwayFromHere) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sendGOAWAYFrame: another thread is handling the close" + " :close: H2InboundLink hc: " + muxLink.hashCode());
            }

            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sendGOAWAYFrame sending a GOAWAY with Last-Stream-ID " + muxLink.getGoawayPromisedStreamId()
                         + " and exception " + e.toString());
        }

        // send out a GoAway in response;
        Frame frame = new FrameGoAway(0, e.getMessage().getBytes(), e.getErrorCode(), muxLink.getGoawayPromisedStreamId(), false);
        processNextFrame(frame, Constants.Direction.WRITING_OUT);
    }

    private void processGOAWAYFrame() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processGOAWAYFrame entry: begin connection shutdown and send reciprocal GOAWAY to client" + " :close: H2InboundLink hc: " + muxLink.hashCode());
        }
        boolean doGoAwayFromHere = muxLink.setStatusLinkToGoAwaySending();
        if (!doGoAwayFromHere) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sendGOAWAYFrame: another thread is handling the close" + " :close: H2InboundLink hc: " + muxLink.hashCode());
            }
            return;
        }
        this.updateStreamState(StreamState.CLOSED);

        // send out a goaway in response; return the same last stream, for now
        currentFrame = new FrameGoAway(0, new byte[0], 0, muxLink.getGoawayPromisedStreamId(), false);

        try {
            writeFrameSync();
        } catch (FlowControlException e) {
            // FlowControlException cannot occur for FrameTypes.GOAWAY, so do nothing here but debug
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeSync caught (logically unexpected) FlowControlException: " + e);
            }
        } catch (Http2Exception e) {
            // we are closing anyway.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeSync caught Http2Exception: " + e);
            }
        } finally {

            muxLink.closeConnectionLink(null);
        }
    }

    private void processPINGFrame() throws Http2Exception {
        if (currentFrame.flagAckSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processPINGFrame: ignore PING received with ACK set");
            }
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processPINGFrame entry: send reciprocal PING frame to client");
        }
        byte[] data = ((FramePing) currentFrame).getPayload();

        // respond to this PING with the reveived opaque data payload
        currentFrame = new FramePing(0, data, false);
        currentFrame.setAckFlag();
        try {
            writeFrameSync();
        } catch (FlowControlException e) {
            // FlowControlException cannot occur for FrameTypes.PING, so do nothing here but debug
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeSync caught (logically unexpected) FlowControlException: " + e);
            }
        }
    }

    /**
     * Process an incoming RstStream Frame: log the error and close this stream
     */
    private void processRstFrame() {
        int error = ((FrameRstStream) currentFrame).getErrorCode();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processRstFrame: error received from peer: " + utils.getErrorFromCode(error));
        }
        this.updateStreamState(StreamState.CLOSED);
    }

    /**
     * Perform operations to transition into IDLE state
     *
     * @param direction
     * @throws CompressionException
     * @throws FlowControlException
     * @throws ProtocolException
     */
    private void processIdle(Constants.Direction direction) throws Http2Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processIdle entry: stream " + myID);
        }
        // Can only receive HEADERS or PRIORITY frame in Idle state
        if (direction == Constants.Direction.READ_IN) {
            if (frameType == FrameTypes.HEADERS) {

                // check to see if too many client streams are currently open for this stream's h2 connection
                muxLink.incrementActiveClientStreams();
                if (muxLink.getActiveClientStreams() > muxLink.getLocalConnectionSettings().getMaxConcurrentStreams()) {
                    RefusedStreamException rse = new RefusedStreamException("too many client-initiated streams are currently active; rejecting this stream");
                    rse.setConnectionError(false);
                    throw rse;
                }

                processHeadersPriority();
                getHeadersFromFrame();

                if (currentFrame.flagEndHeadersSet()) {
                    processCompleteHeaders(false);
                    setHeadersComplete();
                } else {
                    muxLink.setContinuationExpected(true);
                }

                if (currentFrame.flagEndStreamSet()) {
                    endStream = true;
                    updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                    if (currentFrame.flagEndHeadersSet()) {
                        setReadyForRead();
                    }
                } else {
                    updateStreamState(StreamState.OPEN);
                }
            }
        } else {
            // send out a HEADER frame and update the stream state to OPEN
            if (frameType == FrameTypes.HEADERS) {
                updateStreamState(StreamState.OPEN);
            }
            writeFrameSync();
        }
    }

    private int passCount = 0;

    private void processOpen(Constants.Direction direction) throws ProtocolException, FlowControlException, CompressionException, Http2Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processOpen entry: stream " + myID);
        }

        if (direction == Constants.Direction.READ_IN) {
            if (frameType == FrameTypes.DATA) {
                if (!h2HttpInboundLinkWrap.getIsGrpc()) {
                    getBodyFromFrame();
                    if (currentFrame.flagEndStreamSet()) {
                        endStream = true;
                        updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                        processCompleteData(true);
                        setReadyForRead();
                    }
                } else {
                    if (passCount == 0) {
                        // latch so we don't overwrite the first data frame that comes in
                        firstReadLatch = new CountDownLatch(1);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "processOpen: first DATA frame read. using firstReadLatch of: " + firstReadLatch.hashCode());
                        }
                        passCount++;
                        getBodyFromFrame();
                        if (currentFrame.flagEndStreamSet()) {
                            endStream = true;
                            updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                        }
                        processCompleteData(true);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "processOpen: first DATA frame read. calling setReadyForRead() getEndStream returns: " + this.getEndStream());
                        }
                        setReadyForRead();
                    } else {
                        dataPayload = null;
                        // wait until ALL of the data buffered on this stream is read, only one time, after that
                        // we should be streaming up to the callback in the Webcontainer
                        if (passCount == 1) {
                            passCount++;
                            try {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "processOpen: another DATA frame received, wait for the first data frame to get processed completely");
                                }

                                firstReadLatch.await();

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "processOpen: finished waiting for first DATA to be fully processed");
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        getBodyFromFrame();
                        if (currentFrame.flagEndStreamSet()) {
                            endStream = true;
                            updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                        }

                        WsByteBuffer buf = processCompleteData(false);

                        if (buf != null) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "calling setNewBodyBuffer with: " + buf);
                            }
                            // store the buffer and call async complete()
                            h2HttpInboundLinkWrap.setAndStoreNewBodyBuffer(buf);
                            h2HttpInboundLinkWrap.invokeAppComplete();
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "did not call setNewBodyBuffer. buf was null");
                            }
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "processOpen: calling setReadyForRead() getEndStream returns: " + this.getEndStream());
                        }
                    }
                }

            } else if (frameType == FrameTypes.CONTINUATION ||
                       (frameType == FrameTypes.HEADERS)) {
                // if this is a header frame, it must be trailer data
                getHeadersFromFrame();
                if (currentFrame.flagEndHeadersSet()) {
                    muxLink.setContinuationExpected(false);
                    processCompleteHeaders(false);
                    setHeadersComplete();
                    if (currentFrame.flagEndStreamSet()) {
                        endStream = true;
                        updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                        setReadyForRead();
                    }
                } else {
                    muxLink.setContinuationExpected(true);
                }
            }

        } else {
            // write out the current frame and update the stream state
            if (frameType == FrameTypes.PUSH_PROMISE) {
                // writing out a PP doesn't have any effect on the current stream, but rather the promised stream

            } else if (frameType == FrameTypes.HEADERS || frameType == FrameTypes.CONTINUATION) {
                if (currentFrame.flagEndHeadersSet()) {
                    if (currentFrame.flagEndStreamSet()) {
                        endStream = true;
                        updateStreamState(StreamState.HALF_CLOSED_LOCAL);
                    }
                } else {
                    if (currentFrame.flagEndStreamSet()) {
                        endStream = true;
                    }
                }

            }
            boolean writeCompleted = writeFrameSync();
            if (frameType == FrameTypes.DATA && writeCompleted && currentFrame.flagEndStreamSet()) {
                endStream = true;
                updateStreamState(StreamState.HALF_CLOSED_LOCAL);
            }
        }
    }

    /**
     * @param direction
     */
    private void processClosed(Constants.Direction direction) {
        //
    }

    /**
     * @param direction
     */
    private void processHalfClosedLocal(Constants.Direction direction) throws FlowControlException, Http2Exception {
        // A stream transitions from this state to "closed" when a frame that
        // contains an END_STREAM flag is received or when either peer sends
        // a RST_STREAM frame.
        if (direction == Direction.WRITING_OUT) {
            writeFrameSync();
            if (currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
                endStream = true;
                updateStreamState(StreamState.HALF_CLOSED_LOCAL);
            }
        } else if (currentFrame.getFrameType() == FrameTypes.RST_STREAM ||
                   currentFrame.flagEndStreamSet()) {
            endStream = true;
            updateStreamState(StreamState.CLOSED);
        }
    }

    /**
     * @param direction
     * @throws CompressionException
     * @throws ProtocolException
     */
    private void processHalfClosedRemote(Constants.Direction direction) throws FlowControlException, CompressionException, ProtocolException, Http2Exception {
        // A stream can transition from this state to "closed" by sending a
        // frame that contains an END_STREAM flag or when either peer sends a
        // RST_STREAM frame.
        if (direction == Direction.WRITING_OUT) {
            boolean writeCompleted = writeFrameSync();
            if (frameType == FrameTypes.HEADERS || frameType == FrameTypes.CONTINUATION) {
                if (currentFrame.flagEndHeadersSet()) {
                    muxLink.setWriteContinuationExpected(false);
                } else {
                    muxLink.setWriteContinuationExpected(true);
                }
            }
            if ((currentFrame.getFrameType() == FrameTypes.RST_STREAM || currentFrame.flagEndStreamSet() && !muxLink.isWriteContinuationExpected())
                && writeCompleted) {
                endStream = true;
                updateStreamState(StreamState.CLOSED);
            }
        } else if (currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
            endStream = true;
            updateStreamState(StreamState.CLOSED);
        } else if (frameType == FrameTypes.CONTINUATION) {
            getHeadersFromFrame();
            if (currentFrame.flagEndHeadersSet()) {
                processCompleteHeaders(false);
                setHeadersComplete();
                setReadyForRead();
            }
        }
    }

    /**
     * @param direction
     */
    private void processReservedRemote(Constants.Direction direction) {
        // should we ever get here?
    }

    /**
     * @param direction
     */
    private void processReservedLocal(Constants.Direction direction) throws FlowControlException, Http2Exception {
        if (direction == Constants.Direction.WRITING_OUT) {
            if (currentFrame.getFrameType() == FrameTypes.HEADERS || currentFrame.getFrameType() == FrameTypes.CONTINUATION) {
                if (currentFrame.flagEndHeadersSet()) {
                    updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                }
            }
            writeFrameSync();
        }
    }

    /**
     * Check to see if a writing out a frame will cause the stream or connection window to go exceeded
     *
     * @return true if the write window would be exceeded by writing the frame
     */
    private boolean isWindowLimitExceeded(FrameData dataFrame) {
        if (streamWindowUpdateWriteLimit - dataFrame.getPayloadLength() < 0 ||
            muxLink.getWorkQ().getConnectionWriteLimit() - dataFrame.getPayloadLength() < 0) {
            // would exceed window update limit
            String s = "Cannot write Data Frame because it would exceed the stream window update limit."
                       + "streamWindowUpdateWriteLimit: " + streamWindowUpdateWriteLimit
                       + "\nstreamWindowUpdateWriteInitialSize: " + streamWindowUpdateWriteInitialSize
                       + "\nconnection window size: " + muxLink.getWorkQ().getConnectionWriteLimit()
                       + "\nframe size: " + dataFrame.getPayloadLength();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, s);
            }
            return true;
        }
        return false;
    }

    /**
     * Send an artificially created H2 request from a push_promise up to the WebContainer
     */
    public void sendRequestToWc(FramePPHeaders frame) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "H2StreamProcessor.sendRequestToWc()");
        }

        if (null == frame) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "H2StreamProcessor.sendRequestToWc(): Frame is null");
            }
        } else {
            // Make the headers frame look like it just came in
            WsByteBuffer buf = frame.buildFrameForWrite();
            TCPReadRequestContext readi = h2HttpInboundLinkWrap.getConnectionContext().getReadInterface();
            readi.setBuffer(buf);

            // Call the synchronized method to handle the frame
            try {
                processNextFrame(frame, Constants.Direction.READ_IN);
            } catch (Http2Exception he) {
                // ProcessNextFrame() sends a reset/goaway if an error occurs, nothing left but to clean up
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "H2StreamProcessor.sendRequestToWc(): ProcessNextFrame() error, Exception: " + he);
                }

                // Free the buffer
                buf.release();
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "H2StreamProcessor.sendRequestToWc()");
        }

    }

    private void verifyReadFrameSequence() throws ProtocolException, StreamClosedException {

        if (!currentFrame.isReadFrame()) {
            throw new ProtocolException("Write frame was given for Read frame processing");
        }

        switch (frameType) {

            case DATA:
                // (spec) If a DATA frame is received whose stream is not in "open" or "half-closed (local)" state, the
                // recipient MUST respond with a stream error (Section 5.4.2) of type  STREAM_CLOSED.

                if (state == StreamState.IDLE) {
                    throw new ProtocolException("DATA Frame Received in the wrong state of: " + state);
                }
                if (!(state == StreamState.OPEN || state == StreamState.HALF_CLOSED_LOCAL)) {
                    StreamClosedException se = new StreamClosedException("DATA Frame Received in the wrong state of: " + state);
                    se.setConnectionError(false);
                    throw se;
                }
                break;

            case HEADERS:
                // (non-spec) HEADERS frame is only read by the server side if the connection was in idle state,
                // meaning the client/remote side is starting a new stream and sending a request on it, or if the
                // stream was in open state, meaning that trailer headers were sent
                if (state == StreamState.HALF_CLOSED_REMOTE || state == StreamState.CLOSED) {
                    throw new StreamClosedException("HEADERS Frame Received in the wrong state of: " + state);
                } else if (state != StreamState.IDLE && state != StreamState.OPEN) {
                    throw new ProtocolException("HEADERS Frame Received in the wrong state of: " + state);
                } else if (state == StreamState.OPEN && !currentFrame.flagEndStreamSet()) {
                    throw new ProtocolException("second HEADERS frame received with no EOS set");
                } else if (isConnectStream) {
                    ProtocolException pe = new ProtocolException("HEADERS frame received on a CONNECT stream");
                    pe.setConnectionError(false);
                    throw pe;
                }
                break;

            case PRIORITY:
                // (spec) The PRIORITY frame can be sent on a stream in any state, though it
                // cannot be sent between consecutive frames that comprise a single header block

                break;

            case RST_STREAM:
                // (spec) After receiving a RST_STREAM  on a stream, the receiver MUST NOT send additional frames for that
                //  stream, with the exception of PRIORITY.

                // (spec) RST_STREAM frames MUST NOT be sent for a stream in the "idle" state.
                // If a RST_STREAM frame identifying an idle stream is received, the recipient MUST treat
                // this as a connection error (Section 5.4.1) of type PROTOCOL_ERROR.
                if (state == StreamState.IDLE) {
                    throw new ProtocolException("RST_STREAM Frame Received in the wrong state of: " + state);
                }

                break;

            case PUSH_PROMISE:
                // As an HTTP/2 server, and not a client, this code should not receive a PUSH_PROMISE frame
                throw new ProtocolException("PUSH_PROMISE Frame Received on server side");

                //case PING:   PING frame is not stream based.
                //      break;

                //case GOAWAY:   GOAWAY is not stream based, but does have some stream awareness, see spec.
                //      break;

                // case SETTINGS:  Setting is not stream based.
                //      break;

            case WINDOW_UPDATE:
                if (state == StreamState.IDLE && myID != 0) {
                    throw new ProtocolException("WINDOW_UPDATE Frame Received in the wrong state of: " + state);
                }
                break;

            case CONTINUATION:
                if (state == StreamState.IDLE) {
                    throw new ProtocolException("CONTINUATION Frame Received in the wrong state of: " + state);
                } else if (state == StreamState.CLOSED) {
                    throw new StreamClosedException("CONTINUATION Frame Received in the wrong state of: " + state);
                } else if (!muxLink.isContinuationExpected()) {
                    throw new ProtocolException("CONTINUATION Frame Received when not in a Continuation State");
                } else if (isConnectStream) {
                    ProtocolException pe = new ProtocolException("CONTINUATION frame received on a CONNECT stream");
                    pe.setConnectionError(false);
                    throw pe;
                }
                break;

            default:
                break;
        }

    }

    private void verifyWriteFrameSequence() throws ProtocolException {

        if (!currentFrame.isWriteFrame()) {
            throw new ProtocolException("Read frame was given for Write frame processing");
        }

        switch (frameType) {

            case DATA:
                // (spec) DATA frames are subject to flow control and can only be sent when a
                // stream is in the "open" or "half-closed (remote)" state.
                if (!(state == StreamState.OPEN || state == StreamState.HALF_CLOSED_REMOTE)) {
                    throw new ProtocolException("DATA Frame cannot be sent in this state: " + state);
                }

                break;

            case HEADERS:
                // (spec):  HEADERS frames can be sent on a stream in the "idle", "reserved (local)", "open", or
                // "half-closed (remote)" state.

                if (!(state == StreamState.IDLE || state == StreamState.OPEN || state == StreamState.HALF_CLOSED_REMOTE
                      || state == StreamState.RESERVED_LOCAL)) {
                    throw new ProtocolException("HEADERS Frame cannot be sent in this state: " + state);
                }

                break;

            case PRIORITY:
                // (spec) The PRIORITY frame can be sent on a stream in any state, though it
                // cannot be sent between consecutive frames that comprise a single header block
                if (muxLink.isWriteContinuationExpected()) {
                    // in a continuation state
                    throw new ProtocolException("PRIORITY Frame sent when in a Continuation State of: " + state);
                }

                break;

            case RST_STREAM:
                if (state == StreamState.IDLE) {
                    throw new ProtocolException("RST_STREAM Frame cannot be sent in this state: " + state);
                }
                break;

            case SETTINGS:
                break;

            case PUSH_PROMISE:
                // (spec) PUSH_PROMISE frames MUST only be sent on a peer-initiated stream that
                //  is in either the "open" or "half-closed (remote)" state.
                if (!(state == StreamState.OPEN || state == StreamState.HALF_CLOSED_REMOTE)) {
                    throw new ProtocolException("PUSH_PROMISE Frame cannot be sent in this state: " + state);
                }
                break;

            case WINDOW_UPDATE:
                // (spec) WINDOW_UPDATE can be sent in any state, even close or half-closed (remote)
                break;

            case CONTINUATION:
                if (!muxLink.isWriteContinuationExpected()) {
                    // not in a continuation state
                    throw new ProtocolException("CONTINUATION Frame sent when not in a Continuation State");
                }
                break;

            default:
                break;
        }

    }

    /**
     * Call when all header block fragments for a header block have been received
     */
    private void setHeadersComplete() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "completed headers have been received stream " + myID);
        }
        headersCompleted = true;
        muxLink.setContinuationExpected(false);
    }

    /**
     *
     */
    private void processHeadersPriority() {
        FrameHeaders castFrame = (FrameHeaders) currentFrame;
        if (castFrame.flagPrioritySet()) {
            int weight = castFrame.getWeight();
            int streamDependency = castFrame.getStreamDependency();
            boolean exclusive = castFrame.isExclusive();
            muxLink.getWorkQ().updateNodeFrameParameters(this.myID, weight, streamDependency, exclusive);
        }
    }

    /**
     * Appends the header block fragment in the current header frame to this stream's incomplete header block
     */
    private void getHeadersFromFrame() {
        byte[] hbf = null;
        if (currentFrame.getFrameType() == FrameTypes.HEADERS || currentFrame.getFrameType() == FrameTypes.PUSHPROMISEHEADERS) {
            hbf = ((FrameHeaders) currentFrame).getHeaderBlockFragment();
        } else if (currentFrame.getFrameType() == FrameTypes.CONTINUATION) {
            hbf = ((FrameContinuation) currentFrame).getHeaderBlockFragment();
        }

        if (hbf != null && hbf.length > 0) {
            if (headerBlock == null) {
                headerBlock = new ArrayList<byte[]>();
            }
            headerBlock.add(hbf);
        } else {
            emptyFrameReceivedCount++;
        }
    }

    /**
     * Puts stream's header block into the read buffer that will be passed to the webcontainer
     *
     * @throws CompressionException
     * @throws ProtocolException
     */
    private void processCompleteHeaders(boolean isPush) throws CompressionException, ProtocolException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processCompleteHeaders entry: stream " + myID);
        }
        if (this.headerBlock != null) {
            //Leave this to maintain current functionality, but passing a buffer
            //down to the channel is not necessary. H2 Headers will be stored in the H2
            //inbound link wrap.
            WsByteBufferPoolManager bufManager = HttpDispatcher.getBufferManager();
            WsByteBuffer buf = bufManager.allocate(getByteCount(headerBlock));

            int firstBlockLength = headerBlock.get(0).length;
            for (byte[] byteArray : headerBlock) {
                buf.put(byteArray);
            }
            buf.flip();
            headerBlock = null;

            //Get ready to decode headers
            boolean isFirstLineComplete = false;
            HashMap<String, String> pseudoHeaders = new HashMap<String, String>();
            ArrayList<H2HeaderField> headers = new ArrayList<H2HeaderField>();
            H2HeaderField current = null;
            boolean isFirstHeaderBlock;
            boolean isFirstHeader = true;
            boolean processTrailerHeaders = headersCompleted;

            //Decode headers until we reach the end of the buffer
            while (buf.hasRemaining()) {
                isFirstHeaderBlock = buf.position() < firstBlockLength;
                try {
                    current = (H2Headers.decodeHeader(buf, this.muxLink.getReadTable(), isFirstHeader && isFirstHeaderBlock,
                                                      processTrailerHeaders && !isPush, this.muxLink.getLocalConnectionSettings()));
                } catch (Http2Exception e) {
                    buf.release();
                    throw e;
                }
                if (current == null) {
                    // processed a dynamic table size update; go to the next header
                    continue;
                }
                isFirstHeader = false;

                if (!isFirstLineComplete) {
                    //Is this a Pseudo-Header?
                    if (current.getName().startsWith(":")) {
                        //Verify if this is a valid pseudo-header. In the case that this pseudo-header had
                        //already been decoded (two pseudo-headers with same name), throw a decompression
                        //exception and invalidate the table.
                        if (pseudoHeaders.get(current.getName()) != null) {
                            this.muxLink.getReadTable().setDynamicTableValidity(false);
                            buf.release();
                            ProtocolException pe = new ProtocolException("Invalid pseudo-header for decompression context: " + current.toString());
                            pe.setConnectionError(false); // mark this as a stream error so we'll generate an RST_STREAM
                            throw pe;
                        }
                        pseudoHeaders.put(current.getName(), current.getValue());
                    }
                    //If it doesn't start with colon, it is a regular header
                    //so mark this as the beginning of the parsing headers
                    //state and try to set the decoded first line.
                    else {
                        isFirstLineComplete = true;
                        if (H2Headers.getContentLengthValue(current) > -1) {
                            expectedContentLength = H2Headers.getContentLengthValue(current);
                        }
                        headers.add(current);
                    }
                } else {
                    //If header starts with ':' throw error
                    if (current.getName().startsWith(":")) {
                        this.muxLink.getReadTable().setDynamicTableValidity(false);
                        buf.release();
                        throw new CompressionException("Invalid pseudo-header decoded: all pseudo-headers must appear " +
                                                       "in the header block before regular header fields.");
                    }
                    if (H2Headers.getContentLengthValue(current) > -1) {
                        expectedContentLength = H2Headers.getContentLengthValue(current);
                    }

                    headers.add(current);
                }
            }
            buf.release();
            // only set headers on the link once
            if ((isPush || !processTrailerHeaders) && h2HttpInboundLinkWrap.getHeadersLength() == 0) {
                if (!isValidH2Request(pseudoHeaders)) {
                    muxLink.setContinuationExpected(false);
                    muxLink.setWriteContinuationExpected(false);
                    ProtocolException e = new ProtocolException("An invalid request was received on stream-id: " + myID);
                    e.setConnectionError(false); // mark this as a stream error so we'll generate an RST_STREAM
                    throw e;
                }
                if (pseudoHeaders.get(HpackConstants.AUTHORITY) != null) {
                    muxLink.setAuthority(pseudoHeaders.get(HpackConstants.AUTHORITY));
                }
                //Add all decoded pseudo-headers and headers
                this.h2HttpInboundLinkWrap.setReadHeaders(headers);
                this.h2HttpInboundLinkWrap.setReadPseudoHeaders(pseudoHeaders);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processCompleteHeaders no header block set: stream " + myID);
            }
        }
    }

    /**
     * Check to see if the passed headers contain values for :method, :scheme, and :path
     * If the CONNECT method header was found, :path and :scheme are not allowed, and :authority is required
     *
     * @param HashMap<String, String> headers
     * @return true if :method, :scheme, and :path are found
     */
    private boolean isValidH2Request(HashMap<String, String> pseudoHeaders) {
        if (MethodValues.CONNECT.getName().equals(pseudoHeaders.get(HpackConstants.METHOD))) {
            if (pseudoHeaders.get(HpackConstants.PATH) == null && pseudoHeaders.get(HpackConstants.SCHEME) == null
                && pseudoHeaders.get(HpackConstants.AUTHORITY) != null) {
                this.isConnectStream = true;
                return true;
            }
            return false;
        }
        if (pseudoHeaders.get(HpackConstants.METHOD) != null && pseudoHeaders.get(HpackConstants.PATH) != null &&
            pseudoHeaders.get(HpackConstants.SCHEME) != null) {
            return true;
        }
        return false;
    }

    /**
     * Grab the data from the current frame
     */
    private void getBodyFromFrame() {
        if (dataPayload == null) {
            dataPayload = new ArrayList<byte[]>();
        }
        if (currentFrame.getFrameType() == FrameTypes.DATA) {
            if (currentFrame.getPayloadLength() == 0) {
                emptyFrameReceivedCount++;
            } else {
                dataPayload.add(((FrameData) currentFrame).getData());
            }
        }
    }

    /**
     * Put the data payload for this stream into the read buffer that will be passed to the webcontainer.
     * This should only be called when this stream has received an end of stream flag.
     *
     * @param boolean store controls if buffer contents are to be saved on this stream
     * @return WsByteBuffer buffer taht was created and holds the payload
     * @throws ProtocolException
     */
    private WsByteBuffer processCompleteData(boolean store) throws ProtocolException {
        WsByteBufferPoolManager bufManager = HttpDispatcher.getBufferManager();
        WsByteBuffer buf = bufManager.allocate(getByteCount(dataPayload));
        for (byte[] bytes : dataPayload) {
            buf.put(bytes);
        }
        buf.flip();
        int actualContentLength = buf.remaining();
        if (expectedContentLength != -1 && actualContentLength != expectedContentLength) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processCompleteData release buffer and throw ProtocolException");
            }
            buf.release();
            ProtocolException pe = new ProtocolException("content-length header did not match the expected amount of data received");
            pe.setConnectionError(false); // stream error
            throw pe;
        } else if (expectedContentLength == -1 && actualContentLength > 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processCompleteData no content-length header was sent for stream-id: " + streamId() + " but it received "
                             + actualContentLength + " bytes of of body data");
            }
        }
        this.h2HttpInboundLinkWrap.setH2ContentLength(actualContentLength);

        // overwrite the local read buffer (if this is the first data frame on this stream)
        if (store) {
            moveDataIntoReadBufferArray(buf);
        }

        return buf;
    }

    /**
     * Tell the HTTP inbound link that we have data ready for it to read
     *
     * @throws ProtocolException
     */
    private void setReadyForRead() throws ProtocolException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setReadyForRead entry: stream id:" + myID);
        }
        muxLink.updateGoawayPromisedStreamId(myID);
        if (headersCompleted) {
            ExecutorService executorService = CHFWBundle.getExecutorService();
            Http2Ready readyThread = new Http2Ready(h2HttpInboundLinkWrap);
            executorService.execute(readyThread);
        }
    }

    private boolean waitingForWebContainer = false;

    protected boolean isWaitingForWC() {
        return waitingForWebContainer;
    }

    private class Http2Ready implements Runnable {

        private H2HttpInboundLinkWrap h2HttpInboundLinkWrap = null;

        public Http2Ready(H2HttpInboundLinkWrap x) {
            this.h2HttpInboundLinkWrap = x;
        }

        @Override
        public void run() {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "new thread calling h2HttpInboundLinkWrap.ready(...): stream id:" + myID);
            }
            try {
                waitingForWebContainer = true;
                boolean closing = muxLink.checkIfGoAwaySendingOrClosing();
                if (!closing) {
                    h2HttpInboundLinkWrap.ready(this.h2HttpInboundLinkWrap.vc);
                }
            } finally {
                if (getEndStream()) {
                    headersCompleted = false;
                }
                waitingForWebContainer = false;
            }
        }
    }

    /**
     * Add a buffer to the list of buffers that will be sent to the WebContainer when a read is requested
     *
     * @param newReadBuffer
     */
    private void moveDataIntoReadBufferArray(WsByteBuffer newReadBuffer) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moveDataIntoReadBufferArray entry: stream " + myID + " buffer: " + newReadBuffer);
        }

        // move the data that is to be sent up the channel into the currentReadReady byte array, and update the currentReadSize
        if (newReadBuffer != null) {
            int size = newReadBuffer.remaining(); // limit - pos
            if (size > 0) {
                streamReadReady.add(newReadBuffer);
                streamReadSize += size;
                this.readLatch.countDown();
            }
        }
    }

    /**
     * Read the HTTP header and data bytes for this stream
     *
     * @param numBytes the number of bytes to read
     * @param requestBuffers an array of buffers to copy the read data into
     * @return this stream's VirtualConnection or null if too many bytes were requested
     */
    @SuppressWarnings("unchecked")
    public VirtualConnection read(long numBytes, WsByteBuffer[] requestBuffers) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "read entry: stream " + myID + " request: " + requestBuffers + " num bytes requested: " + numBytes);
        }

        long streamByteCount = streamReadSize; // number of bytes available on this stream
        long requestByteCount = bytesRemaining(requestBuffers); // total capacity of the caller byte array
        int reqArrayIndex = 0; // keep track of where we are in the caller's array

        // if at least numBytes are ready to be processed as part of the HTTP Request/Response, then load it up
        if (numBytes > streamReadSize || requestBuffers == null) {
            if (this.headersCompleted) {
                return h2HttpInboundLinkWrap.getVirtualConnection();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "read exit: stream " + myID + " more bytes requested than available for read: " + numBytes + " > " + streamReadSize);
                }
                return null;
            }
        }

        if (streamByteCount < requestByteCount) {
            // the byte count requested exceeds the caller array capacity
            actualReadCount = streamByteCount;
        } else {
            actualReadCount = requestByteCount;
        }

        // copy bytes from this stream into the caller arrays
        for (int bytesRead = 0; bytesRead < actualReadCount; bytesRead++) {

            // find the first buffer from the caller that has remaining capacity
            while ((requestBuffers[reqArrayIndex].position() == requestBuffers[reqArrayIndex].limit())) {
                reqArrayIndex++;
            }

            // find the next stream buffer that has data
            while (!streamReadReady.isEmpty() && !streamReadReady.peek().hasRemaining()) {
                streamReadReady.poll().release();
            }
            requestBuffers[reqArrayIndex].put(streamReadReady.peek().get());
        }
        streamReadSize -= actualReadCount;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "read exit: " + streamId());
        }
        // return the vc since this was a successful read
        return h2HttpInboundLinkWrap.getVirtualConnection();
    }

    public void countDownFirstReadLatch() {
        if (firstReadLatch != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "counting down firstReadLatch: " + firstReadLatch.hashCode());
            }
            firstReadLatch.countDown();
        }
    }

    /**
     * Read the http header and data bytes for this stream
     *
     * @param numBytes the number of bytes requested
     * @param requestBuffers an array of buffers to copy the read data into
     * @return the number of bytes that were actually copied into requestBuffers
     */
    public long readCount(long numBytes, WsByteBuffer[] requestBuffers) {

        if (read(numBytes, requestBuffers) != null) {
            return actualReadCount;
        }
        return 0;
    }

    private long bytesRemaining(@Sensitive WsByteBuffer[] bufs) {
        // return with the number of bytes remaining for all buffers combined
        long count = 0;
        int length = bufs.length;
        for (int i = 0; i < length; i++) {
            if (bufs[i] == null) {
                return count;
            }
            count += bufs[i].remaining();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "bytesRemaining: stream " + myID + " has " + count + " bytes remaining");
        }

        return count;
    }

    /**
     * Write out the frame that's currently set on this stream; first, check to make sure that
     * we're actually writing out a write frame, as we expect.
     *
     * @return true if a write request was successfully passed on to the underlying link
     */
    private boolean writeFrameSync() throws FlowControlException, Http2Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeFrameSync entry: stream: " + myID);
        }
        Frame currentFrame = this.currentFrame;

        if (!currentFrame.getFrameType().equals(FrameTypes.GOAWAY) && isStreamClosed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeFrameSync exit - stream " + myID + " is closed");
            }
            return false;
        }
        if (currentFrame.isWriteFrame() && currentFrame.getInitialized()) {
            WsByteBuffer writeFrameBuffer = null;
            WsByteBuffer[] writeFrameBuffers = null;
            try {
                if (currentFrame.getFrameType() == FrameTypes.DATA) {
                    FrameData data = (FrameData) currentFrame;
                    boolean timedOut = false;

                    // Check to see if the write window is large enough to write this data.
                    if (isWindowLimitExceeded((FrameData) currentFrame)) {
                        // the connection or stream window is too small to write this data frame.  This thread will wait for a max of
                        // 5 seconds for a window update that's large enough to allow the data frame to be written out
                        long startTime = System.currentTimeMillis();
                        while (isWindowLimitExceeded((FrameData) currentFrame) && !timedOut) {
                            synchronized (this) {
                                this.wait(MAX_TIME_TO_WAIT_FOR_WINDOW_UPDATE_MS);
                            }
                            if (state.equals(StreamState.CLOSED) || muxLink.checkIfGoAwaySendingOrClosing()) {
                                return false;
                            } else if (System.currentTimeMillis() - startTime > MAX_TIME_TO_WAIT_FOR_WINDOW_UPDATE_MS) {
                                timedOut = true;
                            }
                        }
                    }
                    // the flow control window is large enough to write the data frame
                    if (!timedOut) {
                        writeFrameBuffers = data.buildFrameArrayForWrite();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "stream: " + myID + " write with default (60 second) timeout.");
                        }
                        muxLink.writeSync(null, writeFrameBuffers, data.getWriteFrameLength(), TCPRequestContext.USE_CHANNEL_TIMEOUT,
                                          data.getFrameType(), data.getPayloadLength(), myID);

                        streamWindowUpdateWriteLimit -= currentFrame.getPayloadLength();
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "stream: " + myID + " Data payload written - new streamWindowUpdateWriteLimit: " + streamWindowUpdateWriteLimit);
                        }

                    } else {
                        // timed out waiting for a window update, throw FCE which will cause this stream to be RESET.
                        FlowControlException up = new FlowControlException("Write failed. Window limit exceeded. Stream will be Reset.");
                        up.setConnectionError(false);
                        throw up;
                    }

                } else {
                    // this frame is not a data frame, and so it's not subject to flow control and we can write immediately
                    writeFrameBuffer = currentFrame.buildFrameForWrite();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "stream: " + myID + " write with default (60 second) timeout");
                    }
                    muxLink.writeSync(writeFrameBuffer, null, currentFrame.getWriteFrameLength(), TCPRequestContext.USE_CHANNEL_TIMEOUT,
                                      currentFrame.getFrameType(), currentFrame.getPayloadLength(), myID);
                }
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeFrameSync caught an IOException: " + e);
                }

                Http2Exception up = new Http2Exception(e.getMessage());
                up.setConnectionError(true);
                throw up;

            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeFrameSync interrupted: " + e);
                }
            } finally {
                // release buffer used to synchronously write the frame
                if (writeFrameBuffer != null) {
                    writeFrameBuffer.release();
                } else if (writeFrameBuffers != null) {
                    for (int i = 0; i < writeFrameBuffers.length; i++) {
                        if (writeFrameBuffers[i] != null) {
                            // buffer at [1] is allocated by old channel code, it will clean it up
                            // later move this logic to a frame cleanup method that can take care of releasing
                            if (i != 1) {
                                writeFrameBuffers[i].release();
                            }
                        }
                    }
                }
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeFrameSync internal flow issue - exiting method ");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeFrameSync exit: stream-id: " + myID);
        }
        return true;
    }

    /**
     * Check if the current stream should be closed
     *
     * @return true if the stream should stop processing
     */
    public boolean isStreamClosed() {
        if (this.myID == 0 && !endStream) {
            return false;
        }

        if (state == StreamState.CLOSED) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isStreamClosed stream closed; " + streamId());
            }
            return true;
        }

        boolean rc = muxLink.checkIfGoAwaySendingOrClosing();

        if (rc == true) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isStreamClosed stream closed via muxLink check; " + streamId());
            }
        }

        return rc;
    }

    /**
     * Return true if the current stream is half closed
     */
    public boolean isHalfClosed() {
        return (state == StreamState.HALF_CLOSED_LOCAL || state == StreamState.HALF_CLOSED_REMOTE);
    }

    protected void setCloseTime(long x) {
        closeTime = x;
    }

    protected long getCloseTime() {
        return closeTime;
    }

    /**
     * Wait on this thread/stream until the H2 connection has completed initializing
     *
     * @return true if this connection initialized correctly
     */
    public boolean waitForConnectionInit() {
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "waitForConnectionInit: waiting for the H2 connection to complete initialization on " + streamId());
            }
            // the connection isn't initialized yet; wait on the init lock
            boolean rc = muxLink.initLock.await(Constants.H2C_UPGRADE_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (rc) {
                // We can get here if the link init was successful or if there was a link init error
                // If there was an error, the link will be shutting down
                // In this case, we just want to issue a message and exit, letting the other thread complete
                // the close
                if (!muxLink.checkIfGoAwaySendingOrClosing()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "waitForConnectionInit: stop waiting, H2 connection initialized " + streamId());
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "waitForConnectionInit: stop waiting, h2 initialization error ");
                    }
                    rc = false;
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "waitForConnectionInit: stop waiting, timed out waiting for client ");
                }
                muxLink.close(muxLink.initialVC, new ProtocolException("http/2 protocol initialization failed"));
            }
            return rc;
        } catch (InterruptedException e) {
            // server error handled in caller
            return false;
        }
    }

    protected CountDownLatch getReadLatch() {
        return this.readLatch;
    }

    private String streamId() {
        return "stream-id: " + myID;
    }

    /**
     * Get the number of bytes in this list of byte arrays
     *
     * @param listOfByteArrays
     * @return the total byte count for this list of byte arrays
     */
    private int getByteCount(ArrayList<byte[]> listOfByteArrays) {
        int count = 0;
        for (byte[] byteArray : listOfByteArrays) {
            if (byteArray != null) {
                count += byteArray.length;
            }
        }
        return count;
    }

    public int getId() {
        return myID;
    }

    public H2HttpInboundLinkWrap getWrappedInboundLink() {
        return h2HttpInboundLinkWrap;
    }

    /**
     * @param frame
     * @return true if frame is a control frame
     */
    public static boolean isControlFrame(Frame frame) {
        switch (frame.getFrameType()) {
            case PRIORITY:
                return true;
            case RST_STREAM:
                return true;
            case SETTINGS:
                return true;
            case PING:
                return true;
            case GOAWAY:
                return true;
            default:
                return false;
        }
    }

    public boolean getEndStream() {
        return this.endStream;
    }

}
