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

import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FramePushPromise extends Frame {

    /*
     * format of PushPromise frame
     *
     * length(24) XX XX XX - length of payload
     * type(8) 05 - PUSH_PROMISE
     * flags(8) bits: 0000 xx00 - END_HEADERS (0x4), PADDED (0x8)
     * R (1) 0
     * Stream (31) xx xx xx xx
     * Payload
     * Pad Length? (8)
     * Reserve (1) X
     * Promised Stream ID (31) xx xx xx xx
     * Header Block Fragment (*)
     * Padding (*)
     */

    int promisedStreamID = 0;
    public byte[] headerBlockFragment = null;
    int paddingLength = 0;

    /**
     * Read frame constructor
     */
    public FramePushPromise(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.PUSH_PROMISE;
    }

    /**
     * Write frame constructor
     */
    public FramePushPromise(int streamId, byte[] headerBlockFragment, int promisedStream, int paddingLength,
                            boolean endHeaders, boolean padded, boolean reserveBit) {
        super(streamId, headerBlockFragment.length + 4, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        this.headerBlockFragment = headerBlockFragment;
        this.paddingLength = paddingLength;
        this.PADDED_FLAG = padded;
        this.END_HEADERS_FLAG = endHeaders;
        this.reservedBit = reserveBit;
        this.promisedStreamID = promisedStream;

        // payload length is HBF size + padding + pad length + promised stream id
        if (padded) {
            payloadLength += paddingLength + 1;
        }
        frameType = FrameTypes.PUSH_PROMISE;
        writeFrameLength += payloadLength;
        setInitialized(); // we have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // +---------------+
        // |Pad Length? (8)| (only present if third flag bit is set)
        // +---------------+-----------------------------------------------+
        // |R|                  Promised Stream ID (31)                    |
        // +-+-----------------------------+-------------------------------+
        // |                   Header Block Fragment (*)                 ...
        // +---------------------------------------------------------------+
        // |                           Padding (*)                       ...
        // +---------------------------------------------------------------+

        setFlags();

        int payloadIndex = 0;
        int paddingLength = 0;
        if (PADDED_FLAG) {
            // The padded field is present; set the paddedLength to represent the actual size of the data we want
            paddingLength = frp.grabNextByte();
            payloadIndex++;
        }

        // grab the reserved bit
        byte nextPayloadByte = frp.grabNextByte();
        payloadIndex++;
        reservedBit = utils.getReservedBit(nextPayloadByte);

        // grab the promised stream ID
        nextPayloadByte = (byte) (nextPayloadByte & Constants.MASK_7F);
        promisedStreamID = frp.grabNext24BitInt(nextPayloadByte);
        payloadIndex += 3;

        // grab the header block fragment
        int headerBlockLength = payloadLength - payloadIndex - paddingLength;
        headerBlockFragment = new byte[headerBlockLength];
        for (int i = 0; payloadIndex++ < payloadLength - paddingLength;)
            headerBlockFragment[i++] = frp.grabNextByte();

        // consume any padding for this frame
        if (PADDED_FLAG) {
            for (; payloadIndex++ < payloadLength;) {
                frp.grabNextByte();
            }
        }
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
        setFrameHeaders(frame, utils.FRAME_TYPE_PUSH_PROMISE);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        if (PADDED_FLAG) {
            utils.Move8BitstoByteArray(paddingLength, frame, frameIndex);
            frameIndex += 1;
        }

        utils.Move31BitstoByteArray(promisedStreamID, frame, frameIndex);
        frameIndex += 4;

        for (int i = 0; i < headerBlockFragment.length; i++) {
            frame[frameIndex] = headerBlockFragment[i];
            frameIndex++;
        }

        for (int i = 0; i < paddingLength; i++) {
            frame[frameIndex] = 0x00;
            frameIndex++;
        }

        buffer.put(frame, 0, writeFrameLength);
        buffer.flip();
        return buffer;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException, FrameSizeException {
        if (streamId == 0) {
            throw new ProtocolException("PUSH_PROMISE Frame stream ID cannot be 0x0");
        }
        if (this.getPayloadLength() > settings.getMaxFrameSize()) {
            throw new FrameSizeException("PUSH_PROMISE payload greater than max allowed");
        }
        if (this.paddingLength >= this.payloadLength) {
            throw new ProtocolException("PUSH_PROMISE padding length must be less than the length of the payload");
        }
    }

    @Override
    protected void setFlags() {
        END_HEADERS_FLAG = utils.getFlag(flags, 2);
        PADDED_FLAG = utils.getFlag(flags, 3);
    }

    public int getPromisedStreamId() {
        return this.promisedStreamID;
    }

    public int getPaddingLength() {
        return this.paddingLength;
    }

    public byte[] getHeaderBlockFragment() {
        return this.headerBlockFragment;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("FrameReserveBit: ").append(this.getFrameReserveBit()).append("\n");
        frameToString.append("PaddingLength: ").append(this.getPaddingLength()).append("\n");
        frameToString.append("PromisedStreamID: ").append(this.getPromisedStreamId()).append("\n");

        return frameToString.toString();
    }

}
