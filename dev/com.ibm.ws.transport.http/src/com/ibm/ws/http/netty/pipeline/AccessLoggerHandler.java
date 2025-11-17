/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.util.Objects;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Netty pipeline handler that produces Liberty access-log entries.
 * 
 * This is intended to be placed directly after the HTTP codec but before the dispatcher
 * handles the response. The start-time is captured as early as the request is begun and 
 * the log is written once per response is final.
 * 
 */
public class AccessLoggerHandler extends ChannelDuplexHandler {

    private final HttpChannelConfig config;

    public AccessLoggerHandler(HttpChannelConfig config) {
        Objects.requireNonNull(config);
        this.config = config;
    }

    /**
     * When the {@link HttpInboundServiceContextImpl} context is passed down to this handler, this will write
     * a single access-log line, then forward the message down to the next pipeline handler.
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (Objects.nonNull(msg) && msg instanceof HttpInboundServiceContextImpl) {
            HttpInboundServiceContextImpl isc = (HttpInboundServiceContextImpl) msg;
            config.getAccessLog().log(isc.getRequest(), isc.getResponse(), isc.getRequestVersion().getName(), null, isc.getRemoteAddr().getHostAddress(), isc.getNumBytesWritten());
        }
        super.write(ctx, msg, promise);

    }

    /**
     * Marks the request-start time on the first pass through so that elapsed time can be
     * measured when the response is logged.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!ctx.channel().hasAttr(NettyHttpConstants.REQUEST_START_TIME)) {

            long startTime = System.nanoTime();
            ctx.channel().attr(NettyHttpConstants.REQUEST_START_TIME).set(startTime);
        }
        super.channelRead(ctx, msg);
    }

}
