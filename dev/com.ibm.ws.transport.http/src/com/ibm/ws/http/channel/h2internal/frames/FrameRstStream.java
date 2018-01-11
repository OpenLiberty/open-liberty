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
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;

public class FrameRstStream extends Frame {

    /*
     * format of priority frame
     *
     * length(24) 00 00 04 length of payload
     * type(8) 03 RST_STREAM
     * flags(8) 00 No flags
     * R (1) 0
     * Stream (31) XX XX XX XX
     *
     * Payload
     * Error Code (32) XX XX XX XX
     */

    private int errorCode;

    /**
     * Read frame constructor
     */
    public FrameRstStream(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.RST_STREAM;
    }

    /**
     * Write frame constructor
     */
    public FrameRstStream(int streamId, int errorCode, boolean reserveBit) {
        super(streamId, 4, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        frameType = FrameTypes.RST_STREAM;
        this.errorCode = errorCode;
        setInitialized(); // we have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // RST_STREAM payload data
        // +---------------------------------------------------------------+
        // |                        Error Code (32)                        |
        // +---------------------------------------------------------------+
        errorCode = frp.grabNext32BitInt();
    }

    @Override
    public byte[] buildFrameForWrite() {

        byte[] frame = super.buildFrameForWrite();

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_RST_STREAM);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        utils.Move32BitstoByteArray(errorCode, frame, frameIndex);

        return frame;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException {
        if (streamId == 0) {
            throw new ProtocolException("RST_STREAM frame stream ID cannot be 0");
        }
    }

    @Override
    protected void setFlags() {
        // No flags defined

    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameRstStream)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "equals: object is not a FrameRstStream");
            }
            return false;
        }

        FrameRstStream frameRstStreamToCompare = (FrameRstStream) object;

        if (!super.equals(frameRstStreamToCompare))
            return false;

        if (this.getErrorCode() != frameRstStreamToCompare.getErrorCode()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getErrorCode() = " + this.getErrorCode() + " frameRstStreamToCompare.getErrorCode() = "
                             + frameRstStreamToCompare.getErrorCode());
            }
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("ErrorCode: " + this.getErrorCode() + "\n");

        return frameToString.toString();

    }

}
