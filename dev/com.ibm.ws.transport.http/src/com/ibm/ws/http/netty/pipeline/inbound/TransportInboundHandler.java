/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 *
 */
public class TransportInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final HttpChannelConfig config;

    /**
     * @param httpConfig
     */
    public TransportInboundHandler(HttpChannelConfig httpConfig) {
        this.config = httpConfig;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {

        //TODO: cleanup into individual handlers
        if (request.headers().contains(HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName())) {
            MSP.log("Found Accept-Encoding: " + request.headers().get(HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName()));
            context.channel().attr(NettyHttpConstants.ACCEPT_ENCODING).set(request.headers().get(HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName()));
        }
        context.fireChannelRead(request.retain());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        MSP.log("Channel became inactive. Channel: {}" + " " + ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        MSP.log("Exception caught in channel. Channel: {}, Reason: {} " + ctx.channel() + " " + cause.getMessage() + " " + cause);
        ctx.close(); // Optionally close the channel if an exception is caught
    }

}
