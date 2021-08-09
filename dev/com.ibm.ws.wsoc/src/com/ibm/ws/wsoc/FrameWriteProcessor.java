/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;

public class FrameWriteProcessor {

    private static final TraceComponent tc = Tr.register(FrameWriteProcessor.class);

    private WsByteBuffer[] frameBuffers = null;
    private long frameLength = 0;

    private int payloadLength7bit;
    private long payloadLength;

    private byte controlByte1 = 0;
    private byte controlByte2 = 0;

    private final static int SIZE_FORMAT_64_NO_MASK = 10;
    private final static int SIZE_FORMAT_16_NO_MASK = 4;
    private final static int SIZE_FORMAT_8_NO_MASK = 2;

    private final WebSocketContainerManager wcManager = WebSocketContainerManager.getRef();

    private WsByteBuffer formatBuffer = null;

    private final Object safetySync = new Object() {};

    @Sensitive
    public WsByteBuffer[] formatForFrameMessage(@Sensitive WsByteBuffer[] bufs, OpcodeType op, boolean shouldMaskData) {

        if (bufs == null) {
            return null;
        }

        // build controlByte1
        // set to fin = 1 if this is last (or only) frame to write for this message, rsv = 0.

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "writing frame with opcode of: " + op);
        }

        switch (op) {
            case TEXT_WHOLE:
                controlByte1 = (byte) 0x81;
                break;

            case TEXT_PARTIAL_FIRST:
                controlByte1 = (byte) 0x01;
                break;

            case TEXT_PARTIAL_CONTINUATION:
            case BINARY_PARTIAL_CONTINUATION:
                controlByte1 = (byte) 0x00;
                break;

            case TEXT_PARTIAL_LAST:
            case BINARY_PARTIAL_LAST:
                controlByte1 = (byte) 0x80;
                break;

            case BINARY_WHOLE:
                controlByte1 = (byte) 0x82;
                break;

            case BINARY_PARTIAL_FIRST:
                controlByte1 = (byte) 0x02;
                break;

            case PING:
                controlByte1 = (byte) 0x89;
                break;

            case PONG:
                controlByte1 = (byte) 0x8A;
                break;

            case CONNECTION_CLOSE:
                controlByte1 = (byte) 0x88;
                break;

            default:
                break;
        }

        // build controlByte2
        payloadLength = bytesRemaining(bufs);

        if (payloadLength < 126) {
            payloadLength7bit = (int) payloadLength;
        } else if ((payloadLength >> 16) == 0) {
            // top 48 bits are 0, so dataLength can be represented using 16 bits
            payloadLength7bit = 126;
        } else {
            // need to use 64 bit to represent message length
            payloadLength7bit = 127;
        }

        // for now assume this is only a serverSide API, so mask is always 0
        controlByte2 = (byte) payloadLength7bit;
        if (shouldMaskData) {
            controlByte2 = (byte) (controlByte2 ^ (1 << 7));
        }

        int maskSize = 0;
        if (shouldMaskData) {
            maskSize = 4;
        }
        WsByteBufferPoolManager mgr = getBufferManager();
        if (payloadLength7bit == 127) {
            // 64 bit payload length size
            formatBuffer = mgr.allocate(SIZE_FORMAT_64_NO_MASK + maskSize);
            frameLength = payloadLength + SIZE_FORMAT_64_NO_MASK + maskSize;
        } else if (payloadLength7bit == 126) {
            // 16 bit payload length size
            formatBuffer = mgr.allocate(SIZE_FORMAT_16_NO_MASK + maskSize);
            frameLength = payloadLength + SIZE_FORMAT_16_NO_MASK + maskSize;
        } else {
            // 8 bit payload length size
            formatBuffer = mgr.allocate(SIZE_FORMAT_8_NO_MASK + maskSize);
            frameLength = payloadLength + SIZE_FORMAT_8_NO_MASK + maskSize;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frameLength set to: " + frameLength);
        }

        // put the bytes into the buffer
        formatBuffer.put(controlByte1);
        formatBuffer.put(controlByte2);

        if (payloadLength7bit == 127) {
            formatBuffer.putLong(payloadLength);
        } else if (payloadLength7bit == 126) {
            int temp = (int) payloadLength;
            byte byte1 = (byte) ((temp >> 8) & (0x000000FF));
            byte byte2 = (byte) (temp & 0x000000FF);
            formatBuffer.put(byte1);
            formatBuffer.put(byte2);
        }

        byte[] maskBytes = null;
        if (shouldMaskData) {
            maskBytes = wcManager.generateNewMaskKey();
            formatBuffer.put(maskBytes);
        }

        // set position back to start, for writing out the data in the next step
        formatBuffer.position(0);

        // create the frame buffer array the contains the frame format bytes and the payload
        int bufsCount = bufs.length;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "frame buf count is: " + bufsCount);
        }
        frameBuffers = new WsByteBuffer[bufs.length + 1];
        frameBuffers[0] = formatBuffer;
        if (shouldMaskData) {
            Utils.maskPayload(maskBytes, bufs, bufsCount);
        }
        for (int i = 0; i < bufsCount; i++) {
            frameBuffers[i + 1] = bufs[i];
        }

        // the return buffers should contain a complete message now
        return frameBuffers;

    }

    public long getFrameLength() {
        return frameLength;
    }

    private long bytesRemaining(@Sensitive WsByteBuffer[] bufs) {
        // return with the number of bytes remaining for all buffers combined
        long count = 0;
        int length = bufs.length;
        for (int i = 0; i < length; i++) {
            count += bufs[i].remaining();
        }

        return count;
    }

    private WsByteBufferPoolManager getBufferManager() {
        return ServiceManager.getBufferPoolManager();
    }

    public void cleanup() {
        // cleanup anything we allocated
        // no reason to check the buffer for null if we are not going to put it in a sync block also
        synchronized (safetySync) {
            if (formatBuffer != null) {
                formatBuffer.release();
                formatBuffer = null;
            }
        }
    }

}
