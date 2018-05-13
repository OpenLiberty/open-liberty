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

import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FrameContinuation extends Frame {

    /*
     * format of Continuation frame
     *
     * length(24) XX XX XX - length of payload
     * type(8) 09 - Continuation
     * flags(8) bits: 0000 0x00 - END_HEADERS (0x4)
     * R (1) 0
     * Stream (31) xx xx xx xx
     * Payload
     * Header Block Fragment (*)
     */

    public byte[] headerBlockFragment = null;

    /**
     * Read frame constructor
     */
    public FrameContinuation(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.CONTINUATION;
    }

    /**
     * Write frame constructor
     */
    public FrameContinuation(int streamId, byte[] headerBlockFragment, boolean endHeaders, boolean endStream, boolean reserveBit) {
        super(streamId, 0, (byte) 0x00, reserveBit, FrameDirection.WRITE);
        if (headerBlockFragment != null) {
            payloadLength += headerBlockFragment.length;
        }
        this.headerBlockFragment = headerBlockFragment;
        this.END_HEADERS_FLAG = endHeaders;
        this.END_STREAM_FLAG = endStream;
        frameType = FrameTypes.CONTINUATION;
        writeFrameLength += payloadLength;
        setInitialized(); // we have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // +---------------------------------------------------------------+
        // |                   Header Block Fragment (*)                 ...
        // +---------------------------------------------------------------+

        // grab all the buffered data
        headerBlockFragment = new byte[payloadLength];

        int payloadIndex = 0;

        for (int i = 0; payloadIndex++ < payloadLength; i++)
            headerBlockFragment[i] = frp.grabNextByte();
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
        setFrameHeaders(frame, utils.FRAME_TYPE_CONTINUATION);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        // copy over header block fragment data
        for (int i = 0; i < headerBlockFragment.length; i++) {
            frame[frameIndex] = headerBlockFragment[i];
            frameIndex++;
        }
        buffer.put(frame, 0, writeFrameLength);
        buffer.flip();
        return buffer;
    }

    public byte[] getHeaderBlockFragment() {
        return this.headerBlockFragment;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws ProtocolException, FrameSizeException {
        if (streamId == 0) {
            throw new ProtocolException("CONTINUATION frame streamID cannot be 0x0");
        }
        if (this.getPayloadLength() > settings.getMaxFrameSize()) {
            throw new FrameSizeException("CONTINUATION payload greater than max allowed");
        }
    }

    @Override
    protected void setFlags() {
        END_HEADERS_FLAG = utils.getFlag(flags, 2);
    }
}
