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

public class FrameGoAway extends Frame {

    /*
     * format of goaway frame
     *
     * length(24) XX XX XX - length of payload
     * type(8) 07 - GOAWAY
     * flags(8) 00 - No flags
     * R (1) 0
     * Stream (31) 00 00 00 00
     * Payload
     * Reserve(1) 0
     * Last-Stream-ID (31) XX XX XX XX
     * Error Code (32) XX XX XX XX
     * Additional Debug Data (*)
     */

    private int errorCode = -1;
    private int lastStreamId = -1;
    private byte[] debugData = null;

    /**
     * Read frame constructor
     */
    public FrameGoAway(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.GOAWAY;
    }

    /**
     * Write frame constructor
     */
    public FrameGoAway(int streamId, byte[] debugData, int errorCode, int lastStreamId, boolean reserveBit) {
        super(streamId, 4 + 4, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        if (debugData != null) {
            this.payloadLength += debugData.length;
            this.debugData = debugData;
        } else {
            this.debugData = new byte[0];
        }
        this.errorCode = errorCode;
        this.lastStreamId = lastStreamId;
        frameType = FrameTypes.GOAWAY;
        setInitialized();
    }

    /**
     * Write frame constructor
     */
    public FrameGoAway() {
        this(0, new byte[] {}, 0, 1, false);
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // +-+-------------------------------------------------------------+
        // |R|                  Last-Stream-ID (31)                        |
        // +-+-------------------------------------------------------------+
        // |                      Error Code (32)                          |
        // +---------------------------------------------------------------+
        // |                  Additional Debug Data (*)                    |
        // +---------------------------------------------------------------+

        byte firstPayloadByte = frp.grabNextByte();

        reservedBit = utils.getReservedBit(firstPayloadByte);

        firstPayloadByte = (byte) (firstPayloadByte & Constants.MASK_7F);
        setLastStreamId(frp.grabNext24BitInt(firstPayloadByte));
        setErrorCode(frp.grabNext32BitInt());

        int payloadIndex = 8;

        if (payloadLength - payloadIndex > 0) {
            debugData = new byte[payloadLength - payloadIndex];
            // if anything's left in the payload after the error code, throw it into debugData
            for (int i = 0; payloadIndex++ < payloadLength; i++) {
                debugData[i] = (frp.grabNextByte());
            }
        }
    }

    @Override
    public byte[] buildFrameForWrite() {

        byte[] frame = super.buildFrameForWrite();

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_GOAWAY);

        // now for the Setting Frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        // move over the lastStreamId
        utils.Move31BitstoByteArray(lastStreamId, frame, frameIndex);
        frameIndex += 4;

        // move over the errorCode
        utils.Move31BitstoByteArray(getErrorCode(), frame, frameIndex);
        frameIndex += 4;

        // move over debugData, if there is any
        for (int i = 0; i < debugData.length; i++) {
            frame[frameIndex] = debugData[i];
            frameIndex++;
        }
        return frame;
    }

    public int getLastStreamId() {
        return lastStreamId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public byte[] getDebugData() {
        return debugData;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException {
        if (streamId != 0) {
            throw new ProtocolException("GOAWAY frame streamID must be 0x0 - received: " + streamId);
        }
    }

    @Override
    protected void setFlags() {
        // No flags defined
    }

    /**
     * @param errorCode the errorCode to set
     */
    private void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * @param lastStreamId the lastStreamId to set
     */
    private void setLastStreamId(int lastStreamId) {
        this.lastStreamId = lastStreamId;
    }

    /**
     * @param debugData the debugData to set
     */
    public void setDebugData(byte[] debugData) {
        this.debugData = debugData;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameGoAway)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "equals: object is not a FrameGoAway");
            }
            return false;
        }

        FrameGoAway frameGoAwayToCompare = (FrameGoAway) object;

        if (!super.equals(frameGoAwayToCompare))
            return false;

        if (this.getLastStreamId() != frameGoAwayToCompare.getLastStreamId()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getLastStreamId() = " + this.getLastStreamId() + " frameGoAwayToCompare.getLastStreamId() = "
                             + frameGoAwayToCompare.getLastStreamId());
            }
            return false;
        }
        if (this.getErrorCode() != frameGoAwayToCompare.getErrorCode()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getErrorCode() = " + this.getErrorCode() + " frameGoAwayToCompare.getErrorCode() = "
                             + frameGoAwayToCompare.getErrorCode());
            }
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("LastStreamId: " + this.getLastStreamId() + "\n");
        frameToString.append("ErrorCode: " + this.getErrorCode() + "\n");
        String errorCode = "";
        if (this.getDebugData() != null) {
            errorCode = new String(this.getDebugData());
        }
        frameToString.append("DebugData: " + errorCode + "\n");

        return frameToString.toString();

    }
}
