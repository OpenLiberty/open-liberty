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
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FrameWindowUpdate extends Frame {

    /*
     * format of window update frame
     *
     * length(24) 00 00 04 - length of payload
     * type(8) 08 - WINDOW_UPDATE
     * flags(8) 00 - none
     * R (1) 0
     * Stream (31) 00 00 00 00
     *
     * R (1) 0
     * Window Size Increment (31) - XX XX XX XX
     */

    private int windowSizeIncrement;

    /**
     * Read frame constructor
     */
    public FrameWindowUpdate(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.WINDOW_UPDATE;
    }

    /**
     * Write frame constructor
     */
    public FrameWindowUpdate(int streamId, int windowSizeIncrement, boolean reserveBit) {
        super(streamId, 4, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        this.windowSizeIncrement = windowSizeIncrement;
        frameType = FrameTypes.WINDOW_UPDATE;
        writeFrameLength += payloadLength;
        setInitialized(); // we have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // +-+-------------------------------------------------------------+
        // |R|              Window Size Increment (31)                     |
        // +-+-------------------------------------------------------------+

        byte firstPayloadByte = frp.grabNextByte();

        reservedBit = utils.getReservedBit(firstPayloadByte);

        firstPayloadByte = (byte) (firstPayloadByte & Constants.MASK_7F);
        windowSizeIncrement = frp.grabNext24BitInt(firstPayloadByte);
    }

    @Override
    public WsByteBuffer buildFrameForWrite() {

        WsByteBuffer buffer = super.buildFrameForWrite();
        byte[] frame;
        if (buffer.hasArray()) {
            frame = buffer.array();
        } else {
            frame = super.createFrameArray();
        }

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_WINDOW_UPDATE);

        // now for the Setting Frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        // then the four byte payload
        utils.Move31BitstoByteArray(windowSizeIncrement, frame, frameIndex);

        buffer.put(frame, 0, writeFrameLength);
        buffer.flip();
        return buffer;
    }

    public int getWindowSizeIncrement() {
        return windowSizeIncrement;
    }

    @Override
    protected void setFlags() {
        // No flags defined

    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException, FrameSizeException {
        // A receiver MUST treat the receipt of a WINDOW_UPDATE frame with a
        // flow-control window increment of 0 as a stream error (Section 5.4.2)
        // of type PROTOCOL_ERROR; errors on the connection flow-control window
        // MUST be treated as a connection error (Section 5.4.1).
        if (windowSizeIncrement < 1) {
            ProtocolException e = new ProtocolException("WINDOW_UPDATE flow-control window increment must be greater than 0");
            if (this.streamId > 0) {
                // flow control
                e.setConnectionError(false);
                throw e;
            } else {
                throw e;
            }
        }
        // A WINDOW_UPDATE frame with a length other than 4 octets MUST be
        // treated as a connection error (Section 5.4.1) of type
        // FRAME_SIZE_ERROR.
        if (this.payloadLength != 4) {
            throw new FrameSizeException("WINDOW_UPDATE frame must have a length of 4.  Actual length : " + payloadLength);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameWindowUpdate)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "equals: object is not a FrameWindowUpdate, it is a " + object.getClass());
            }
            return false;
        }

        FrameWindowUpdate frameWindowUpdateToCompare = (FrameWindowUpdate) object;

        if (!super.equals(frameWindowUpdateToCompare))
            return false;

        if (this.getWindowSizeIncrement() != frameWindowUpdateToCompare.getWindowSizeIncrement()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getWindowSizeIncrement() = " + this.getWindowSizeIncrement() + " frameWindowUpdateToCompare.getWindowSizeIncrement() = "
                             + frameWindowUpdateToCompare.getWindowSizeIncrement());
            }
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("WindowSizeIncrement: " + this.getWindowSizeIncrement() + "\n");

        return frameToString.toString();

    }

}
