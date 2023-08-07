/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 *
 */
public class AccessLoggerHandler extends ChannelDuplexHandler {

    private final HttpChannelConfig config;

    public AccessLoggerHandler(HttpChannelConfig config) {
        Objects.requireNonNull(config);
        this.config = config;
        MSP.log("Access logger config set, format : " + config.getAccessLog().getFormat());
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (Objects.nonNull(msg) && msg instanceof HttpInboundServiceContextImpl) {
            HttpInboundServiceContextImpl isc = (HttpInboundServiceContextImpl) msg;
            config.getAccessLog().log(isc.getRequest(), isc.getResponse(), isc.getRequestVersion().getName(), null, isc.getRemoteAddr().getHostAddress(), isc.getNumBytesWritten());
        }
        super.write(ctx, msg, promise);

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!ctx.channel().hasAttr(NettyHttpConstants.REQUEST_START_TIME)) {

            long startTime = System.nanoTime();
            MSP.log("Setting current time: " + startTime);
            ctx.channel().attr(NettyHttpConstants.REQUEST_START_TIME).set(startTime);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

}
