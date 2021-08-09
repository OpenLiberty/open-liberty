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

public class FrameReadProcessor {

    private static final TraceComponent tc = Tr.register(FrameReadProcessor.class);

    /** starting size of the pending buffer array */
    private static final int BUFFER_ARRAY_INITIAL_SIZE = 10;
    /** starting minimum growth size of the pending buffer array */
    private static final int BUFFER_ARRAY_GROWTH_SIZE = 10;

    // frame fields bit masks
    private static final byte FIN_MASK = (byte) 0x80;
    // private static final byte FIN_SHIFT = 7;
    private static final byte RSV_MASK = (byte) 0x70;
    private static final byte RSV_SHIFT = 4;
    private static final byte OPCODE_MASK = (byte) 0x0F;
    private static final byte MASK_FLAG_MASK = (byte) 0x80;
    // private static final byte MASK_FLAG_SHIFT = 7;
    private static final byte FIRST_PAYLOAD_LEN_MASK = (byte) 0x7F;

    // opcode values
    private static final byte OPCODE_TEXT_DATA = 0x01;
    private static final byte OPCODE_BINARY_DATA = 0x02;
    private static final byte OPCODE_CLOSE = 0x08;
    private static final byte OPCODE_PING = 0x09;
    private static final byte OPCODE_PONG = 0x0A;

    // field field sizes    	                               
    private static final int SIZE_INIT = 2;
    private static final int SIZE_16BIT_PAYLOAD_LENGTH = 2;
    private static final int SIZE_64BIT_PAYLOAD_LENGTH = 8;
    private static final int SIZE_MASK = 4;

    private byte fin;
    private byte rsv;
    private byte opcode;
    private byte maskFlag;
    private int payloadLength7bit;
    private int mask;
    private final byte[] maskArray = new byte[4];
    private long payloadLength;

    FrameState frameState = FrameState.INIT;

    int currentBufferArrayIndex = -1; // marks the current buffer that we have processed into.
    int countOfBuffers = 0; // counts the number of buffer we have added in the buffer array for processing.

    byte controlByte1 = 0;
    byte controlByte2 = 0;

    boolean binaryData = false;
    boolean textData = false;
    boolean controlFrame = false;
    OpcodeType controlOpcodeType;

    boolean shouldReadMaskedData = false;

    private WsByteBuffer[] frameBuffers = new WsByteBuffer[BUFFER_ARRAY_INITIAL_SIZE];

    public FrameReadProcessor() {}

    public void initialize(boolean shouldReadMaskedData) {
        this.shouldReadMaskedData = shouldReadMaskedData;
    }

    public void reset(boolean releaseBuffers) {
        if (releaseBuffers) {
            releaseBuffers();
        }

        // set other objects/variables back to their initial values
        frameState = FrameState.INIT;
        currentBufferArrayIndex = -1;
        countOfBuffers = 0;
        controlByte1 = 0;
        controlByte2 = 0;
        frameBuffers = new WsByteBuffer[BUFFER_ARRAY_INITIAL_SIZE];

    }

    public synchronized void releaseBuffers() {
        // release all the buffers that have been stored away.
        for (int i = 0; i < countOfBuffers; i++) {
            if (frameBuffers[i] != null) {
                frameBuffers[i].release();
                frameBuffers[i] = null;
            }
        }

        // set countOfBuffers to 0, to prevent easy double-releasing
        countOfBuffers = 0;

    }

    public int processNextBuffer(@Sensitive WsByteBuffer buf) throws FrameFormatException, WsocBufferException {

        // Return code has a double meaning, depending if it is negative or not, "ill-behaved" or "elegant", depending on your point of view.
        // return  0 or greater if the frame ended midway in the last buffer, 
        //           with the return value being the relative position where the next frame should start.
        // return Contants.BP_FRAME_ALREADY_COMPLETE (-3) if the frame was already complete
        // return Contants.BP_FRAME_IS_NOT_COMPLETE (-2) if frame is not complete
        // return Contants.BP_FRAME_EXACTLY_COMPLETED (-1) if frame is complete at the exact end of this buffer

        // the Position value of buffers will be moved forward when the non-payload data, at the beginning of a frame, is processed, but
        // the Position value of buffers will NOT be moved when processing the payload data.  Therefore, at the very end of processing the entire
        // frame the position values will only point to the payload data.

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Entered processNextBuffer with frameState of:  " + frameState);
        }

        if (frameState == FrameState.PAYLOAD_COMPLETE) {
            return Constants.BP_FRAME_ALREADY_COMPLETE;
        }

        // grow the array if needed
        if (countOfBuffers >= frameBuffers.length) {
            // need to grow the array
            int originalSize = frameBuffers.length;
            WsByteBuffer[] temp = new WsByteBuffer[originalSize + BUFFER_ARRAY_GROWTH_SIZE];
            System.arraycopy(frameBuffers, 0, temp, 0, originalSize); // parames are source, dest, size
            frameBuffers = temp;
        }

