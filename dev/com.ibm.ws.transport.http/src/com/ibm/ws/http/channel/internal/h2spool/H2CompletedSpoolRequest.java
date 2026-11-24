/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.h2spool;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http2.HttpConversionUtil;

/**
 * An ordinary HTTP/2 request published only after END_STREAM and a successful
 * body seal. The dispatcher takes the body once; otherwise this event closes it.
 */
public final class H2CompletedSpoolRequest extends DefaultHttpRequest implements AutoCloseable {
    private final int streamId;
    private final HttpHeaders trailers;
    private H2SealedBody body;

    public H2CompletedSpoolRequest(int streamId, HttpRequest headers, H2SealedBody body, HttpHeaders trailers) {
        super(headers.protocolVersion(), headers.method(), headers.uri(), headers.headers().copy());
        if (streamId <= 0) {
            throw new IllegalArgumentException("HTTP/2 stream ID must be positive");
        }
        List<String> ids = headers.headers().getAll(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if (ids.size() != 1 || !Integer.toString(streamId).equals(ids.get(0))) {
            throw new IllegalArgumentException("HTTP/2 request headers do not match the stream ID");
        }
        H2SealedBody sealed = Objects.requireNonNull(body);
        // Match the inherited completed-message route using this event's header copy.
        HttpUtil.setContentLength(this, sealed.length());
        this.streamId = streamId;
        this.body = sealed;
        this.trailers = trailers == null ? null : trailers.copy();
    }

    public int streamId() {
        return streamId;
    }

    public HttpHeaders trailers() {
        return trailers;
    }

    public synchronized H2SealedBody takeBody() {
        H2SealedBody taken = body;
        if (taken == null) {
            throw new IllegalStateException("HTTP/2 sealed body already transferred or closed");
        }
        if (!taken.claimForApplication()) {
            throw new IllegalStateException("HTTP/2 sealed body closed before application handoff");
        }
        body = null;
        return taken;
    }

    public synchronized CompletionStage<Void> closeAsync() {
        H2SealedBody owned = body;
        body = null;
        return owned == null ? java.util.concurrent.CompletableFuture.completedFuture(null) : owned.closeAsync();
    }

    @Override
    public void close() {
        closeAsync();
    }
}
