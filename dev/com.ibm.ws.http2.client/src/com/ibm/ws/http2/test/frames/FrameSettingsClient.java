/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2.test.frames;

import java.util.Base64;

/**
 *
 */
public class FrameSettingsClient extends com.ibm.ws.http.channel.h2internal.frames.FrameSettings {

    private Base64.Encoder urlEncoder;
    private final byte[] frameBuilt;

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

    }

    public String getBase64UrlPayload() {
        urlEncoder = Base64.getUrlEncoder();
        System.out.println(urlEncoder.encodeToString(payload()));
        return urlEncoder.encodeToString(payload());
    }

    private byte[] payload() {
        byte[] settingsPayload = new byte[frameBuilt.length - 9];
        System.arraycopy(frameBuilt, 9, settingsPayload, 0, getPayloadLength());
        return settingsPayload;
    }

}
