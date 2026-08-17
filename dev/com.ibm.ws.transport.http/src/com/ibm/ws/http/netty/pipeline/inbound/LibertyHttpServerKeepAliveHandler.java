/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;

/**
 * This is a subclass of HttpServerKeepAliveHandler that gates
 * inbound requests against the HTTP/1.x version whitelist (HTTP/1.0 and
 * HTTP/1.1) before delegating to the keep-alive logic.
 *
 * Any HttpRequest whose protocol version is not HTTP/1.0 or HTTP/1.1
 * is rejected with 505 HTTP Version Not Supported
 *
 */
public class LibertyHttpServerKeepAliveHandler extends HttpServerKeepAliveHandler {

    private static final TraceComponent tc = Tr.register(
            LibertyHttpServerKeepAliveHandler.class,
            HttpMessages.HTTP_TRACE_NAME,
            HttpMessages.HTTP_BUNDLE);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpVersion version = ((HttpRequest) msg).protocolVersion();
            if (!HttpVersion.HTTP_1_0.equals(version) && !HttpVersion.HTTP_1_1.equals(version)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Rejecting request with unsupported HTTP version [" + version
                                 + "] from " + ctx.channel().remoteAddress()
                                 + ". Responding 505 and closing connection.");
                }
                ReferenceCountUtil.release(msg);  

                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED,
                        Unpooled.EMPTY_BUFFER);
                HttpUtil.setContentLength(response, 0);
                HttpUtil.setKeepAlive(response, false);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return; 
            }
        }
        super.channelRead(ctx, msg);
    }
}