        // Special Debug Utils.printOutBuffer(buf);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "adding buf at index:  " + countOfBuffers);
        }
        frameBuffers[countOfBuffers] = buf;
        countOfBuffers++;

        if (frameState == FrameState.INIT) {
            long bytesToProcess = bytesRemaining();

            // Need at least the a few bytes of the frame to start processing it 
            if (bytesToProcess < SIZE_INIT) {
                return Constants.BP_FRAME_IS_NOT_COMPLETE;
            }

            currentBufferArrayIndex = 0;

            // read in first two bytes and set control values.
            controlByte1 = grabNextByte();

            fin = (byte) (controlByte1 & FIN_MASK);
            if (fin != 0) {
                fin = 1;
            }
            // byte masks and shifts with the the sign bit is proving frustrating, the below doesn't work. 
            // fin = (byte) ((byte) (controlByte1 & FIN_MASK) >>> FIN_SHIFT); 

            rsv = (byte) ((byte) (controlByte1 & RSV_MASK) >>> RSV_SHIFT);
            if (rsv != 0) {
                throw new FrameFormatException("Reserved frame must be 0.");
            }
            opcode = (byte) (controlByte1 & OPCODE_MASK);

            // first frame opcode tells if this is binary or text data
            if (opcode == OPCODE_TEXT_DATA) {
                textData = true;
            } else if (opcode == OPCODE_BINARY_DATA) {
                binaryData = true;
            } else if (opcode == OPCODE_PING) {
                controlFrame = true;
                controlOpcodeType = OpcodeType.PING;
            } else if (opcode == OPCODE_PONG) {
                controlFrame = true;
                controlOpcodeType = OpcodeType.PONG;
            } else if (opcode == OPCODE_CLOSE) {
                controlFrame = true;
                controlOpcodeType = OpcodeType.CONNECTION_CLOSE;
            }

            if (controlFrame && (fin == 0)) {
                throw new FrameFormatException("Control frames must have FIN bit set to 1.");
            }

            controlByte2 = grabNextByte();

            // maskFlag = (byte) ((byte) (controlByte2 & MASK_FLAG_MASK) >>> MASK_FLAG_SHIFT);
            maskFlag = (byte) (controlByte2 & MASK_FLAG_MASK);
            if (maskFlag != 0) {
                maskFlag = 1;
            }

            payloadLength7bit = controlByte2 & FIRST_PAYLOAD_LEN_MASK;

            if (payloadLength7bit == 126) {
                frameState = FrameState.FIND_16BIT_PAYLOAD_LENGTH;
            } else if (payloadLength7bit == 127) {
                frameState = FrameState.FIND_64BIT_PAYLOAD_LENGTH;
            } else {
                payloadLength = payloadLength7bit;
                frameState = FrameState.FIND_MASK;
            }

            if ((payloadLength7bit >= 126) && controlFrame) {
                FrameFormatException e = new FrameFormatException("Control frame must have payload length less than 126 bytes");
                throw e;
            }
        }

        if (frameState == FrameState.FIND_16BIT_PAYLOAD_LENGTH) {
            long bytesToProcess = bytesRemaining();

            if (bytesToProcess < SIZE_16BIT_PAYLOAD_LENGTH) {
                return Constants.BP_FRAME_IS_NOT_COMPLETE;
            }

            payloadLength = grabNext16BitInt();
            frameState = FrameState.FIND_MASK;
        }

        if (frameState == FrameState.FIND_64BIT_PAYLOAD_LENGTH) {
            long bytesToProcess = bytesRemaining();

            if (bytesToProcess < SIZE_64BIT_PAYLOAD_LENGTH) {
                return Constants.BP_FRAME_IS_NOT_COMPLETE;
            }
            payloadLength = grabNext64BitLong();
            frameState = FrameState.FIND_MASK;
        }

        if (frameState == FrameState.FIND_MASK) {
            if (shouldReadMaskedData) {
                long bytesToProcess = bytesRemaining();

                if (bytesToProcess < SIZE_MASK) {
                    return Constants.BP_FRAME_IS_NOT_COMPLETE;
                }
                mask = grabNext32BitInt();

                maskArray[0] = (byte) ((mask >> 24) & 0x000000ff);
                maskArray[1] = (byte) ((mask >> 16) & 0x000000ff);
                maskArray[2] = (byte) ((mask >> 8) & 0x000000ff);
                maskArray[3] = (byte) ((mask) & 0x000000ff);
            }
            frameState = FrameState.FIND_PAYLOAD;
        }

        if (frameState == FrameState.FIND_PAYLOAD) {
            // we don't want to move the position pointers of the WsByteBuffers for the payload data.  For now, assuming there will not be a lot of
            // WsByteBuffers per frame, then simpliest way to determine current payload length is to look at all the buffers each time
            long count = 0;
            int remaining = 0;
            for (int i = 0; i < countOfBuffers; i++) {
                remaining = frameBuffers[i].remaining();
                if (count + remaining <= payloadLength) {
                    count += remaining;
                    // check if count now ends at the end of this buffer also, and return -1 if it does.
                    if (count == payloadLength) {
                        frameState = FrameState.PAYLOAD_COMPLETE;
                        return Constants.BP_FRAME_EXACTLY_COMPLETED;
                    }
                } else {
                    // found end of payloadLength inside last buffer.  Return position where next message is assumed to start.
                    int nextPosition = frameBuffers[i].position() + ((int) (payloadLength - count));
                    frameState = FrameState.PAYLOAD_COMPLETE;
                    return nextPosition;
                }
            }

            // indicate that we have not read in all the payload data
            return Constants.BP_FRAME_IS_NOT_COMPLETE;

        }

        // if we made it down here, then something about this frame (or this code!) is wrong
        FrameFormatException e = new FrameFormatException("Frame was not processed correctly");
        throw e;
    }

    public byte getFin() {
        return fin;
    }

    public byte getRsv() {
        return rsv;
    }

    public byte getOpcode() {
        return opcode;
    }

    public byte getMaskFlag() {
        return maskFlag;
    }

    public int getMask() {
        return mask;
    }

    @Sensitive
    public byte[] getMaskArray() {
        return maskArray;
    }

    public long getPayloadLength() {
        return payloadLength;
    }

    @Sensitive
    public WsByteBuffer[] getFrameBuffers() {
        return frameBuffers;
    }

    public int getFrameBufferListSize() {
        return countOfBuffers;
    }

    @Sensitive
    public WsByteBuffer getBufferAtIndex(int index) {
        return frameBuffers[index];
    }

    public FrameState getFrameState() {
        return frameState;
    }

    public boolean getControlFrame() {
        return controlFrame;
    }

    public OpcodeType getControlOpcodeType() {
        return controlOpcodeType;
    }

    public void unmaskPayload() {

        Utils.maskPayload(getMaskArray(), frameBuffers, countOfBuffers);
    }

    // ---------- separator of public and private methods ----------

    private int grabNext16BitInt() throws WsocBufferException {
        int value = 0;
        byte b1;
        byte b2;

        // Network order data should be Big Endian, so first byte read is most significant.  Java is also Big Endian, so...
        b1 = this.grabNextByte();
        b2 = this.grabNextByte();
        // not sure one needs the "&" below - but put it in for safety given the typecasting
        value = ((b1) & 0x000000ff) << 8 | ((b2) & 0x000000ff);

        return value;
    }

    private int grabNext32BitInt() throws WsocBufferException {
        int value = 0;
        byte b;

        // Network order data should be Big Endian, so first byte read is most significant.  Java is also Big Endian, so...
        for (int i = 3; i >= 0; i--) {
            b = this.grabNextByte();
            // not sure one needs the "&" below - but put it in for safety given the typecasting
            value = value | ((b) & 0x000000ff) << (i * 8);
        }
        return value;
    }

    public long grabNext64BitLong() throws WsocBufferException {
        long value = 0;
        byte b;

        // Network order data should be Big Endian, so first byte read is most significant.  Java is also Big Endian, so...
        for (int i = 7; i >= 0; i--) {
            b = this.grabNextByte();
            // not sure one needs the "&" below - but put it in for safety given the typecasting
            value = value | (((long) b) & 0x00000000000000ff) << (i * 8);
        }
        return value;
    }

    public long bytesRemaining() {
        // return with the number of bytes remaining for all buffers combined
        long count = 0;
        for (int i = 0; i < countOfBuffers; i++) {
            count += frameBuffers[i].remaining();
        }

        return count;
    }

    private byte grabNextByte() throws WsocBufferException {

        byte b;
        int startArrayIndex;

        // first see if we can get this quickly and return
        if (frameBuffers[currentBufferArrayIndex].hasRemaining()) {
            return frameBuffers[currentBufferArrayIndex].get();
        }

        // current array is used up, so move on to the next array		
        startArrayIndex = currentBufferArrayIndex + 1;

        int i; // might need i after the loop
        for (i = startArrayIndex; i < countOfBuffers; i++) {

            if (frameBuffers[i].hasRemaining()) {
                b = frameBuffers[i].get();
                currentBufferArrayIndex = i;
                return b;
            }
        }

        // no more bytes remaining.  can't return a value, so will need to throw an exception. 
        // first update the current index into the array
        currentBufferArrayIndex = i;

        String msg = Tr.formatMessage(tc,
                                      "bytes.notavailable",
                                      null);
        WsocBufferException e = new WsocBufferException(msg);
        throw e;

    }
}
