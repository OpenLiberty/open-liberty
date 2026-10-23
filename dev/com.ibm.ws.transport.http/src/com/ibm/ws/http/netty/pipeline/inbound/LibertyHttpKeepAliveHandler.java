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
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;

/**
 * Extends Netty's {@link HttpServerKeepAliveHandler} to enforce Liberty-specific
 * keep-alive configuration: {@code keepAliveEnabled} and {@code maxPersistentRequests}.
 *
 * <p>This handler consolidates all keep-alive logic in one place:
 * <ul>
 *   <li>Tracks the number of HTTP requests on the channel.</li>
 *   <li>Sets {@code Connection: close} on incoming requests when keep-alive is
 *       disabled or the maximum number of persistent requests has been reached.</li>
 *   <li>Delegates remaining keep-alive header management to the parent handler.</li>
 * </ul>
 */
public class LibertyHttpKeepAliveHandler extends HttpServerKeepAliveHandler {

    private static final TraceComponent tc = Tr.register(LibertyHttpKeepAliveHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static final String NAME = "httpKeepAlive";

    private final NettyHttpChannelConfig config;

    public LibertyHttpKeepAliveHandler(NettyHttpChannelConfig config) {
        this.config = config;
    }

    /**
     * Intercepts each incoming {@link HttpRequest} to:
     * <ol>
     *   <li>Skip keep-alive enforcement for HTTP/2 connections — identified by the
     *       connection's established {@link ProtocolState}, never by a request
     *       header, because a client can spoof headers such as
     *       {@code x-http2-stream-id} on an HTTP/1.x connection. Keep-alive is an
     *       HTTP/1.1 concept and the H2 codec manages its own connection
     *       lifecycle.</li>
     *   <li>Increment the per-channel HTTP/1.x request counter.</li>
     *   <li>Set {@code Connection: close} if keep-alive is disabled or the
     *       maximum number of persistent requests has been reached, so that
     *       the parent handler will subsequently close the connection after
     *       the response is written.</li>
     * </ol>
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;

            // Keep-alive is an HTTP/1.x concept only. Use the connection's established
            // protocol rather than a client-supplied header, which can be spoofed.
            boolean isHttp2 = ProtocolState.current(ctx.channel()) == ProtocolName.HTTP2;

            if (!isHttp2) {
                // Increment request counter
                Integer count = ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).get();
                if (count == null) {
                    count = 0;
                }
                ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).set(count + 1);

                int maxRequests = config.getMaximumPersistentRequests();
                boolean maxReached = maxRequests != -1 && count + 1 >= maxRequests;

                if (!config.isKeepAliveEnabled() || maxReached) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "channelRead: setting Connection: close — keepAliveEnabled=" + config.isKeepAliveEnabled()
                                     + ", requestCount=" + (count + 1) + ", maxRequests=" + maxRequests);
                    }
                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    /**
     * Passes the response write through to the parent, which handles the full
     * keep-alive response header and connection-close lifecycle.
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }
}
