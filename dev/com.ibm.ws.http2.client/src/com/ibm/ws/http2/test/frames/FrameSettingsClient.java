/*******************************************************************************
 * Copyright (c) 2018, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http2.test.frames;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Base64;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

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

    static {
        // We discovered an issue with the adaptive allocator using Unsafe being unavailable
        // and throwing exceptions which do not allow the tests to proceed due to Java 2 Security.
        // Because of this, while this is fixed we will use the pooled allocator as before to
        // ensure proper testing
        if (System.getSecurityManager() == null) {
            System.setProperty("io.netty.allocator.type", "pooled");
        }
        else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    System.setProperty("io.netty.allocator.type", "pooled");
                    return null;
                }
            });
        }
    }

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

        // Get the local settings for the handler.
        Http2Settings settings = new Http2Settings();

        // Serialize the payload of the SETTINGS frame
        int payloadLength = 6 * settings.size();

        frame = Unpooled.buffer(payloadLength);

        for (CharObjectMap.PrimitiveEntry<Long> entry : settings.entries()) {
            frame.writeChar(entry.key());
            frame.writeInt(entry.value().intValue());
        }
    }

    public String getBase64UrlPayload() {
        return io.netty.handler.codec.base64.Base64.encode(frame, io.netty.handler.codec.base64.Base64Dialect.URL_SAFE).toString(io.netty.util.CharsetUtil.UTF_8);
    }

    private byte[] payload() {
        return frameBuilt.array();
    }

}
