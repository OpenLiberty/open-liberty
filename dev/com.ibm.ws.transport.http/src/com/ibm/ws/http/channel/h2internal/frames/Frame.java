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
package com.ibm.ws.http.channel.h2internal.frames;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 *
 */
public abstract class Frame {

    /** RAS tracing variable */
    protected static final TraceComponent tc = Tr.register(Frame.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    protected int streamId;
    protected int payloadLength;

    public enum FrameDirection {
        WRITE, READ;
    }

    FrameDirection direction;

    protected byte flags;
    protected boolean reservedBit;
    protected FrameTypes frameType;
    public static final int SIZE_FRAME_BEFORE_PAYLOAD = 9;

    protected boolean END_STREAM_FLAG;
    protected boolean END_HEADERS_FLAG;
    protected boolean PADDED_FLAG;
    protected boolean PRIORITY_FLAG;
    protected boolean ACK_FLAG;

    protected boolean initialized;

    public Frame(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        this.streamId = streamId;
        this.payloadLength = payloadLength;
        this.flags = flags;
        this.reservedBit = reserveBit;
        this.initialized = false;
        this.direction = direction;
        setFlags();
    }

    // ################# FRAME READ OPS #####################################
    abstract public void validate(H2ConnectionSettings settings) throws Http2Exception;

    /**
     * Grabs the payload out of a binary read frame
     */
    abstract public void processPayload(FrameReadProcessor frp) throws FrameSizeException;

    // ################# FRAME WRITE OPS #####################################

    abstract protected void setFlags();

    protected byte[] createFrameArray() {
        return new byte[SIZE_FRAME_BEFORE_PAYLOAD + payloadLength];
    }

    protected void setFrameHeaders(byte[] frame, byte type) {
        // set up the standard 9 bytes that make up every frame
        utils.Move24BitstoByteArray(payloadLength, frame, 0);
        frame[utils.FRAME_TYPE_INDEX] = type;

        frame[utils.FRAME_FLAGS_INDEX] = 0x00;

        // TODO: verify this logic
        if (PADDED_FLAG) {
            frame[utils.FRAME_FLAGS_INDEX] = (byte) (frame[utils.FRAME_FLAGS_INDEX] | 0x08);
        }
        if (END_HEADERS_FLAG) {
            frame[utils.FRAME_FLAGS_INDEX] = (byte) (frame[utils.FRAME_FLAGS_INDEX] | 0x04);
        }
        if (ACK_FLAG | END_STREAM_FLAG) {
            frame[utils.FRAME_FLAGS_INDEX] = (byte) (frame[utils.FRAME_FLAGS_INDEX] | 0x01);
        }
        if (PRIORITY_FLAG) {
            frame[utils.FRAME_FLAGS_INDEX] = (byte) (frame[utils.FRAME_FLAGS_INDEX] | 0x20);
        }
        if (this.reservedBit) {
            streamId = streamId ^ (1 << 31);
            utils.Move32BitstoByteArray(streamId, frame, utils.FRAME_STREAM_START_INDEX_INT);
        } else {
            utils.Move31BitstoByteArray(streamId, frame, utils.FRAME_STREAM_START_INDEX_INT);
        }
    }

    /**
     * Build the byte representation of a frame
     *
     * @return
     */
    public byte[] buildFrameForWrite() {
        if (!initialized) {
            return null;
        }

        // set up the frame byte array
        byte[] frame = createFrameArray();

        return frame;
    }

    // ################# SETTERS / GETTERS #####################################
    public byte getFrameFlags() {
        return flags;
    }

    public boolean isReadFrame() {
        return (direction == FrameDirection.READ);
    }

    public boolean isWriteFrame() {
        return (direction == FrameDirection.WRITE);
    }

    public boolean getFrameReserveBit() {
        return reservedBit;
    }

    public int getStreamId() {
        return this.streamId;
    }

    public void setInitialized() {
        initialized = true;
    }

    public boolean getInitialized() {
        return initialized;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public FrameTypes getFrameType() {
        return frameType;
    }

    public boolean flagEndStreamSet() {
        return this.END_STREAM_FLAG;
    }

    public boolean flagEndHeadersSet() {
        return this.END_HEADERS_FLAG;
    }

    public boolean flagPrioritySet() {
        return this.PRIORITY_FLAG;
    }

    public boolean flagAckSet() {
        return this.ACK_FLAG;
    }

    public boolean flagPaddingSet() {
        return this.PADDED_FLAG;
    }

    public boolean setAckFlag() {
        return this.ACK_FLAG = true;
    }

    public boolean flagPaddedSet() {
        return PADDED_FLAG;
    }

    public void setPaddedFlag() {
        PADDED_FLAG = true;
    }

    /**
     * Checks frame flags, type, reserve bit, payload length, and stream ID.
     */
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Frame))
            return false;

        Frame frameToCompare = (Frame) object;

        if (this.flagAckSet() != frameToCompare.flagAckSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.flagAckSet() = " + this.flagAckSet() + " frameToCompare.flagAckSet() = " + frameToCompare.flagAckSet());
            }
            return false;
        }
        if (this.flagPrioritySet() != frameToCompare.flagPrioritySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.flagPrioritySet() = " + this.flagPrioritySet() + " frameToCompare.flagPrioritySet() = " + frameToCompare.flagPrioritySet());
            }
            return false;
        }
        if (this.flagEndStreamSet() != frameToCompare.flagEndStreamSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.flagEndStreamSet() = " + this.flagEndStreamSet() + " frameToCompare.flagEndStreamSet() = " + frameToCompare.flagEndStreamSet());
            }
            return false;
        }
        if (this.flagEndHeadersSet() != frameToCompare.flagEndHeadersSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.flagEndHeadersSet() = " + this.flagEndHeadersSet() + " frameToCompare.flagEndHeadersSet() = " + frameToCompare.flagEndHeadersSet());
            }
            return false;
        }
        if (this.flagPaddedSet() != frameToCompare.flagPaddedSet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.flagPaddedSet() = " + this.flagPaddedSet() + " frameToCompare.flagPaddedSet() = " + frameToCompare.flagPaddedSet());
            }
            return false;
        }
        if (this.getFrameType() != frameToCompare.getFrameType()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getFrameType is false");
            }
            return false;
        }
        if (this.getFrameReserveBit() != frameToCompare.getFrameReserveBit()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getFrameReserveBit is false");
            }
            return false;
        }
        if (this.getPayloadLength() != frameToCompare.getPayloadLength()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getPayloadLength is false");
            }
            return false;
        }
        if (this.getStreamId() != frameToCompare.getStreamId()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getStreamId is false");
            }
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append("FrameType: " + this.getFrameType() + "\n");

        frameToString.append("FrameFlags:\n");
        frameToString.append(" FlagAckSet: ").append(this.flagAckSet()).append("\n");
        frameToString.append(" FlagPrioritySet: ").append(this.flagPrioritySet()).append("\n");
        frameToString.append(" FlagEndStreamSet: ").append(this.flagEndStreamSet()).append("\n");
        frameToString.append(" FlagEndHeadersSet: ").append(this.flagEndHeadersSet()).append("\n");
        frameToString.append(" FlagPaddedSet: ").append(this.flagPaddedSet()).append("\n");

        frameToString.append("FrameReserveBit: ").append(this.getFrameReserveBit()).append("\n");
        frameToString.append("PayloadLength: ").append(this.getPayloadLength()).append("\n");
        frameToString.append("StreamId: ").append(this.getStreamId()).append("\n");

        return frameToString.toString();
    }
}
