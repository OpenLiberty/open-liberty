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
        this.paddingLength = paddingLength;
        this.PADDED_FLAG = padded;
        this.END_STREAM_FLAG = endStream;
        if (padded) {
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
        this.PADDED_FLAG = false;
        this.END_STREAM_FLAG = endStream;
        frameType = FrameTypes.DATA;
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

        int paddedLength = payloadLength;

        setFlags();

        int payloadIndex = 0;
        try {
            if (PADDED_FLAG) {
                // The padded field is present; set the paddedLength to represent the actual size of the data we want
                paddedLength -= frp.grabNextByte();
                payloadIndex++;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "PADDED_FLAG on. paddedLength = " + paddedLength);
                }
            }

            // grab all the buffered data
            data = new byte[paddedLength];

            for (int i = 0; payloadIndex++ < paddedLength; i++) {
                data[i] = (frp.grabNextByte());
            }

            setInitialized();
        } catch (FrameSizeException e) {
            e.setConnectionError(false);
            throw e;
        }
    }

    @Override
    public byte[] buildFrameForWrite() {
        byte[] frame = super.buildFrameForWrite();

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_DATA);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        // add pad length field
        if (PADDED_FLAG) {
            utils.Move8BitstoByteArray(paddingLength, frame, frameIndex);
            frameIndex++;
        }

        // copy data payload
        for (int i = 0; i < data.length; i++) {
            frame[frameIndex] = data[i];
            frameIndex++;
        }

        // copy padding
        for (int i = 0; i < paddingLength; i++) {
            frame[frameIndex] = 0x00;
            frameIndex++;
        }
        return frame;
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
        if (this.getPayloadLength() > settings.maxFrameSize) {
            throw new FrameSizeException("DATA payload greater than allowed by the max frame size");
        }
        if (this.paddingLength > this.payloadLength) {
            throw new ProtocolException("DATA padding length must be less than the length of the payload");
        }
        if (this.paddingLength < 0) {
            throw new ProtocolException("DATA padding length is invalid");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameData))
            return false;

        FrameData frameDataToCompare = (FrameData) object;

        if (!super.equals(frameDataToCompare))
            return false;

        if (this.getPaddingLength() != frameDataToCompare.getPaddingLength()) {
            return false;
        }
        if (!Arrays.equals(this.getData(), frameDataToCompare.getData())) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("PaddingLength: " + this.getPaddingLength() + "\n");

        byte[] ba = this.getData();
        if (ba == null) {
            frameToString.append("Data: null");
        } else {
            int length = ba.length;
            if (length > 512) {
                length = 512;
            }
            StringBuffer sbuf = printCharArrayWithHex(ba, length);
            frameToString.append(sbuf.toString());
        }

        return frameToString.toString();
    }

    public StringBuffer printCharArrayWithHex(byte[] x, int length) {
        StringBuffer sb = new StringBuffer("Data: (up to first 512 btytes):\n");
        byte b;
        int count = 0;
        for (int i = 0; i < length; i++) {
            count++;
            b = x[i];
            char c = (char) b;
            if (((count % 64) == 0) && (b != 0x0A)) {
                if (b == 0x0D) {
                    sb.append("<CR>");
                    count = 0;
                } else if ((b > 0) && (b < 127)) {
                    sb.append(c);
                    count = 0;
                } else {
                    sb.append(String.format("<0x%02X>", b) + " ");
                    count = 0;
                }
            } else {
                if (b == 0x0A) {
                    sb.append("\n<LF>");
                    count = 0;
                } else if (b == 0x0D) {
                    sb.append("<CR>");
                } else if ((b > 0) && (b < 127)) {
                    sb.append(c);
                } else {
                    sb.append(String.format("<0x%02X>", b) + " ");
                }
            }
        }
        return sb;
    }

}
