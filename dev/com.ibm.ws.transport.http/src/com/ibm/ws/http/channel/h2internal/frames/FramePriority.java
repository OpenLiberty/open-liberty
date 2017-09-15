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
import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;

public class FramePriority extends Frame {

    /*
     * format of priority frame
     *
     * length(24) 00 00 05 - length of payload
     * type(8) 02 - PRIORITY
     * flags(8) 00 - No flags
     * R (1) 0
     * Stream (31) XX XX XX XX
     *
     * Payload
     * Exclusive (1)
     * Stream Dependency (31)
     * Weight (8)
     */

    private boolean exclusive = false;
    private int streamDependency = 0;
    private int weight = 0;

    /**
     * Read frame constructor
     */
    public FramePriority(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.PRIORITY;
    }

    /**
     * Write frame constructor
     */
    public FramePriority(int streamId, int streamDependency, int weight, boolean exclusive, boolean reserveBit) {
        super(streamId, 5, (byte) 0x00, reserveBit, FrameDirection.WRITE);

        this.exclusive = exclusive;
        this.streamDependency = streamDependency;
        this.weight = weight;

        frameType = FrameTypes.PRIORITY;
        setInitialized(); // we have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // PRIORITY payload data
        // +-+-------------------------------------------------------------+------------+
        // |E|                  Stream Dependency (31)                     | Weight (8) |
        // +-+-------------+-----------------------------------------------+------------+

        try {
            byte firstPayloadByte = frp.grabNextByte();

            exclusive = utils.getReservedBit(firstPayloadByte);

            firstPayloadByte = (byte) (firstPayloadByte & Constants.MASK_7F);
            streamDependency = frp.grabNext24BitInt(firstPayloadByte);

            // convert signed byte to integer as if the byte was unsigned.
            weight = (0x00FF & frp.grabNextByte());

            // weight comes over the wire as 0 - 255, but as per spec instructions should be 1 - 256 internally
            weight = weight + 1;
        } catch (FrameSizeException e) {
            e.setConnectionError(false);
            throw e;
        }

    }

    @Override
    public byte[] buildFrameForWrite() {

        byte[] frame = super.buildFrameForWrite();

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_PRIORITY);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        utils.Move31BitstoByteArray(streamDependency, frame, frameIndex);
        if (exclusive) {
            frame[frameIndex] = (byte) (frame[frameIndex] | 0x80);
        }
        frameIndex += 4;
        utils.Move8BitstoByteArray(weight, frame, frameIndex);

        return frame;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException {
        if (streamId == 0) {
            throw new ProtocolException("PRIORITY frame stream ID cannot be 0x0");
        }
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public int getStreamDependency() {
        return streamDependency;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    protected void setFlags() {
        // No flags defined
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FramePriority)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "equals: object is not a FramePriority");
            }
            return false;
        }

        FramePriority framePriorityToCompare = (FramePriority) object;

        if (!super.equals(framePriorityToCompare))
            return false;

        if (this.isExclusive() != framePriorityToCompare.isExclusive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.isExclusive() = " + this.isExclusive() + " framePriorityToCompare.isExclusive() = " + framePriorityToCompare.isExclusive());
            }
            return false;
        }
        if (this.getStreamDependency() != framePriorityToCompare.getStreamDependency()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getStreamDependency() = " + this.getStreamDependency() + " framePriorityToCompare.getStreamDependency() = "
                             + framePriorityToCompare.getStreamDependency());
            }
            return false;
        }
        if (this.getWeight() != framePriorityToCompare.getWeight()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getWeight() = " + this.getWeight() + " framePriorityToCompare.getWeight() = " + framePriorityToCompare.getWeight());
            }
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("Exclusive: " + this.isExclusive() + "\n");
        frameToString.append("StreamDependency: " + this.getStreamDependency() + "\n");
        frameToString.append("Weight: " + this.getWeight() + "\n");

        return frameToString.toString();

    }
}
