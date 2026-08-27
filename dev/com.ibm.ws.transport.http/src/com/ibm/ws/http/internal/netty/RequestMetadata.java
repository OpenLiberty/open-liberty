/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.internal.netty;

import java.util.List;

import com.ibm.ws.http.internal.netty.exception.InvalidRequestMetadataException;
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.ProtocolState;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;

import io.netty.handler.codec.http2.HttpConversionUtil;

/** Protocol and stream details captured once for an inbound request. */
public final class RequestMetadata {
    private static final CharSequence STREAM_ID = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text();

    private final ProtocolName protocol;
    private final int streamId;

    private RequestMetadata(ProtocolName protocol, int streamId) {
        this.protocol = protocol;
        this.streamId = streamId;
    }
    /**
     * Capture the request protocol and validate its HTTP/2 stream ID. This runs before
     * request wrappers and request-owned state are created.
     */
    public static RequestMetadata capture(Channel channel, HttpRequest request) {
        ProtocolName protocol = ProtocolState.current(channel);
        if (protocol == ProtocolName.HTTP10 || protocol == ProtocolName.HTTP1) {
            request.headers().remove(STREAM_ID);
            return new RequestMetadata(protocol, -1);
        }
        if (protocol != ProtocolName.HTTP2) {
            throw new InvalidRequestMetadataException("Request received before the connection protocol was established: " + protocol);
        }

        List<String> values = request.headers().getAll(STREAM_ID);
        if (values.size() != 1) {
            throw new InvalidRequestMetadataException("HTTP/2 request must contain exactly one stream ID");
        }

        String value = values.get(0);
        final int streamId;
        try {
            streamId = Integer.parseInt(value);
        } catch (RuntimeException e) {
            throw new InvalidRequestMetadataException("HTTP/2 request has an invalid stream ID", e);
        }
        if (streamId <= 0) {
            throw new InvalidRequestMetadataException("HTTP/2 request has a nonpositive stream ID: " + streamId);
        }
        return new RequestMetadata(protocol, streamId);
    }

    public ProtocolName protocol() {
        return protocol;
    }

    public boolean isHttp2() {
        return protocol == ProtocolName.HTTP2;
    }

    public int streamId() {
        return streamId;
    }
}
