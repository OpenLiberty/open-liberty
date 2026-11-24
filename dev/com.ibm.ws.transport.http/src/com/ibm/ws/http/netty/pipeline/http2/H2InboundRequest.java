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

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

/** A request published before END_STREAM, bound to the decoder's stream ID. */
public final class H2InboundRequest extends DefaultHttpRequest {
    private final int streamId;

    public H2InboundRequest(int streamId, HttpRequest request) {
        super(request.protocolVersion(), request.method(), request.uri(), request.headers());
        this.streamId = streamId;
    }

    public int streamId() {
        return streamId;
    }
}
