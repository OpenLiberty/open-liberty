/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.http.channel.h2internal.Constants.Direction;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.ws.http.channel.h2internal.frames.Frame;
import com.ibm.ws.http.channel.h2internal.frames.Frame.FrameDirection;
import com.ibm.ws.http.channel.h2internal.frames.FrameFactory;
import com.ibm.ws.http.channel.h2internal.frames.FrameRstStream;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FrameReadProcessor {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(FrameReadProcessor.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** starting size of the pending buffer array */
    private static final int BUFFER_ARRAY_INITIAL_SIZE = 10;
    /** starting minimum growth size of the pending buffer array */
    private static final int BUFFER_ARRAY_GROWTH_SIZE = 10;

    private FrameTypes enumFrameType = FrameTypes.NOT_SET;

    private byte byteFrameType;

    private Frame currentFrame;

    FrameState frameState = FrameState.INIT;

    int currentBufferArrayIndex = -1; // marks the current buffer that we have processed into.
    int countOfBuffers = 0; // counts the number of buffer we have added in the buffer array for processing.

    private WsByteBuffer[] frameBuffers = new WsByteBuffer[BUFFER_ARRAY_INITIAL_SIZE];

    H2InboundLink muxLink = null;

    public FrameReadProcessor(H2InboundLink m) {
        muxLink = m;
    }

    /**
     * Finish building the current frame: process its payload and pass it to the Stream Processor
     *
     * @throws ProtocolException
     *
     */
    public void processCompleteFrame() throws Http2Exception {
        Frame currentFrame = getCurrentFrame();

        boolean frameSizeError = false;
        try {
            currentFrame.processPayload(this);
        } catch (Http2Exception e) {
            // If we get an error here, it should be safe to assume that this frame doesn't have the expected byte count,
            // which must be treated as an error of type FRAME_SIZE_ERROR.  If we're processing a DATA or PRIORITY frame, then
            // we can treat the error as a stream error rather than a connection error.
            if (!e.isConnectionError()) {
                frameSizeError = true;
            } else {
                // this is a connection error; we need to send a GOAWAY on the connection
                throw e;
            }
        } catch (Exception e) {
            throw new ProtocolException("Error processing the payload for " + currentFrame.getFrameType()
                                        + " frame on stream " + currentFrame.getStreamId());
        }

        // call the stream processor to process this stream. For now, don't return from here until the
        // frame has been fully processed.
        int streamId = currentFrame.getStreamId();
        H2StreamProcessor stream = muxLink.getStream(streamId);

        if (stream == null) {
            if ((streamId != 0) && (streamId % 2 == 0)) {
                if (currentFrame.getFrameType().equals(FrameTypes.PRIORITY)) {
                    // ignore PRIORITY frames in any state
                    return;
                } else if (currentFrame.getFrameType().equals(FrameTypes.RST_STREAM) && streamId < muxLink.getHighestClientStreamId()) {
                    // tolerate RST_STREAM frames that are sent on closed push streams
                    return;
                } else {
                    throw new ProtocolException("Cannot start a stream from the client with an even numbered ID. stream-id: " + streamId);
                }
            } else {
                stream = startNewInboundSession(streamId);
            }
        }
        if (frameSizeError) {
            currentFrame = new FrameRstStream(streamId, 4, (byte) 0, false, FrameDirection.READ);
            ((FrameRstStream) currentFrame).setErrorCode(Constants.FRAME_SIZE_ERROR);
        }

        stream.processNextFrame(currentFrame, Direction.READ_IN);
    }

    // When the read processor figures out that there is a new http-request/http2-stream to start, it can call this method
    // with the new streamID, then call the new stream object with the information it needs to initialize and call the ready method
    // of the http1.1 HttpInboundLink.  HttpInboundLink is wrapped by the H2HttpInboundLinkWrap that the new stream has access to.
    public H2StreamProcessor startNewInboundSession(Integer streamID) {
        H2StreamProcessor h2s = null;
        h2s = muxLink.createNewInboundLink(streamID);
        return h2s;
        // call the stream processor with the data it needs to then call "ready" are the underlying HttpInboundLink
        // (what H2HttpInboundLinkWrap for the stream is wrapping)
        // h2s.startNewInboundLink(....);
    }

    public void reset(boolean releaseBuffers) {
        if (releaseBuffers) {
            releaseBuffers();
        }

        // set other objects/variables back to their initial values
        frameState = FrameState.INIT;
        currentBufferArrayIndex = -1;
        countOfBuffers = 0;
        frameBuffers = new WsByteBuffer[BUFFER_ARRAY_INITIAL_SIZE];
        enumFrameType = FrameTypes.NOT_SET;
        currentFrame = null;

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

    public int processNextBuffer(@Sensitive WsByteBuffer buf) throws ProtocolException, FrameSizeException {

        // Return code has a double meaning, depending if it is negative or not, "ill-behaved" or "elegant", depending on your point of view.
        // return  0 or greater if the frame ended midway in the last buffer,
        //           with the return value being the relative position where the next frame should start.
        // return Constants.BP_FRAME_ALREADY_COMPLETE (-3) if the frame was already complete
        // return Constants.BP_FRAME_IS_NOT_COMPLETE (-2) if frame is not complete
        // return Constants.BP_FRAME_EXACTLY_COMPLETED (-1) if frame is complete at the exact end of this buffer

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
            System.arraycopy(frameBuffers, 0, temp, 0, originalSize); // params are source, dest, size
            frameBuffers = temp;
        }

        // Special Debug Utils.printOutBuffer(buf);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "adding buf at index:  " + countOfBuffers);
        }
        frameBuffers[countOfBuffers] = buf;
        countOfBuffers++;

        // the client must always send the preface string "PRI *HTTP/2.0\r\n\r\nSM\r\n\r\n" at the
        // beginning of the H2 connection; we must process this string before any other frames.
        if (muxLink != null && muxLink.connection_preface_string_rcvd == false) {
            long bytesToProcess = bytesRemaining();
            if (bytesToProcess < 24) {
                return Constants.BP_FRAME_IS_NOT_COMPLETE;
            }
            currentBufferArrayIndex = 0;
            try {
                if (checkConnectionPreface()) {
                    frameState = FrameState.INIT;
                    muxLink.processConnectionPrefaceMagic();
                } else {
                    throw new ProtocolException("Connection preface/magic was invalid");
                }
            } catch (Http2Exception e) {
                throw new ProtocolException("Failed to complete the connection preface");
            }
        }

        if (frameState == FrameState.INIT) {
            byte frameSixthByte;
            long bytesToProcess = bytesRemaining();

            // Frame always starts with 9 bytes, so read them all in before processing
            if (bytesToProcess < Frame.SIZE_FRAME_BEFORE_PAYLOAD) {
                return Constants.BP_FRAME_IS_NOT_COMPLETE;
            }

            currentBufferArrayIndex = 0;

            int payloadLength = grabNext24BitInt();
            byteFrameType = grabNextByte();

            byte flags = grabNextByte();
            frameSixthByte = grabNextByte();

            int frameReserveBit = (byte) (frameSixthByte & Constants.MASK_80);
            // change value to 0 or 1 - not 0x00 or 0x80
            if (frameReserveBit != 0) {
                frameReserveBit = 1;
            }

            frameSixthByte = (byte) (frameSixthByte & Constants.MASK_7F);
            int streamId = new Integer(grabNext24BitInt(frameSixthByte));

            this.currentFrame = FrameFactory.getFrame(byteFrameType, streamId, payloadLength, flags, frameReserveBit == 1, Frame.FrameDirection.READ);
            if (this.currentFrame.getFrameType() == FrameTypes.UNKNOWN) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "ignoring a frame of unknown type");
                }
            }
            frameState = FrameState.FIND_PAYLOAD;
        }

        if (currentFrame != null && frameState == FrameState.FIND_PAYLOAD) {
            int returnValue = Constants.BP_FRAME_IS_NOT_COMPLETE;
            // we don't want to move the position pointers of the WsByteBuffers for the payload data.  For now, assuming there will not be a lot of
            // WsByteBuffers per frame, then simpliest way to determine current payload length is to look at all the buffers each time
            long count = 0;
            int remaining = 0;
            for (int i = 0; i < countOfBuffers; i++) {
                remaining = frameBuffers[i].remaining();
                if (count + remaining <= currentFrame.getPayloadLength()) {
                    count += remaining;
                    // check if count now ends at the end of this buffer also, and return -1 if it does.
                    if (count == currentFrame.getPayloadLength()) {
                        frameState = FrameState.PAYLOAD_COMPLETE;
                        returnValue = Constants.BP_FRAME_EXACTLY_COMPLETED;
                    }
                } else {
                    // found end of payloadLength inside last buffer.  Return position where next message is assumed to start.
                    int nextPosition = frameBuffers[i].position() + ((int) (currentFrame.getPayloadLength() - count));
                    frameState = FrameState.PAYLOAD_COMPLETE;
                    returnValue = nextPosition;
                }
            }

            return returnValue;
        }

        // if we made it down here, then something about this frame (or this code!) is wrong
        ProtocolException e = new ProtocolException("Frame was not processed correctly");
        throw e;
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

    public void setFrameState(FrameState state) {
        frameState = state;
    }

    public FrameState getFrameState() {
        return frameState;
    }

    public Constants.Direction getFrameDirection() {
        return Constants.Direction.READ_IN;
    }

    public long bytesRemaining() {
        // return with the number of bytes remaining for all buffers combined
        long count = 0;
        for (int i = 0; i < countOfBuffers; i++) {
            if (frameBuffers[i] != null)
                count += frameBuffers[i].remaining();
        }

        return count;
    }

    private int grabNext24BitInt() throws FrameSizeException {
        int value = 0;
        byte b;

        // Network order data should be Big Endian, so first byte read is most significant.  Java is also Big Endian, so...
        for (int i = 2; i >= 0; i--) {
            b = this.grabNextByte();
            // not sure one needs the "&" below - but put it in for safety given the typecasting
            value = value | ((b) & 0x000000ff) << (i * 8);
        }
        return value;
    }

    public int grabNext16BitInt() throws FrameSizeException {
        int value = 0;
        byte b;

        // Network order data should be Big Endian, so first byte read is most significant.  Java is also Big Endian, so...
        for (int i = 1; i >= 0; i--) {
            b = this.grabNextByte();
            // not sure one needs the "&" below - but put it in for safety given the typecasting
            value = value | ((b) & 0x000000ff) << (i * 8);
        }
        return value;
    }

    public int grabNext24BitInt(byte firstValue) throws FrameSizeException {
        int value = 0;
        byte b;
        int firstShift = 3;

        value = value | ((firstValue) & 0x000000ff) << (firstShift * 8);

        // Network order data should be Big Endian, so first byte read is most significant.  Java is also Big Endian, so...
        for (int i = 2; i >= 0; i--) {
            b = this.grabNextByte();
            // not sure one needs the "&" below - but put it in for safety given the typecasting
            value = value | ((b) & 0x000000ff) << (i * 8);
        }
        return value;
    }

    public int grabNext32BitInt() throws FrameSizeException {
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

    public byte[] grabNextBytes(int numBytes) throws FrameSizeException {
        byte[] value = new byte[numBytes];
        byte b;

        if (this.bytesRemaining() >= numBytes) {
            int j = 0;
            for (int i = numBytes - 1; i >= 0; i--) {
                b = this.grabNextByte();
                value[j++] = b;
            }
        }
        return value;
    }

    public boolean checkConnectionPreface() throws FrameSizeException {
        byte[] value = grabNextBytes(24);
        String valueString = new String(value);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "checkConnectionPreface: processNextFrame-:  stream: 0 frame type: Magic Preface  direction: "
                         + Direction.READ_IN
                         + " H2InboundLink hc: " + muxLink.hashCode());
            if (value != null) {
                Tr.debug(tc, "checkConnectionPreface: Preface String: " + Arrays.toString(valueString.getBytes()));
            }
        }

        return valueString.equals("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
    }

    public byte grabNextByte() throws FrameSizeException {

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

        String msg = Tr.formatMessage(tc, "bytes.notavailable", (Object[]) null);
        FrameSizeException e = new FrameSizeException(msg);
        throw e;
    }

    public Frame getCurrentFrame() {
        return currentFrame;
    }
}
