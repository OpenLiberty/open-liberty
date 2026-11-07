/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.http2;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpHeaders;

/** DATA or trailers published with the decoder's stream ID. */
public final class H2InboundContent extends DefaultHttpContent {
    private final int streamId;
    private final boolean endStream;
    private final HttpHeaders trailers;

    public H2InboundContent(int streamId, ByteBuf content, boolean endStream, HttpHeaders trailers) {
        super(content);
        this.streamId = streamId;
        this.endStream = endStream;
        this.trailers = trailers;
    }

    public int streamId() {
        return streamId;
    }

    public boolean endStream() {
        return endStream;
    }

    public HttpHeaders trailers() {
        return trailers;
    }
}
