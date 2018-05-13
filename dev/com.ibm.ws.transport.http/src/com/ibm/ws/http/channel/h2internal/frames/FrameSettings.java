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
import com.ibm.ws.http.channel.h2internal.exceptions.FlowControlException;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FrameSettings extends Frame {

    /*
     * format of setting frame
     *
     * length(24) XX XX XX - length of payload
     * type(8) 04 - SETTINGS
     * flags(8) XX - bit 0 ACK
     * R (1) 0
     * Stream (31) 00 00 00 00
     * Payload
     * Identifier (16) 00 0X -
     * value (32) XX XX XX XX
     * Identifier (16) 00 0X
     * value (32) XX XX XX XX
     * etc....
     */

    private int headerTableSize = -1;
    private int enablePush = -1;
    private int maxConcurrentStreams = -1;
    private int maxFrameSize = -1;
    private int maxHeaderListSize = -1;
    private int initialWindowSize = -1;

    // frame size setting constants
    private final int MAX_INITIAL_WINDOW_SIZE = 2147483647;
    private final int INITIAL_MAX_FRAME_SIZE = 16384;
    private final int MAX_FRAME_SIZE = 16777215;

    // Payload IDs
    private final int HEADER_TABLE_SIZE_ID = 0x01;
    private final int ENABLE_PUSH_ID = 0x02;
    private final int MAX_CONCURRENT_STREAMS_ID = 0x03;
    private final int INITIAL_WINDOW_SIZE_ID = 0x04;
    private final int MAX_FRAME_SIZE_ID = 0x05;
    private final int MAX_HEADER_LIST_SIZE_ID = 0x06;

    /**
     * Read frame constructor
     */
    public FrameSettings(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.SETTINGS;
    }

    /**
     * Write frame constructor
     */
    public FrameSettings(int streamId, int headerTableSize, int enablePush, int maxConcurrentStreams, int initialWindowSize,
                         int maxFrameSize, int maxHeaderListSize, boolean reserveBit) {
        super(streamId, 0, (byte) 0x00, reserveBit, FrameDirection.WRITE);

        if (headerTableSize != -1) {
            this.headerTableSize = headerTableSize;
            payloadLength += 6;
        }
        if (enablePush != -1) {
            this.enablePush = enablePush;
            payloadLength += 6;
        }
        if (maxConcurrentStreams != -1) {
            this.maxConcurrentStreams = maxConcurrentStreams;
            payloadLength += 6;
        }
        if (initialWindowSize != -1) {
            this.initialWindowSize = initialWindowSize;
            payloadLength += 6;
        }
        if (maxFrameSize != -1) {
            this.maxFrameSize = maxFrameSize;
            payloadLength += 6;
        }
        if (maxHeaderListSize != -1) {
            this.maxHeaderListSize = maxHeaderListSize;
            payloadLength += 6;
        }

        frameType = FrameTypes.SETTINGS;
        writeFrameLength += payloadLength;
        setInitialized(); // we have everything we need to write out, now
    }

    /**
     * EMPTY frame constructor
     */
    public FrameSettings() {
        this(0, -1, -1, -1, -1, -1, -1, false);
        setInitialized(); // we have everything we need to write out, now
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws Http2Exception {
        // +-------------------------------+
        // |       Identifier (16)         |
        // +-------------------------------+-------------------------------+
        // |                        Value (32)                             |
        // +---------------------------------------------------------------+

        if (payloadLength % 6 != 0) {
            throw new FrameSizeException("Settings frame is malformed");
        }
        int numberOfSettings = this.payloadLength / 6;
        int settingId;
        long settingValue;

        while (numberOfSettings-- > 0) {
            settingId = frp.grabNext16BitInt();
            settingValue = frp.grabNext32BitInt();
            settingValue = settingValue & 0x00000000ffffffffL; // convert to unsigned
            putSettingValue(settingId, settingValue);
        }
    }

    public void processPayload(byte[] payload) throws Http2Exception {
        // +-------------------------------+
        // |       Identifier (16)         |
        // +-------------------------------+-------------------------------+
        // |                        Value (32)                             |
        // +---------------------------------------------------------------+

        int numberOfSettings = this.payloadLength / 6;
        int settingId;
        long settingValue;

        int i = 0;
        while (numberOfSettings-- > 0) {
            settingId = (payload[i++] & payload[i++]);
            settingValue = (payload[i++] & payload[i++] & payload[i++] & payload[i++]);
            putSettingValue(settingId, settingValue);
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
        setFrameHeaders(frame, utils.FRAME_TYPE_SETTINGS);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        if (headerTableSize != -1) {
            utils.Move16BitstoByteArray(HEADER_TABLE_SIZE_ID, frame, frameIndex);
            frameIndex += 2;
            utils.Move32BitstoByteArray(headerTableSize, frame, frameIndex);
            frameIndex += 4;
        }

        if (enablePush != -1) {
            utils.Move16BitstoByteArray(ENABLE_PUSH_ID, frame, frameIndex);
            frameIndex += 2;
            utils.Move32BitstoByteArray(enablePush, frame, frameIndex);
            frameIndex += 4;
        }
        if (maxConcurrentStreams != -1) {
            utils.Move16BitstoByteArray(MAX_CONCURRENT_STREAMS_ID, frame, frameIndex);
            frameIndex += 2;
            utils.Move32BitstoByteArray(maxConcurrentStreams, frame, frameIndex);
            frameIndex += 4;
        }
        if (initialWindowSize != -1) {
            utils.Move16BitstoByteArray(INITIAL_WINDOW_SIZE_ID, frame, frameIndex);
            frameIndex += 2;
            utils.Move32BitstoByteArray(initialWindowSize, frame, frameIndex);
            frameIndex += 4;
        }
        if (maxFrameSize != -1) {
            utils.Move16BitstoByteArray(MAX_FRAME_SIZE_ID, frame, frameIndex);
            frameIndex += 2;
            utils.Move32BitstoByteArray(maxFrameSize, frame, frameIndex);
            frameIndex += 4;
        }
        if (maxHeaderListSize != -1) {
            utils.Move16BitstoByteArray(MAX_HEADER_LIST_SIZE_ID, frame, frameIndex);
            frameIndex += 2;
            utils.Move32BitstoByteArray(maxHeaderListSize, frame, frameIndex);
            frameIndex += 4;
        }

        buffer.put(frame, 0, writeFrameLength);
        buffer.flip();
        return buffer;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws Http2Exception {
        if (streamId != 0) {
            throw new ProtocolException("SETTINGS frame stream ID must be 0x0; received " + streamId);
        }
        if (this.ACK_FLAG == true && this.getPayloadLength() != 0) {
            throw new FrameSizeException("SETTINGS frame with ACK set cannot have an additional payload");
        }
        if (enablePush != -1 && enablePush != 0 && enablePush != 1) {
            throw new ProtocolException("SETTINGS_ENABLE_PUSH must be set to 0 or 1 " + streamId);
        }
        if (maxFrameSize != -1 && maxFrameSize < INITIAL_MAX_FRAME_SIZE) {
            throw new ProtocolException("SETTINGS_MAX_FRAME_SIZE value is below the allowable minimum value");
        }
        if (maxFrameSize != -1 && maxFrameSize > MAX_FRAME_SIZE) {
            throw new ProtocolException("SETTINGS_MAX_FRAME_SIZE value exceeded the max allowable value");
        }
        /*
         * We store initialWindowSize as a signed int, so overflow is checked in putSettingValue instead of here
         */
    }

    @Override
    protected void setFlags() {
        ACK_FLAG = utils.getFlag(flags, 0);
    }

    /**
     * @return the headerTableSize
     */
    public int getHeaderTableSize() {
        return headerTableSize;
    }

    /**
     * @return the enablePush
     */
    public int getEnablePush() {
        return enablePush;
    }

    /**
     * @return the maxConcurrentStreams
     */
    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    /**
     * @return the initialWindowSize
     */
    public int getInitialWindowSize() {
        return initialWindowSize;
    }

    /**
     * @return the maxFrameSize
     */
    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    /**
     * @return the maxHeaderListSize
     */
    public int getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    private void putSettingValue(int id, long value) throws ProtocolException, FlowControlException {
        switch (id) {
            case HEADER_TABLE_SIZE_ID:
                if (value > Integer.MAX_VALUE) {
                    throw new ProtocolException("Max header table size setting value exceeded max allowable value");
                }
                headerTableSize = (int) value;
                break;
            case ENABLE_PUSH_ID:
                enablePush = (int) value;
                break;
            case MAX_CONCURRENT_STREAMS_ID:
                if (value > Integer.MAX_VALUE) {
                    throw new ProtocolException("Max concurrent streams setting value exceeded max allowable value");
                }
                maxConcurrentStreams = (int) value;
                break;
            case INITIAL_WINDOW_SIZE_ID:
                if (value > Integer.MAX_VALUE) {
                    throw new FlowControlException("Initial window size setting value exceeded max allowable value");
                }
                initialWindowSize = (int) value;
                break;
            case MAX_FRAME_SIZE_ID:
                if (value > Integer.MAX_VALUE) {
                    throw new ProtocolException("Max frame size setting value exceeded max allowable value");
                }
                maxFrameSize = (int) value;
                break;
            case MAX_HEADER_LIST_SIZE_ID:
                if (value > Integer.MAX_VALUE) {
                    throw new ProtocolException("Max header list size setting value exceeded max allowable value");
                }
                maxHeaderListSize = (int) value;
                break;
            default:
                break;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FrameSettings)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Object is not FrameSettings");
            }
            return false;
        }

        FrameSettings frameSettingsToCompare = (FrameSettings) object;

        if (!super.equals(frameSettingsToCompare))
            return false;

        if (this.getEnablePush() != frameSettingsToCompare.getEnablePush()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getEnablePush() = " + this.getEnablePush() + " frameSettingsToCompare.getEnablePush() = " + frameSettingsToCompare.getEnablePush());
            }
            return false;
        }
        if (this.getHeaderTableSize() != frameSettingsToCompare.getHeaderTableSize()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getHeaderTableSize() = " + this.getHeaderTableSize() + " frameSettingsToCompare.getHeaderTableSize() = "
                             + frameSettingsToCompare.getHeaderTableSize());
            }
            return false;
        }
        if (this.getInitialWindowSize() != frameSettingsToCompare.getInitialWindowSize()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getInitialWindowSize() = " + this.getInitialWindowSize() + " frameSettingsToCompare.getInitialWindowSize() = "
                             + frameSettingsToCompare.getInitialWindowSize());
            }
            return false;
        }
        if (this.getMaxConcurrentStreams() != frameSettingsToCompare.getMaxConcurrentStreams()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getMaxConcurrentStreams() = " + this.getMaxConcurrentStreams() + " frameSettingsToCompare.getMaxConcurrentStreams() = "
                             + frameSettingsToCompare.getMaxConcurrentStreams());
            }

            return false;
        }
        if (this.getMaxFrameSize() != frameSettingsToCompare.getMaxFrameSize()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getMaxFrameSize() = " + this.getMaxFrameSize() + " frameSettingsToCompare.getMaxFrameSize() = " + frameSettingsToCompare.getMaxFrameSize());
            }
            return false;
        }
        if (this.getMaxHeaderListSize() != frameSettingsToCompare.getMaxHeaderListSize()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "this.getMaxHeaderListSize() = " + this.getMaxHeaderListSize() + " frameSettingsToCompare.getMaxHeaderListSize() = "
                             + frameSettingsToCompare.getMaxHeaderListSize());
            }
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString());

        frameToString.append("EnablePush: " + this.getEnablePush() + "\n");
        frameToString.append("HeaderTableSize: " + this.getHeaderTableSize() + "\n");
        frameToString.append("InitialWindowSize: " + this.getInitialWindowSize() + "\n");
        frameToString.append("MaxFrameSize: " + this.getMaxFrameSize() + "\n");
        frameToString.append("MaxHeaderListSize: " + this.getMaxHeaderListSize() + "\n");
        frameToString.append("MaxConcurrentStreams: " + this.getMaxConcurrentStreams() + "\n");

        return frameToString.toString();

    }

}