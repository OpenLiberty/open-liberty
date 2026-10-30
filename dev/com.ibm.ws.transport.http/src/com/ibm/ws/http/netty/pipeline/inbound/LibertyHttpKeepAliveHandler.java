/*******************************************************************************
 * Copyright 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.ProtocolState;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.HttpStatusClass;

/**
 * Extends Netty's {@link HttpServerKeepAliveHandler} to enforce Liberty-specific
 * keep-alive configuration: {@code keepAliveEnabled} and {@code maxKeepAliveRequests}.
 *
 * <p>This handler consolidates all keep-alive logic in one place:
 * <ul>
 *   <li>Counts the HTTP/1.x requests on the channel.</li>
 *   <li>Sets {@code Connection: close} on the <em>response</em> when keep-alive is
 *       disabled or the maximum number of persistent requests has been reached.
 *       Request headers are never modified.</li>
 *   <li>Delegates the rest of the keep-alive handling, including closing the
 *       channel after a response that says {@code Connection: close}, to the
 *       parent handler.</li>
 * </ul>
 *
 * <p>Enforcing on the response matches how the channel framework and the earlier
 * Netty implementation behave. Handlers further down the pipeline, such as the
 * WebSocket handshake, therefore see the request exactly as the client sent it.
 */
public class LibertyHttpKeepAliveHandler extends HttpServerKeepAliveHandler {

    private static final TraceComponent tc = Tr.register(LibertyHttpKeepAliveHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static final String NAME = "httpKeepAlive";

    private final NettyHttpChannelConfig config;

    public LibertyHttpKeepAliveHandler(NettyHttpChannelConfig config) {
        this.config = config;
    }

    /**
     * Counts each incoming {@link HttpRequest} on an HTTP/1.x connection. The
     * request is passed on unchanged.
     *
     * <p>HTTP/2 connections are skipped. They are identified by the connection's
     * established {@link ProtocolState}, never by a request header, because a
     * client can spoof headers such as {@code x-http2-stream-id} on an HTTP/1.x
     * connection. Keep-alive is an HTTP/1.1 concept and the H2 codec manages its
     * own connection lifecycle.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest && !isHttp2(ctx)) {
            Integer count = ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).get();
            ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).set(count == null ? 1 : count + 1);
        }
        super.channelRead(ctx, msg);
    }

    /**
     * Sets {@code Connection: close} on the outgoing response when keep-alive is
     * disabled or the maximum number of persistent requests has been reached, then
     * lets the parent handler close the connection after the response is written.
     *
     * <p>Informational responses ({@code 1xx}) are left alone. That keeps the
     * {@code Connection: Upgrade} of a {@code 101 Switching Protocols}, and lets a
     * {@code 100 Continue} pass through; the final response carries the
     * {@code close}.
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse && !isHttp2(ctx)) {
            HttpResponse response = (HttpResponse) msg;
            if (response.status().codeClass() != HttpStatusClass.INFORMATIONAL && shouldClose(ctx)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            }
        }
        super.write(ctx, msg, promise);
    }

    /** True when keep-alive is disabled or the request limit has been reached. */
    private boolean shouldClose(ChannelHandlerContext ctx) {
        Integer count = ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).get();
        int requests = count == null ? 0 : count;
        int maxRequests = config.getMaximumPersistentRequests();
        boolean maxReached = maxRequests != -1 && requests >= maxRequests;

        boolean close = !config.isKeepAliveEnabled() || maxReached;
        if (close && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "write: setting Connection: close on the response — keepAliveEnabled=" + config.isKeepAliveEnabled()
                         + ", requestCount=" + requests + ", maxRequests=" + maxRequests);
        }
        return close;
    }

    /** Keep-alive is an HTTP/1.x concept; the connection's established protocol decides, not a header. */
    private static boolean isHttp2(ChannelHandlerContext ctx) {
        return ProtocolState.current(ctx.channel()) == ProtocolName.HTTP2;
    }
}
