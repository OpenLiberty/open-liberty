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

import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FrameData extends Frame {

    /*
     * format of data frame
     *
     * length(24) XX XX XX - length of payload
     * type(8) 00 - DATA
     * flags(8) bits: 0000 x00x - END_STREAM (0x1), PADDED (0x8)
     * R (1) 0
     * Stream (31) xx xx xx xx
     * PayloadstreamID
     * Pad Length? (8)
     * Data (*)
     * Padding (*)
     */

    private int paddingLength = 0;
    private byte[] data;
    WsByteBuffer dataBuffer;

    /**
     * Read frame constructor
     */
    public FrameData(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.DATA;
    }

    /**
     * Write frame constructor
     */
    public FrameData(int streamId, byte[] data, int paddingLength, boolean endStream, boolean padded, boolean reserveBit) {
        super(streamId, data.length, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        this.data = data;
        if (data != null) {
            this.dataBuffer = getBuffer(data.length).put(data).flip();
        }
        this.paddingLength = paddingLength;
        this.PADDED_FLAG = padded;
        this.END_STREAM_FLAG = endStream;
        if (padded) {
            // padding length must be contained in one byte, so don't accept values greater than 2^8 - 1
            paddingLength = (paddingLength > 255) ? 255 : paddingLength;
            payloadLength += paddingLength + 1; // padding length + padding field length
        }
        frameType = FrameTypes.DATA;
        setInitialized(); // we should have everything we need to write out, now
    }

    /**
     * Write frame constructor
     */
    public FrameData(int streamId, byte[] data, boolean endStream) {
        super(streamId, data.length, (byte) 0x00, false, FrameDirection.WRITE);
        this.data = data;
        if (data != null) {
            this.dataBuffer = getBuffer(data.length).put(data).flip();
        }
        this.PADDED_FLAG = false;
        this.END_STREAM_FLAG = endStream;
        frameType = FrameTypes.DATA;
        if (data != null) {
            writeFrameLength += data.length;
        }
        setInitialized(); // we should have everything we need to write out, now
    }

    /**
     * Write frame constructor
     */
    public FrameData(int streamId, WsByteBuffer data, int length, boolean endStream) {
        super(streamId, length, (byte) 0x00, false, FrameDirection.WRITE);
        this.dataBuffer = data;
        this.PADDED_FLAG = false;
        this.END_STREAM_FLAG = endStream;
        frameType = FrameTypes.DATA;
        if (data != null) {
            writeFrameLength += data.remaining();
        }
        setInitialized(); // we should have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // +---------------+
        // |Pad Length? (8)| (only present if third flag bit is set)
        // +---------------+-----------------------------------------------+
        // |                            Data (*)                         ...
        // +---------------------------------------------------------------+
        // |                           Padding (*)                       ...
        // +---------------------------------------------------------------+

        setFlags();

        int payloadIndex = 0;
        try {
            if (PADDED_FLAG) {
                // The padded field is present; set the paddedLength to represent the actual size of the data we want
                paddingLength = frp.grabNextByte() & 0xFF; // grab byte and convert to int
                payloadIndex++;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "PADDED_FLAG on. paddedLength = " + (paddingLength));
                }
            }

            // grab all the buffered data
            data = new byte[payloadLength - paddingLength];
            for (int i = 0; payloadIndex++ < payloadLength - paddingLength; i++) {
                data[i] = (frp.grabNextByte());
            }

            // read the padding bytes; don't do anything with them
            for (int i = 0; i < paddingLength; i++) {
                frp.grabNextByte();
            }
            setInitialized();
        } catch (FrameSizeException e) {
            e.setConnectionError(false);
            throw e;
        }
    }

    /**
     * The test code expects a single buffer instead of an array of buffers, as returned by buildFrameArrayForWrite
     */
    @Override
    public WsByteBuffer buildFrameForWrite() {

        WsByteBuffer[] output = buildFrameArrayForWrite();
        int size = 0;
        for (WsByteBuffer b : output) {
            if (b != null) {
                size += b.remaining();
            }
        }
        WsByteBuffer singleBuffer = this.getBuffer(size);
        singleBuffer.put(output);
        singleBuffer.flip();
        return singleBuffer;
    }

    /**
     * Builds an array of buffers representing this http2 data frame
     * output[0] = http2 frame header data
     * output[1] = payload data
     * output[2] ?= padding
     *
     * @return WsByteBuffer[]
     */
    public WsByteBuffer[] buildFrameArrayForWrite() {
        WsByteBuffer[] output;

        int headerSize = SIZE_FRAME_BEFORE_PAYLOAD;
        if (PADDED_FLAG) {
            headerSize = SIZE_FRAME_BEFORE_PAYLOAD + 1;
        }
        WsByteBuffer frameHeaders = getBuffer(headerSize);
        byte[] frame;
        if (frameHeaders.hasArray()) {
            frame = frameHeaders.array();
        } else {
            frame = super.createFrameArray();
        }

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_DATA);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        // add pad length field
        if (PADDED_FLAG) {
            utils.Move8BitstoByteArray(paddingLength, frame, frameIndex);
            frameIndex++;
        }
        frameHeaders.put(frame, 0, headerSize);
        frameHeaders.flip();

        // create padding and put in return buffer array
        if (PADDED_FLAG) {
            WsByteBuffer padding = getBuffer(paddingLength);
            for (int i = 0; i < paddingLength; i++) {
                padding.put((byte) 0);
            }
            padding.flip();
            output = new WsByteBuffer[3];
            output[0] = frameHeaders;
            output[1] = dataBuffer;
            output[2] = padding;
        }
        // create the output buffer array
        else {
            output = new WsByteBuffer[2];
            output[0] = frameHeaders;
            output[1] = dataBuffer;
        }
        return output;
    }

    @Override
    protected void setFlags() {
        END_STREAM_FLAG = utils.getFlag(flags, 0);
        PADDED_FLAG = utils.getFlag(flags, 3);
    }

    public int getPaddingLength() {
        return paddingLength;
    }

    public byte[] getData() {
        return (initialized) ? this.data : null;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException, FrameSizeException {
        if (streamId == 0) {
            throw new ProtocolException("DATA frame stream ID cannot be 0x0");
        }
        if (getPayloadLength() > settings.getMaxFrameSize()) {
            throw new FrameSizeException("DATA payload greater than allowed by the max frame size");
        }
        if (payloadLength > 0 && paddingLength > 0 && paddingLength >= payloadLength) {
            throw new ProtocolException("DATA padding length must be less than the length of the total payload");
        }
        if (paddingLength < 0) {
            throw new ProtocolException("DATA padding length is invalid");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameData))
            return false;

        FrameData frameDataToCompare = (FrameData) object;

        if (!subSuperEquals(frameDataToCompare))
            return false;

        if (this.getPaddingLength() != frameDataToCompare.getPaddingLength()) {
            return false;
        }
        if (!Arrays.equals(this.getData(), frameDataToCompare.getData())) {
            return false;
        }

        return true;
    }

    public boolean subSuperEquals(Object object) {
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
        // For DATA frames don't compare end of stream, since that is timing dependent for some tests.
        //if (this.flagEndStreamSet() != frameToCompare.flagEndStreamSet()) {
        //    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        //        Tr.debug(tc, "this.flagEndStreamSet() = " + this.flagEndStreamSet() + " frameToCompare.flagEndStreamSet() = " + frameToCompare.flagEndStreamSet());
        //    }
        //    return false;
        //}
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
        if ((this.getPayloadLength() != 0) && (this.getPayloadLength() != frameToCompare.getPayloadLength())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getPayloadLength() = " + this.getPayloadLength() + " frameToCompare.getPayloadLength() = " + frameToCompare.getPayloadLength());
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

        frameToString.append(super.toString());

        frameToString.append("PaddingLength: " + this.getPaddingLength() + "\n");

        // don't toString user data payload, unless we turn this on for some special debug
        /*
         * byte[] ba = this.getData();
         * if (ba == null) {
         * frameToString.append("Data: null");
         * } else {
         * int length = ba.length;
         * if (length > 512) {
         * length = 512;
         * }
         * StringBuffer sbuf = printCharArrayWithHex(ba, length);
         * frameToString.append(sbuf.toString());
         * }
         */

        return frameToString.toString();
    }

    // don't toString user data payload, unless we turn this on for some special debug
    /*
     * public StringBuffer printCharArrayWithHex(byte[] x, int length) {
     * StringBuffer sb = new StringBuffer("Data: (up to first 512 btytes):\n");
     * byte b;
     * int count = 0;
     * for (int i = 0; i < length; i++) {
     * count++;
     * b = x[i];
     * char c = (char) b;
     * if (((count % 64) == 0) && (b != 0x0A)) {
     * if (b == 0x0D) {
     * sb.append("<CR>");
     * count = 0;
     * } else if ((b > 0) && (b < 127)) {
     * sb.append(c);
     * count = 0;
     * } else {
     * sb.append(String.format("<0x%02X>", b) + " ");
     * count = 0;
     * }
     * } else {
     * if (b == 0x0A) {
     * sb.append("\n<LF>");
     * count = 0;
     * } else if (b == 0x0D) {
     * sb.append("<CR>");
     * } else if ((b > 0) && (b < 127)) {
     * sb.append(c);
     * } else {
     * sb.append(String.format("<0x%02X>", b) + " ");
     * }
     * }
     * }
     * return sb;
     * }
     */
}
