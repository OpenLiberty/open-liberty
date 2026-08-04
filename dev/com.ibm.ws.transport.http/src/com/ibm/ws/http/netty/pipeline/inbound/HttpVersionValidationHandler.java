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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
/**
 * Netty inbound handler that enforces the version check
 *
*/

@Sharable
public class HttpVersionValidationHandler extends ChannelInboundHandlerAdapter {

    private static final TraceComponent tc = Tr.register(
            HttpVersionValidationHandler.class,
            HttpMessages.HTTP_TRACE_NAME,
            HttpMessages.HTTP_BUNDLE);

    public static final String NAME = "httpVersionValidationHandler";

    public static final HttpVersionValidationHandler INSTANCE = new HttpVersionValidationHandler();

    private HttpVersionValidationHandler() {}

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }

        if (rejectIfUnsupported(ctx, (HttpRequest) msg)) {
            return;
        }
        ctx.fireChannelRead(msg);
    }

    /**
     * Checks whether the HTTP version of the given message is supported
     * (HTTP/1.0 or HTTP/1.1).
     * @param ctx the channel handler context
     * @param msg the inbound HTTP message whose version is to be checked
     * @return  true if the version was unsupported and a 505 was sent;
     *          false if the version is legal and the caller should continue
     */
    public static boolean rejectIfUnsupported(ChannelHandlerContext ctx, HttpRequest msg) {
        HttpVersion version = msg.protocolVersion();

        if (HttpVersion.HTTP_1_0.equals(version) || HttpVersion.HTTP_1_1.equals(version)) {
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Rejecting request with unsupported HTTP version [" + version
                         + "] from " + ctx.channel().remoteAddress()
                         + ". Responding 505 and closing connection.");
        }

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.HTTP_VERSION_NOT_SUPPORTED,
                Unpooled.EMPTY_BUFFER);
        HttpUtil.setContentLength(response, 0);
        HttpUtil.setKeepAlive(response, false);
        ctx.writeAndFlush(response);
        return true;
    }
}