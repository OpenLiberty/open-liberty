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

import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.frames.Frame.FrameDirection;

/**
 *
 */
public class FrameFactory {

    public static Frame getFrame(byte frameType, int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        FrameTypes frameTypeWrite = getFrameType(frameType);

        switch (frameTypeWrite) {

            case DATA:
                return new FrameData(streamId, payloadLength, flags, reserveBit, direction);

            case HEADERS:
                return new FrameHeaders(streamId, payloadLength, flags, reserveBit, direction);

            case PRIORITY:
                return new FramePriority(streamId, payloadLength, flags, reserveBit, direction);

            case RST_STREAM:
                return new FrameRstStream(streamId, payloadLength, flags, reserveBit, direction);

            case SETTINGS:
                return new FrameSettings(streamId, payloadLength, flags, reserveBit, direction);

            case PUSH_PROMISE:
                return new FramePushPromise(streamId, payloadLength, flags, reserveBit, direction);

            case CONTINUATION:
                return new FrameContinuation(streamId, payloadLength, flags, reserveBit, direction);

            case PING:
                return new FramePing(streamId, payloadLength, flags, reserveBit, direction);

            case GOAWAY:
                return new FrameGoAway(streamId, payloadLength, flags, reserveBit, direction);

            case WINDOW_UPDATE:
                return new FrameWindowUpdate(streamId, payloadLength, flags, reserveBit, direction);

            case UNKNOWN:
                return new FrameUnknown(streamId, payloadLength, flags, reserveBit, direction);

            default:
                return null;
        }
    }

    public static FrameTypes getFrameType(byte byteFrameType) {

        if (byteFrameType == 0x00) {
            return FrameTypes.DATA;
        }
        if (byteFrameType == 0x01) {
            return FrameTypes.HEADERS;
        }
        if (byteFrameType == 0x02) {
            return FrameTypes.PRIORITY;
        }
        if (byteFrameType == 0x03) {
            return FrameTypes.RST_STREAM;
        }
        if (byteFrameType == 0x04) {
            return FrameTypes.SETTINGS;
        }
        if (byteFrameType == 0x05) {
            return FrameTypes.PUSH_PROMISE;
        }
        if (byteFrameType == 0x06) {
            return FrameTypes.PING;
        }
        if (byteFrameType == 0x07) {
            return FrameTypes.GOAWAY;
        }
        if (byteFrameType == 0x08) {
            return FrameTypes.WINDOW_UPDATE;
        }
        if (byteFrameType == 0x09) {
            return FrameTypes.CONTINUATION;
        }

        return FrameTypes.UNKNOWN;
    }
}
