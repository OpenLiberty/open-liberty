/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.frames;

import java.util.Arrays;
import java.util.Base64;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.util.collection.CharObjectMap;

/**
 *
 */
public class FrameSettingsClient extends com.ibm.ws.http.channel.h2internal.frames.FrameSettings {

    private final Base64.Encoder urlEncoder;
    private final WsByteBuffer frameBuilt;

    private final ByteBuf frame;

    /**
     *
     * @param streamId
     * @param headerTableSize
     * @param enablePush
     * @param maxConcurrentStreams
     * @param initialWindowSize
     * @param maxFrameSize
     * @param maxHeaderListSize
     * @param reserveBit
     */
    public FrameSettingsClient(int streamId, int headerTableSize, int enablePush, int maxConcurrentStreams, int initialWindowSize,
                               int maxFrameSize, int maxHeaderListSize, boolean reserveBit) {
        super(streamId, headerTableSize, enablePush, maxConcurrentStreams, initialWindowSize, maxFrameSize, maxHeaderListSize, reserveBit);
        frameBuilt = buildFrameForWrite();
        urlEncoder = Base64.getUrlEncoder();

        System.out.println("Got built buffer: ");
        System.out.println(WsByteBufferUtils.asString(frameBuilt));
        System.out.println("Got buffer array: ");
        System.out.println(Arrays.toString(payload()));
        System.out.println("Got encoded buffer: ");
        System.out.println(urlEncoder.encodeToString(payload()));
        System.out.println("Buffer size: ");
        System.out.println(payload().length);

        // Get the local settings for the handler.
        Http2Settings settings = Http2Settings.defaultSettings();

        // Serialize the payload of the SETTINGS frame
        int payloadLength = 6 * settings.size();
//        frame = Unpooled.wrappedBuffer(ByteBuffer.allocate(payloadLength));

        frame = Unpooled.buffer(payloadLength);

//        frameBuilt.clear();
        for (CharObjectMap.PrimitiveEntry<Long> entry : settings.entries()) {
            frame.writeChar(entry.key());
            frame.writeInt(entry.value().intValue());
//            frameBuilt.putChar(entry.key());
//            frameBuilt.putInt(entry.value().intValue());
        }
    }

    public String getBase64UrlPayload() {
        return io.netty.handler.codec.base64.Base64.encode(frame, io.netty.handler.codec.base64.Base64Dialect.URL_SAFE).toString(io.netty.util.CharsetUtil.UTF_8);
//        urlEncoder = Base64.getUrlEncoder();
//        System.out.println(urlEncoder.encodeToString(payload()));
//        return urlEncoder.encodeToString(payload());
    }

    private byte[] payload() {
        return frameBuilt.array();
    }

}
