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
import java.util.Queue;
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
 */
public class H2StreamProcessor {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2StreamProcessor.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    // This class is intentionally not thread safe, it is designed with the idea that per stream only one frame will be processed at one time.
    StreamState state = StreamState.IDLE;

    Frame currentFrame;

    FrameTypes frameType;

    // stream data
    byte[] headerBlock;
    byte[] dataPayload;

    // stream settings
    int weight = 0;
    boolean exclusive = false;
    int streamDependency = 0;

    long readReceivedCount = 0;
    long readRequestSize = 0;

    // if false, this stream's buffers do not contain a complete set of headers
    private boolean headersCompleted = false;
    private boolean continuationExpected = false;

    public boolean endStream = false;

    int myID = -1;
    H2HttpInboundLinkWrap h2HttpInboundLinkWrap = null;
    H2InboundLink muxLink = null;

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

    private long closeTime = Constants.INITIAL_CLOSE_TIME;

    // TODO refine buffer handling
    WsByteBuffer[] streamReadReady = new WsByteBuffer[32]; // hard code the size for now
    int streamReadSize = 0;
    int streamReadBufferIndex = 0;
    long actualReadCount = 0;

    public H2StreamProcessor(Integer id, H2HttpInboundLinkWrap link, H2InboundLink m) {
        this(id, link, m, StreamState.IDLE);
    }

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
            this.writeFrameSync();
        } catch (FlowControlException e) {
            // FlowControlException can only occur writing DATA frames
        }
        if (Constants.SPEC_INITIAL_WINDOW_SIZE != muxLink.maxReadWindowSize) {
            // the user has changed the max connection read window, so we'll update that now
            currentFrame = new FrameWindowUpdate(0, (int) muxLink.maxReadWindowSize, false);
            try {
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
            Tr.debug(tc, "processNextFrame-entry:  stream: " + myID + " frame type: " + frame.getFrameType().toString() + " direction: " + direction.toString());
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
                    if (state.equals(StreamState.HALF_CLOSED_REMOTE)) {
                        throw new ProtocolException("Stream half closed remote and received a DATA or HEADERS frame");
                    } else {
                        //If we're not half closed remote then we don't need to do anything with the frame received
                        return;
                    }
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
                    Tr.debug(tc, "processNextFrame: addFrame GOAWAY: highestStreamIDToProcess: " + muxLink.getLastStreamToProcess());
                }
                currentFrame = new FrameGoAway(0, addFrameException.getMessage().getBytes(), addFrameException.getErrorCode(), muxLink.getLastStreamToProcess(), false);
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
                if (addFrame == ADDITIONAL_FRAME.FIRST_TIME) {
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

                if (muxLink.checkStreamCloseVersusLinkState(frame.getStreamId())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processNextFrame: " + currentFrame.getFrameType() + " received on stream " + this.myID +
                                     " after a GOAWAY was sent.  " + "This frame will be ignored.");
                    }
                    return;
                }

                // Header frames must be received in a contiguous chunk; cannot interleave across streams
                if (isContinuationFrameExpected() && (frameType != FrameTypes.CONTINUATION || !this.continuationExpected)) {
                    addFrame = ADDITIONAL_FRAME.GOAWAY;
                    addFrameException = new ProtocolException("Did not receive the expected continuation frame");
                    System.out.println("wtlucy: writing exception for mismatched continuation");
                    continue;
                }

                if (frameType == FrameTypes.RST_STREAM || frameType == FrameTypes.SETTINGS ||
                    frameType == FrameTypes.GOAWAY || frameType == FrameTypes.PING) {

                    switch (frameType) {
                        case RST_STREAM:
                            processRstFrame();
                            break;
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

                // made it here, so frame seems to be legit given the frame state.

                // since we have already verify priority frame or window update frame  is allowed,
                // and since it will not change the state of the connection, process
                // the priority and window_update frame outside of state specific processing
                if (frameType == FrameTypes.PRIORITY) {
                    processPriorityFrame();
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
        if (currentFrame.getFrameType() == FrameTypes.GOAWAY
            || currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
            writeFrameSync();
            this.updateStreamState(StreamState.CLOSED);
            if (currentFrame.getFrameType() == FrameTypes.GOAWAY) {
                muxLink.goAway(muxLink.getLastStreamToProcess());
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

        if (state == StreamState.CLOSED || state == StreamState.HALF_CLOSED_LOCAL) {
            muxLink.setLastStreamToProcess(myID);
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
     */
    private void processHalfClosedRemote(Constants.Direction direction) throws FlowControlException {
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

            }
        } else if (currentFrame.getFrameType() == FrameTypes.RST_STREAM) {
            endStream = true;
            updateStreamState(StreamState.CLOSED);
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

    private void processSETTINGSFrame() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processSETTINGSFrame entry");
        }

        // this is the first Settings frame we're processing as part of the connection preface
        if (!muxLink.connection_preface_settings_rcvd) {
            muxLink.connection_preface_settings_rcvd = true;
        }
        if (((FrameSettings) currentFrame).flagAckSet()) {
            if (!muxLink.connection_preface_settings_ack_rcvd) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "connection preface completed; notify any waiting streams to continue");
                }

                muxLink.connection_preface_settings_ack_rcvd = true;
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
                writeFrameSync();
            } catch (FlowControlException e) {
                // FlowControlException cannot occur for FrameTypes.SETTINGS, so do nothing here but debug
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeSync caught (logically unexpected) FlowControlException: " + e);
                }
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
            Tr.debug(tc, "sendGOAWAYFrame sending a GOAWAY with Last-Stream-ID " + muxLink.getLastStreamToProcess()
                         + " and exception " + e.toString());
        }

        // send out a goaway in response; return the same last stream, for now
        Frame frame = new FrameGoAway(0, e.getMessage().getBytes(), e.getErrorCode(), muxLink.getLastStreamToProcess(), false);
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

        int lastStreamId = ((FrameGoAway) currentFrame).getLastStreamId();
        muxLink.triggerStreamClose(this);
        muxLink.setLastStreamToProcess(lastStreamId);

        // send out a goaway in response; return the same last stream, for now
        currentFrame = new FrameGoAway(0, new byte[0], 0, muxLink.getLastStreamToProcess(), false);

        try {
            writeFrameSync();
        } catch (FlowControlException e) {
            // FlowControlException cannot occur for FrameTypes.GOAWAY, so do nothing here but debug
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "writeSync caught (logically unexpected) FlowControlException: " + e);
            }
        } finally {

            muxLink.goAway(lastStreamId);
        }
    }

    private void processPINGFrame() {
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

    private void processIdle(Constants.Direction direction) throws CompressionException, FlowControlException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processIdle entry: stream " + myID);
        }

        // Can only receive HEADERS or PRIORITY frame in Idle state
        if (direction == Constants.Direction.READ_IN) {
            if (frameType == FrameTypes.HEADERS) {
                //if END_HEADERS is set and END_STREAM is not set,
                //    Stream goes to RemoteStarted_Open state.
                //if END_HEADERS is not set and END_STREAM is not set,
                //    then CONTINUATION frames must follow.  Stream is in RemoteStarted_Idle_Continuation state
                //if END_STREAM is set and END_HEADERS is not set,
                //    then CONTINUATION frames will still follow. Stream in RemoteStarted_Idle_EndStreamContinuation state
                //if END_STREAM is set and END_HEADERS is set
                //    then state goes to RemoteStarted_HalfCloseRemote state.

                // process the new priority settings if any were passed in the payload
                processHeadersPriority();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "header frame read in on stream " + myID);
                }
                // get the headers out of the current frame and save them off
                getHeadersFromFrame();

                // end headers flag is set, so we won't expect any continuation frames
                if (currentFrame.flagEndHeadersSet()) {
                    setHeadersComplete();
                    processCompleteHeaders();
                    // if headers and stream are done, then the body-less request is ready to be handed off to the WebContainer
                    if (currentFrame.flagEndStreamSet()) {
                        updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                        endStream = true;
                        setReadyForRead();
                    } else {
                        // stream is not done, so we are waiting for the http request body data on a DATA frame
                        updateStreamState(StreamState.OPEN);
                    }
                } else {
                    // remote closing the it's side of the stream, but it will still send more headers/continuation frames
                    setContinuationFrameExpected(true);
                    if (currentFrame.flagEndStreamSet()) {
                        endStream = true;
                    }
                }
            }

            if (frameType == FrameTypes.CONTINUATION) {
                getHeadersFromFrame();

                // the stream has been signaled to close: we don't need to worry about DATA frames following
                if (currentFrame.flagEndHeadersSet()) {
                    setHeadersComplete();
                    processCompleteHeaders();
                    updateStreamState(StreamState.OPEN);
                }
                if (endStream) {
                    // if the headers AND stream are done, the client is done with their req and we can send it up
                    updateStreamState(StreamState.HALF_CLOSED_REMOTE);
                } else {
                    // the headers are done, but since EOS is not set we need to anticipate DATA from the client
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

    private void processOpen(Constants.Direction direction) throws ProtocolException, FlowControlException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "processOpen entry: stream " + myID);
        }

        if (direction == Constants.Direction.READ_IN) {
            if (frameType == FrameTypes.DATA) {
                // if END_STREAM is set: change Stream state to RemoteStarted_HalfCloseRemote
                // if END_STREAM is not set: wait for more frames to arrive, Stream remains RemoteStarted_Open
                getBodyFromFrame();
                if (currentFrame.flagEndStreamSet()) {
                    endStream = true;
                    updateStreamState(StreamState.HALF_CLOSED_REMOTE);

                    // if headers and body and stream are done, then the request is ready to be handed off to the WebContainer
                    // and the response read
                    setReadyForRead();
                }
            }
        } else {
            // write out the current frame and update the stream state
            if (frameType == FrameTypes.PUSH_PROMISE) {
                // writing out a PP doesn't have any effect on the current stream, but rather the promised stream

            } else if (frameType == FrameTypes.HEADERS || frameType == FrameTypes.CONTINUATION) {
                //if END_HEADERS is set, Stream stays in RemoteStarted_Open.
                //if END_HEADERS is not set, then CONTINUATION frames must follow.  change Stream to RemoteStarted_Open_Continuation
                //if END_STREAM without END_HEADERS, then CONTINUATION frames will still follow. Stream in RemoteStarted_Open_EndStreamContinuation state
                //if END_STREAM and END_HEADERS then change Stream state to RemoteStarted_HalfCloseLocal.
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
                // Writing the HTTP/2 representation of the HTTP Response body here
                // if END_STREAM is set, change Stream state to RemoteStarted_HalfCloseLocal
                // if END_STREAM is not set, wait for more frames to write, Stream remains RemoteStarted_Open state.
                endStream = true;
                updateStreamState(StreamState.HALF_CLOSED_LOCAL);
            }
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
     * TODO There may be a problem here, since a RST_STREAM frame can come in on the reserved PP
     * stream
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
                // meaning the client/remote side is starting a new stream and sending a request on it
                if (state != StreamState.IDLE) {
                    throw new ProtocolException("HEADERS Frame Received in the wrong state of: " + state);
                } else if (state == StreamState.HALF_CLOSED_REMOTE) {
                    throw new StreamClosedException("HEADERS Frame Received in the wrong state of: " + state);
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
                // seems that this can be received in any stream state
                break;

            case CONTINUATION:
                if (!isContinuationFrameExpected()) {
                    // not in a continuation state
                    throw new ProtocolException("CONTINUATION Frame Received when not in a Continuation State");
                } else if (state == StreamState.HALF_CLOSED_REMOTE) {
                    throw new StreamClosedException("CONTINUATION Frame Received in the wrong state of: " + state);
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
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "completed headers have been received stream " + myID);
        }
        headersCompleted = true;
        setContinuationFrameExpected(false);
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
                headerBlock = hbf;
            } else {
                headerBlock = concatenateArrays(headerBlock, hbf);
            }
        }
    }

    /**
     * Puts stream's header block into the read buffer that will be passed to the webcontainer
     *
     * @throws CompressionException
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
            WsByteBuffer buf = bufManager.allocate(this.headerBlock.length);
            buf.put(this.headerBlock);
            buf.flip();
            moveDataIntoReadBufferArray(buf);
            headerBlock = null;

            //Get ready to decode headers
            boolean isFirstLineComplete = false;
            HashMap<String, String> pseudoHeaders = new HashMap<String, String>();
            ArrayList<H2HeaderField> headers = new ArrayList<H2HeaderField>();
            H2HeaderField current = null;
            //Decode headers until we reach the end of the buffer
            while (buf.hasRemaining()) {
                current = (H2Headers.decodeHeader(buf, this.muxLink.getReadTable()));
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
                        headers.add(current);
                    }
                } else {
                    //If header starts with ':' throw error
                    if (current.getName().startsWith(":")) {
                        this.muxLink.getReadTable().setDynamicTableValidity(false);
                        throw new CompressionException("Invalid pseudo-header decoded: all pseudo-headers must appear " +
                                                       "in the header block before regular header fields.");
                    }
                    headers.add(current);
                }
            }
            //Add all decoded pseudo-headers / headers to the H2 inbound link wrap
            this.h2HttpInboundLinkWrap.setReadPseudoHeaders(pseudoHeaders);
            this.h2HttpInboundLinkWrap.setReadHeaders(headers);
            buf.flip();
        }
    }

    /**
     * Put the payload from the current Data frame into the read buffer that will be passed to the webcontainer
     */
    private void getBodyFromFrame() {
        if (currentFrame.getFrameType() == FrameTypes.DATA) {
            dataPayload = ((FrameData) currentFrame).getData();
            WsByteBufferPoolManager bufManager = HttpDispatcher.getBufferManager();
            WsByteBuffer buf = bufManager.allocate(this.dataPayload.length);
            buf.put(this.dataPayload);
            moveDataIntoReadBufferArray(buf);
        }
    }

    /**
     * Tell the HTTP inbound link that we have data ready for it to read
     */
    private void setReadyForRead() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setReadyForRead entry: stream id:" + myID);
        }

        if (headersCompleted) {
            //h2HttpInboundLinkWrap.ready(this.h2HttpInboundLinkWrap.vc);
            //headersCompleted = false;
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

    private void moveDataIntoReadBufferArray(WsByteBuffer newBuf) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "moveDataIntoReadBufferArray entry: stream " + myID + " buffer: " + newBuf);
        }

        // new data starts at Position and goes to Limit (one before limit)
        // move the data that is to be sent up the channel into the currentReadReady byte array, and update the currentReadSize

        if (newBuf != null) {
            int size = newBuf.remaining(); // limit - pos
            streamReadReady[streamReadBufferIndex] = newBuf;
            streamReadBufferIndex++;
            streamReadSize += size;
        }
    }

    public VirtualConnection read(long numBytes, WsByteBuffer[] req) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "read entry: stream " + myID + " request: " + req + " num bytes requested: " + numBytes);
        }

        long streamCount = 0;
        long requestCount = 0;

        // if at least numBytes are ready to be processed as part of the HTTP Request/Response, then load it up
        if (numBytes > streamReadSize) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "read exit: stream " + myID + " more bytes requested than available for read: " + numBytes + " > " + streamReadSize);
            }
            return null;
        }

        streamCount = streamReadSize;
        requestCount = bytesRemaining(req);

        // copy the data from the stream read buffer array, into calling read buffer array.
        if (streamCount < requestCount) {
            actualReadCount = streamCount;
        } else {
            actualReadCount = requestCount;
        }

        // so this is going to be a slow, brute force, one byte copy at a time, needs to be optimized later
        int streamArrayIndex = 0;
        int reqArrayIndex = 0;

        for (int i = 0; i < actualReadCount; i++) {

            if (!req[reqArrayIndex].hasRemaining()) {
                // move to the next buffer in the array
                reqArrayIndex++;
                while (true) {
                    if (req[reqArrayIndex].hasRemaining()) {
                        break;
                    } else {
                        reqArrayIndex++;
                    }
                }
            }

            // if nothing left in this buffer, then move to the next buffer in the array
            if (!streamReadReady[streamArrayIndex].hasRemaining()) {

                // done with this buffer, so release it
                streamReadReady[streamArrayIndex].release();

                streamArrayIndex++;
                while (true) {
                    if (streamReadReady[streamArrayIndex].hasRemaining()) {
                        break;
                    } else {
                        streamArrayIndex++;
                    }
                }
            }

            req[reqArrayIndex].put(streamReadReady[streamArrayIndex].get());
        }

        // put stream array back in shape
        int streamOldBufferIndex = streamReadBufferIndex;
        streamReadBufferIndex = 0;
        streamReadSize = 0;
        if (streamReadReady[streamArrayIndex].hasRemaining()) {
            moveDataIntoReadBufferArray(streamReadReady[streamArrayIndex].slice());
        }
        streamArrayIndex++;
        while (streamArrayIndex <= streamOldBufferIndex) {
            moveDataIntoReadBufferArray(streamReadReady[streamArrayIndex]);
            streamArrayIndex++;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "read exit: stream " + myID);
        }
        // return the vc since this was a successful read
        return h2HttpInboundLinkWrap.getVirtualConnection();
    }

    public long readCount(long numBytes, WsByteBuffer[] req) {

        if (read(numBytes, req) != null) {
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
            } finally {
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
                Tr.debug(tc, "isStreamClosed stream closed; stream: " + myID);
            }
            return true;
        }

        boolean rc = muxLink.checkStreamCloseVersusLinkState(myID);

        if (rc == true) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isStreamClosed stream closed via muxLink check; stream: " + myID);
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

    /**
     * Combine arrays a and b
     *
     * @param a the array to be copied first
     * @param b the array to be copied second
     * @return a new array of length a + b
     */
    protected byte[] concatenateArrays(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] concatenated = new byte[aLen + bLen];
        System.arraycopy(a, 0, concatenated, 0, aLen);
        System.arraycopy(b, 0, concatenated, aLen, bLen);
        return concatenated;
    }

    /**
     * @param frame to check
     * @return true if the passed frame is an HTTP2 control frame
     */
    private boolean isControlFrame(Frame frame) {
        FrameTypes type = frame.getFrameType();
        if (type == FrameTypes.GOAWAY || type == FrameTypes.RST_STREAM || type == FrameTypes.SETTINGS
            || type == FrameTypes.WINDOW_UPDATE || type == FrameTypes.PING || type == FrameTypes.PRIORITY) {
            return true;
        }
        return false;
    }

    protected void setCloseTime(long x) {
        closeTime = x;
    }

    protected long getCloseTime() {
        return closeTime;
    }
}
