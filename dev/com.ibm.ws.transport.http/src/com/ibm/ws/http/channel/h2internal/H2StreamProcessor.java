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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
import com.ibm.ws.http.channel.h2internal.exceptions.StreamClosedException;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.FrameContinuation;
import com.ibm.ws.http.channel.h2internal.frames.FrameData;
import com.ibm.ws.http.channel.h2internal.frames.FrameGoAway;
import com.ibm.ws.http.channel.h2internal.frames.FrameHeaders;
import com.ibm.ws.http.channel.h2internal.frames.FramePing;
import com.ibm.ws.http.channel.h2internal.frames.FramePriority;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.h2internal.frames.FrameSettings;
import com.ibm.ws.http.channel.h2internal.frames.FrameWindowUpdate;
import com.ibm.ws.http.channel.h2internal.frames.utils;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * Represents an independent HTTP/2 stream
 * This class is intentionally not thread safe, it is designed with the idea that per stream only one frame will be processed at one time
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
    private boolean continuationExpected = false;
    private boolean trailerExpected = false;
    private List<String> trailerHeaderNames;

    // the anticipated content length, as passed in from a content-length header
    private int expectedContentLength = -1;

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

    // the local window, which we're keeping track of as a receiver
    private long streamReadWindowSize = Constants.SPEC_INITIAL_WINDOW_SIZE;

    // flag used to signal that we don't want to queue any data frames, as the flow control window has not been exceeded
    private boolean waitingForWindowUpdate = false;
    // keep track of any DATA frames that cannot be sent until the client updates the window
    Queue<FrameData> dataWaitingForWindowUpdate;

    // a list of buffers to be read in by the WebContainer
    private final ArrayList<WsByteBuffer> streamReadReady = new ArrayList<WsByteBuffer>();
    private int streamReadSize = 0;
    private long actualReadCount = 0;
    private CountDownLatch readLatch = new CountDownLatch(1);

    /**
     * Constructor
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
     */
    protected void completeConnectionPreface() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "completeConnectionPreface entry: about to send SETTINGS frame to ACK receipt of MAGIC");
        }

        // send out a settings frame with any HTTP2 settings that the user may have changed
        if (Constants.SPEC_INITIAL_WINDOW_SIZE != this.streamReadWindowSize) {
            currentFrame = new FrameSettings(0, -1, -1, -1, (int) this.streamReadWindowSize, -1, -1, false);
        } else {
            currentFrame = new FrameSettings();
        }
        this.frameType = FrameTypes.SETTINGS;
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "completeConnectionPreface processNextFrame-:  stream: " + myID + " frame type: " + currentFrame.getFrameType().toString() + " direction: "
                             + Direction.WRITING_OUT
                             + " H2InboundLink hc: " + muxLink.hashCode());
            }

            this.writeFrameSync();

        } catch (FlowControlException e) {
            // FlowControlException can only occur writing DATA frames
        }
        if (Constants.SPEC_INITIAL_WINDOW_SIZE != muxLink.maxReadWindowSize) {
            // the user has changed the max connection read window, so we'll update that now
            currentFrame = new FrameWindowUpdate(0, (int) muxLink.maxReadWindowSize, false);
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "completeConnectionPreface processNextFrame-:  stream: " + myID + " frame type: " + currentFrame.getFrameType().toString() + " direction: "
                                 + Direction.WRITING_OUT
                                 + " H2InboundLink hc: " + muxLink.hashCode());
                }

                this.writeFrameSync();

            } catch (FlowControlException e) {
                // FlowControlException can only occur writing DATA frames
            }
        }
    }

    public synchronized void processNextFrame(Frame frame, Constants.Direction direction) throws ProtocolException {

        // Make it easy to follow frame processing in the trace by searching for "processNextFrame-" to see all fraame processing
        boolean doDebugWhile = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processNextFrame-entry:  stream: " + myID + " frame type: " + frame.getFrameType().toString() + " direction: " + direction.toString()
                         + " H2InboundLink hc: " + muxLink.hashCode());
        }
        if (isStreamClosed()) {
            // Handle Read or Write while the stream is closed.

            if (direction.equals(Constants.Direction.WRITING_OUT)) {
                // writing while closed. check to see if it's past the significant time, and
                // if it is past time, throw an exception, which should generate a GOAWAY
                if (muxLink.significantlyPastCloseTime(myID)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processNextFrame: Stream significantly past close time - throwing ProtocolException");
                    }
                    throw new ProtocolException("Stream significantly past close time");
                } else {
                    // closed, but not past time.
                    // If frame type PRIORITY, WINDOW_UPDATE, or RST_STREAM ignore
                    // if HEADERS, DATA, PUSH_PROMISE throw error
                    if (frame.getFrameType() == FrameTypes.PRIORITY || frame.getFrameType() == FrameTypes.WINDOW_UPDATE
                        || frame.getFrameType() == FrameTypes.RST_STREAM) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "processNextFrame: close, not past time, ignoring frame");
                        }
                        return;
                    } else if (frame.getFrameType() == FrameTypes.HEADERS || frame.getFrameType() == FrameTypes.DATA || frame.getFrameType() == FrameTypes.PUSH_PROMISE) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "processNextFrame: Stream is closed - throwing ProtocolException");
                        }
                        throw new ProtocolException("Stream is closed, can't write out HEADER, DATA, or PUSH_PROMISE frames");
                    }
                }
            } else if (direction.equals(Constants.Direction.READ_IN)) {
                // reading while closed

                if (frame.getFrameType() == FrameTypes.PRIORITY) {
                    // Ignore PRIORITY in all closed situations
                    return;
                }

                if (muxLink.significantlyPastCloseTime(myID)) {
                    // too old of stream, so throw a Protocol Exception
                    throw new ProtocolException("Stream is already closed");
                }

                // RST_STREAM throw PROTOCOL_ERROR
                // HEADERS or DATA received and in half closed remote throw PROTOCOL_ERROR
                // process WINDOW_UPDATE as normal

                //if (frame.getFrameType() == FrameTypes.RST_STREAM) {
                //    throw new ProtocolException("Stream is already closed");
                //}

                if (frame.getFrameType() == FrameTypes.DATA || frame.getFrameType() == FrameTypes.HEADERS) {
                    throw new ProtocolException("DATA or HEADERS frame received on a closed stream");
                } else if (frame.getFrameType() == FrameTypes.RST_STREAM) {
                    return;
                }
            }
        }

        ADDITIONAL_FRAME addFrame = ADDITIONAL_FRAME.FIRST_TIME;
        Http2Exception addFrameException = null;
        currentFrame = frame;

        while (addFrame != ADDITIONAL_FRAME.NO) {

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
                currentFrame = new FrameRstStream(myID, addFrameException.getErrorCode(), false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: exception encountered.  Sending RST_STREAM on stream "
                                 + myID + " with the error code " + addFrameException.getErrorString());
                }
                direction = Constants.Direction.WRITING_OUT;
            }

            // if looping to GOAWAY, then load it up now
            if (addFrame == ADDITIONAL_FRAME.GOAWAY) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "processNextFrame: addFrame GOAWAY: HighestClientStreamId: " + muxLink.getHighestClientStreamId());
                }
                currentFrame = new FrameGoAway(0, addFrameException.getMessage().getBytes(), addFrameException.getErrorCode(), muxLink.getHighestClientStreamId(), false);
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
                currentFrame.validate(muxLink.getConnectionSettings());
            } catch (Http2Exception e) {
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
                        // TODO: otherwise just close the stream/connection at the TCP Channel layer and clean up resources
                    }
                    addFrame = ADDITIONAL_FRAME.NO;
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

                // Header frames must be received in a contiguous chunk; cannot interleave across streams
                if (isContinuationFrameExpected() && (frameType != FrameTypes.CONTINUATION || !this.continuationExpected)) {
                    addFrame = ADDITIONAL_FRAME.GOAWAY;
                    addFrameException = new ProtocolException("Did not receive the expected continuation frame");

                    continue;
                }

                if (frameType == FrameTypes.SETTINGS ||
                    frameType == FrameTypes.GOAWAY || frameType == FrameTypes.PING) {

                    switch (frameType) {
                        case SETTINGS:
                            processSETTINGSFrame();
                            break;

                        case GOAWAY:
                            processGOAWAYFrame();
                            updateStreamState(StreamState.CLOSED);
                            break;

                        case PING:
                            processPINGFrame();
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
                    return;
                }

                try {
                    if (frameType == FrameTypes.WINDOW_UPDATE) {
                        processWindowUpdateFrame();
                        // check to see if there is data waiting for this update
                        if (dataWaitingForWindowUpdate != null) {
                            if (!this.isWindowLimitExceeded(dataWaitingForWindowUpdate.peek())) {
                                // there is data waiting; signal that we need to stop queuing data
                                waitingForWindowUpdate = false;
                            }
                        } else {
                            return;
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
                    readWriteTransitionState(direction);
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
            }
            // check to see if there's a queued data frame that should be written out
            if (!waitingForWindowUpdate && dataWaitingForWindowUpdate != null &&
                !this.isWindowLimitExceeded(this.dataWaitingForWindowUpdate.peek())) {
                addFrame = ADDITIONAL_FRAME.DATA;
                currentFrame = this.dataWaitingForWindowUpdate.remove();
                direction = Constants.Direction.WRITING_OUT;
                if (this.dataWaitingForWindowUpdate.isEmpty()) {
                    this.dataWaitingForWindowUpdate = null;
                }

            } else {
                addFrame = ADDITIONAL_FRAME.NO;
            }
        }
    }

    private void readWriteTransitionState(Constants.Direction direction) throws Http2Exception {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "readWriteTransitionState: entry: frame type: " + currentFrame.getFrameType() + " state: " + state);
        }

        if (currentFrame.getFrameType() == FrameTypes.GOAWAY
            || currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
            writeFrameSync();
            this.updateStreamState(StreamState.CLOSED);

            if (currentFrame.getFrameType() == FrameTypes.GOAWAY) {
                muxLink.closeConnectionLink(null);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "readWriteTransitionState: return: state: " + state);
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
     */
    private void processSETTINGSFrame() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processSETTINGSFrame entry");
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
            muxLink.getConnectionSettings().updateSettings((FrameSettings) currentFrame);

            // immediately send out ACK (an empty SETTINGS frame with the ACK flag set)
            currentFrame = new FrameSettings();
            currentFrame.setAckFlag();

            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "completeConnectionPreface processNextFrame-:  stream: " + myID + " frame type: " + currentFrame.getFrameType().toString() + " direction: "
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

        if (castFrame.getStreamId() == 0) {
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
     * If this stream is receiving a DATA frame, the local read window needs to be updated. If the read window drops below a threshold,
     * a WINDOW_UPDATE frame will be sent for both the connection and stream to update the windows.
     */
    private void updateStreamReadWindow() throws FlowControlException {
        if (currentFrame instanceof FrameData) {
            long frameSize = currentFrame.getPayloadLength();
            streamReadWindowSize -= frameSize; // decrement stream read window
            muxLink.connectionReadWindowSize -= frameSize; // decrement connection read window

            // if the stream or connection windows become too small, update the windows
            // TODO: decide how often we should update the read window via WINDOW_UPDATE
            if (streamReadWindowSize < (muxLink.maxReadWindowSize / 2) ||
                muxLink.connectionReadWindowSize < (muxLink.maxReadWindowSize / 2)) {

                int windowChange = (int) (muxLink.maxReadWindowSize - this.streamReadWindowSize);
                Frame savedFrame = currentFrame; // save off the current frame
                currentFrame = new FrameWindowUpdate(myID, windowChange, false);
                writeFrameSync();
                long windowSizeIncrement = muxLink.maxReadWindowSize - muxLink.connectionReadWindowSize;
                currentFrame = new FrameWindowUpdate(0, (int) windowSizeIncrement, false);
                writeFrameSync();
                currentFrame = savedFrame;
            }
        }
    }

    protected void updateInitialWindowsUpdateSize(int newSize) {
        // this method should only be called by the thread that came in on processNewFrame.
        // newSize should be treated as an unsigned 32-bit int

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateInitialWindowsUpdateSize entry: newSize: " + newSize);
        }

        long diff = newSize - streamWindowUpdateWriteInitialSize;

        streamWindowUpdateWriteInitialSize = newSize;
        streamWindowUpdateWriteLimit += diff;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "streamWindowUpdateWriteInitialSize updated to: " + streamWindowUpdateWriteInitialSize);
            Tr.debug(tc, "streamWindowUpdateWriteLimit updated to: " + streamWindowUpdateWriteLimit);
        }

    }

    public void sendGOAWAYFrame(Http2Exception e) throws ProtocolException {

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
            Tr.debug(tc, "sendGOAWAYFrame sending a GOAWAY with Last-Stream-ID " + muxLink.getHighestClientStreamId()
                         + " and exception " + e.toString());
        }

        // send out a GoAway in response;
        Frame frame = new FrameGoAway(0, e.getMessage().getBytes(), e.getErrorCode(), muxLink.getHighestClientStreamId(), false);
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

        muxLink.triggerStreamClose(this);

        // send out a goaway in response; return the same last stream, for now
        currentFrame = new FrameGoAway(0, new byte[0], 0, muxLink.getHighestClientStreamId(), false);

        try {
            writeFrameSync();
        } catch (FlowControlException e) {
            // FlowControlException cannot occur for FrameTypes.GOAWAY, so do nothing here but debug
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeSync caught (logically unexpected) FlowControlException: " + e);
            }
        } finally {

            muxLink.closeConnectionLink(null);
        }
    }

    private void processPINGFrame() {
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
        muxLink.triggerStreamClose(this);
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
    private void processIdle(Constants.Direction direction) throws CompressionException, FlowControlException, ProtocolException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processIdle entry: stream " + myID);
        }
        if (currentFrame.getFrameType() == FrameTypes.HEADERS ||
            currentFrame.getFrameType() == FrameTypes.PUSH_PROMISE) {
            muxLink.updateHighestStreamId(myID);
        }
        // Can only receive HEADERS or PRIORITY frame in Idle state
        if (direction == Constants.Direction.READ_IN) {
            if (frameType == FrameTypes.HEADERS) {

                processHeadersPriority();
                getHeadersFromFrame();

                if (currentFrame.flagEndHeadersSet()) {
                    processCompleteHeaders();
                    setHeadersComplete();
                    setReadyForRead();
                } else {
                    setContinuationFrameExpected(true);
                }

                if (currentFrame.flagEndStreamSet()) {
                    endStream = true;
                    updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                } else {
                    updateStreamState(StreamState.OPEN);
                }
            }
        } else {
            // send out a HEADER frame and update the stream state to OPEN
            if (frameType == FrameTypes.HEADERS) {
                updateStreamState(StreamState.OPEN);
                if (!currentFrame.flagEndHeadersSet()) {
                    setContinuationFrameExpected(true);
                }
            }
            writeFrameSync();
        }
    }

    private void processOpen(Constants.Direction direction) throws ProtocolException, FlowControlException, CompressionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processOpen entry: stream " + myID);
        }

        if (direction == Constants.Direction.READ_IN) {
            if (frameType == FrameTypes.DATA) {
                getBodyFromFrame();
                if (currentFrame.flagEndStreamSet()) {
                    processCompleteData();
                    setReadyForRead();
                }
            } else if (frameType == FrameTypes.CONTINUATION ||
                       (frameType == FrameTypes.HEADERS)) {
                getHeadersFromFrame();
                if (currentFrame.flagEndHeadersSet()) {
                    processCompleteHeaders();
                    setHeadersComplete();
                    setReadyForRead();
                    if (currentFrame.flagEndStreamSet()) {
                        setReadyForRead();
                    }
                } else {
                    setContinuationFrameExpected(true);
                }
            }
            if (currentFrame.flagEndStreamSet()) {
                endStream = true;
                updateStreamState(StreamState.HALF_CLOSED_REMOTE);
            }

        } else {
            // write out the current frame and update the stream state
            if (frameType == FrameTypes.PUSH_PROMISE) {
                // writing out a PP doesn't have any effect on the current stream, but rather the promised stream

            } else if (frameType == FrameTypes.HEADERS || frameType == FrameTypes.CONTINUATION) {
                if (currentFrame.flagEndHeadersSet()) {
                    setContinuationFrameExpected(false);
                    if (currentFrame.flagEndStreamSet()) {
                        endStream = true;
                        updateStreamState(StreamState.HALF_CLOSED_LOCAL);
                    }
                } else {
                    setContinuationFrameExpected(true);
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
    private void processHalfClosedLocal(Constants.Direction direction) throws FlowControlException {
        // A stream transitions from this state to "closed" when a frame that
        // contains an END_STREAM flag is received or when either peer sends
        // a RST_STREAM frame.
        if (direction == Direction.WRITING_OUT) {
            writeFrameSync();
            if (currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
                endStream = true;
                updateStreamState(StreamState.CLOSED);
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
     */
    private void processHalfClosedRemote(Constants.Direction direction) throws FlowControlException, CompressionException {
        // A stream can transition from this state to "closed" by sending a
        // frame that contains an END_STREAM flag or when either peer sends a
        // RST_STREAM frame.
        if (direction == Direction.WRITING_OUT) {
            boolean writeCompleted = writeFrameSync();
            if ((currentFrame.getFrameType() == FrameTypes.RST_STREAM || currentFrame.flagEndStreamSet())
                && writeCompleted) {
                endStream = true;
                muxLink.triggerStreamClose(this);
                updateStreamState(StreamState.CLOSED);

            } else if (frameType == FrameTypes.HEADERS || frameType == FrameTypes.CONTINUATION) {
                if (currentFrame.flagEndHeadersSet()) {
                    setContinuationFrameExpected(false);
                } else {
                    setContinuationFrameExpected(true);
                }
            }
        } else if (currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
            endStream = true;
            updateStreamState(StreamState.CLOSED);
        } else if (frameType == FrameTypes.CONTINUATION) {
            getHeadersFromFrame();
            if (currentFrame.flagEndHeadersSet()) {
                processCompleteHeaders();
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
    private void processReservedLocal(Constants.Direction direction) throws FlowControlException {
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
    private boolean isWindowLimitExceeded(Frame frame) {
        if (streamWindowUpdateWriteLimit - frame.getPayloadLength() < 0 ||
            muxLink.getWorkQ().getConnectionWriteLimit() - frame.getPayloadLength() < 0) {
            // would exceed window update limit
            String s = "Cannot write Data Frame because it would exceed the stream window update limit."
                       + "streamWindowUpdateWriteLimit: " + streamWindowUpdateWriteLimit
                       + " streamWindowUpdateWriteInitialSize: " + streamWindowUpdateWriteInitialSize
                       + " frame size: " + frame.getPayloadLength();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, s);
            }
            return true;
        }
        return false;
    }

    /*
     * Send an artificially created H2 request from a push_promise up to the WebContainer
     */
    public void sendRequestToWc(FrameHeaders frame) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "sendRequestToWc");
        }

        if (null == frame) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "sendRequestToWc: frame is null");
            }
        } else {
            // Make the frame look like it just came in
            WsByteBufferPoolManager bufManager = HttpDispatcher.getBufferManager();
            WsByteBuffer buf = bufManager.allocate(frame.buildFrameForWrite().length);
            byte[] ba = frame.buildFrameForWrite();
            buf.put(ba);
            buf.flip();
            TCPReadRequestContext readi = h2HttpInboundLinkWrap.getConnectionContext().getReadInterface();
            readi.setBuffer(buf);
            currentFrame = frame;
            this.getHeadersFromFrame();
            setHeadersComplete();
            try {
                processCompleteHeaders();
            } catch (CompressionException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "sendRequestToWc: compression exception when creating the pushed reqeust on stream-id " + myID);
                }
                // Free the buffer, set the current frame to null, and remove the SP from the table
                buf.release();
                this.currentFrame = null;
                h2HttpInboundLinkWrap.muxLink.streamTable.remove(this);
                return;
            }

            // Start a new thread to pass along this frame to wc
            setReadyForRead();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "sendRequestToWc");
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

                if (!(state == StreamState.OPEN || state == StreamState.HALF_CLOSED_LOCAL)) {
                    throw new StreamClosedException("DATA Frame Received in the wrong state of: " + state);
                }
                break;

            case HEADERS:
                // (non-spec) HEADERS frame is only read by the server side if the connection was in idle state,
                // meaning the client/remote side is starting a new stream and sending a request on it, or if the
                // stream was in open state, meaning that trailer headers were sent
                if (state != StreamState.IDLE && state != StreamState.OPEN) {
                    throw new ProtocolException("HEADERS Frame Received in the wrong state of: " + state);
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
                if (!isContinuationFrameExpected()) {
                    throw new ProtocolException("CONTINUATION Frame Received when not in a Continuation State");
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
                if (isContinuationFrameExpected()) {
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
                if (!isContinuationFrameExpected()) {
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
        if (!trailerExpected) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "completed headers have been received stream " + myID);
            }
            headersCompleted = true;
            setContinuationFrameExpected(false);
        }
    }

    public boolean isContinuationFrameExpected() {
        return muxLink.isContinuationExpected();
    }

    public void setContinuationFrameExpected(boolean expected) {
        muxLink.setContinuationExpected(expected);
        this.continuationExpected = expected;
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
        if (currentFrame.getFrameType() == FrameTypes.HEADERS) {
            hbf = ((FrameHeaders) currentFrame).getHeaderBlockFragment();
        } else if (currentFrame.getFrameType() == FrameTypes.CONTINUATION) {
            hbf = ((FrameContinuation) currentFrame).getHeaderBlockFragment();
        }

        if (hbf != null) {
            if (headerBlock == null) {
                headerBlock = new ArrayList<byte[]>();
            }
            headerBlock.add(hbf);
        }
    }

    /**
     * Puts stream's header block into the read buffer that will be passed to the webcontainer
     *
     * @throws CompressionException
     * @throws ProtocolException
     */
    private void processCompleteHeaders() throws CompressionException {
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
            boolean processTrailerHeaders = trailerExpected;

            //Decode headers until we reach the end of the buffer
            while (buf.hasRemaining()) {
                isFirstHeaderBlock = buf.position() < firstBlockLength;
                current = (H2Headers.decodeHeader(buf, this.muxLink.getReadTable(), isFirstHeader && isFirstHeaderBlock,
                                                  processTrailerHeaders, this.muxLink.getConnectionSettings()));
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
                            throw new CompressionException("Invalid pseudo-header for decompression context: " + current.toString());
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
                        throw new CompressionException("Invalid pseudo-header decoded: all pseudo-headers must appear " +
                                                       "in the header block before regular header fields.");
                    }
                    if (H2Headers.getContentLengthValue(current) > -1) {
                        expectedContentLength = H2Headers.getContentLengthValue(current);
                    }
                    headers.add(current);
                }
                // check to see if the header name is "trailers"; if so, parse the value for later use
                if ("trailer".equalsIgnoreCase(current.getName()) && trailerHeaderNames == null) {
                    this.trailerExpected = true;
                    headersCompleted = false;
                    String[] trailerHeaders = current.getValue().split(",");
                    for (String header : trailerHeaders) {
                        header.trim();
                    }
                    trailerHeaderNames = java.util.Arrays.asList(trailerHeaders);
                }
            }
            // only set headers on the link once
            if (!processTrailerHeaders && h2HttpInboundLinkWrap.getHeadersLength() == 0) {
                //Add all decoded pseudo-headers / headers to the H2 inbound link wrap
                this.h2HttpInboundLinkWrap.setReadHeaders(headers);
                this.h2HttpInboundLinkWrap.setReadPseudoHeaders(pseudoHeaders);
                // add any trailer headers to the link header list
            } else if (processTrailerHeaders) {
                trailerExpected = false;
                ArrayList<H2HeaderField> readHeaders = h2HttpInboundLinkWrap.getReadHeaders();
                for (H2HeaderField header : headers) {
                    if (trailerHeaderNames.contains(header.getName())) {
                        readHeaders.add(header);
                    }
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "processCompleteHeaders no header block set: stream " + myID);
            }
        }
    }

    /**
     * Grab the data from the current frame
     */
    private void getBodyFromFrame() {
        if (dataPayload == null) {
            dataPayload = new ArrayList<byte[]>();
        }
        if (currentFrame.getFrameType() == FrameTypes.DATA) {
            dataPayload.add(((FrameData) currentFrame).getData());
        }
    }

    /**
     * Put the data payload for this stream into the read buffer that will be passed to the webcontainer.
     * This should only be called when this stream has received an end of stream flag.
     *
     * @throws ProtocolException
     */
    private void processCompleteData() throws ProtocolException {
        WsByteBufferPoolManager bufManager = HttpDispatcher.getBufferManager();
        WsByteBuffer buf = bufManager.allocate(getByteCount(dataPayload));
        for (byte[] bytes : dataPayload) {
            buf.put(bytes);
        }
        buf.flip();
        if (expectedContentLength != -1 && buf.limit() != expectedContentLength) {
            throw new ProtocolException("content-length header did not match the expected amount of data received");
        }
        moveDataIntoReadBufferArray(buf);
    }

    /**
     * Tell the HTTP inbound link that we have data ready for it to read
     */
    private void setReadyForRead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setReadyForRead entry: stream id:" + myID);
        }
        if (headersCompleted) {
            ExecutorService executorService = CHFWBundle.getExecutorService();
            Http2Ready readyThread = new Http2Ready(h2HttpInboundLinkWrap);
            executorService.execute(readyThread);
        }
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
            h2HttpInboundLinkWrap.ready(this.h2HttpInboundLinkWrap.vc);
            headersCompleted = false;
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
            while (!streamReadReady.isEmpty() && !streamReadReady.get(0).hasRemaining()) {
                streamReadReady.get(0).release();
                streamReadReady.remove(0);
            }
            requestBuffers[reqArrayIndex].put(streamReadReady.get(0).get());
        }

        // put stream array back in shape
        streamReadSize = 0;
        readLatch = new CountDownLatch(1);
        for (WsByteBuffer buffer : ((ArrayList<WsByteBuffer>) streamReadReady.clone())) {
            streamReadReady.clear();
            if (buffer.hasRemaining()) {
                moveDataIntoReadBufferArray(buffer.slice());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "read exit: " + streamId());
        }
        // return the vc since this was a successful read
        return h2HttpInboundLinkWrap.getVirtualConnection();
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
     */
    private boolean writeFrameSync() throws FlowControlException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "writeFrameSync entry: stream: " + myID + " write out the following frame: " + currentFrame);
        }

        if (isStreamClosed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeFrameSync exit - stream " + myID + " is closed");
            }
            return false;
        }
        if (currentFrame.isWriteFrame() && currentFrame.getInitialized()) {
            byte[] writeFrameBytes = currentFrame.buildFrameForWrite();

            WsByteBufferPoolManager mgr = HttpDispatcher.getBufferManager();
            WsByteBuffer writeFrame = mgr.allocate(writeFrameBytes.length);

            try {
                writeFrame.put(writeFrameBytes);
                writeFrame.flip();

                // We need to check to see if the write window is large enough to write this data.
                // If it's not, we'll queue it up and wait for the client to update the window
                if (currentFrame.getFrameType() == FrameTypes.DATA) {
                    if (!isWindowLimitExceeded(currentFrame) && !waitingForWindowUpdate) {
                        streamWindowUpdateWriteLimit -= currentFrame.getPayloadLength();

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "stream: " + myID + " Data payload written - new streamWindowUpdateWriteLimit: " + streamWindowUpdateWriteLimit);
                        }
                    } else {
                        waitingForWindowUpdate = true;
                        // the write window is too small: save this frame off until the client sends a WINDOW_UPDATE
                        if (dataWaitingForWindowUpdate == null) {
                            dataWaitingForWindowUpdate = new LinkedList<FrameData>();
                        }
                        dataWaitingForWindowUpdate.add((FrameData) currentFrame);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "writeFrameSync the write window is too small, so the current DATA frame will be queued on stream " + myID);
                        }
                        return false;
                    }
                }

                muxLink.writeSync(writeFrame, null, writeFrame.limit(), TCPRequestContext.NO_TIMEOUT,
                                  currentFrame.getFrameType(), currentFrame.getPayloadLength(), myID);

            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeFrameSync caught an IOException: " + e);
                }
                // release buffer used to synchronously write the frame
                writeFrame.release();
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
            muxLink.initLock.await();
            // check to see if the initialization failed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "waitForConnectionInit: stop waiting, H2 connection initialized " + streamId());
            }
            return true;
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
}
