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
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.outbound.HeaderHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpResponse;

/**
 *
 */
public class TransportOutboundHandler extends ChannelOutboundHandlerAdapter {

    HttpChannelConfig config;

    public TransportOutboundHandler(HttpChannelConfig config) {
        Objects.requireNonNull(config);
        this.config = config;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        MSP.log("Writing outbound, msg is: " + msg);
        //TODO: only if first time running through here (persist needs to clear)

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            HeaderHandler headerHandler = new HeaderHandler(config, response);
            headerHandler.complianceCheck();

            if (ctx.channel().hasAttr(NettyHttpConstants.ACCEPT_ENCODING)) {
                String acceptEncoding = ctx.channel().attr(NettyHttpConstants.ACCEPT_ENCODING).get();
                ResponseCompressionHandler compressionHandler = new ResponseCompressionHandler(config, response, acceptEncoding);
                compressionHandler.process();
            }

        }

        ctx.writeAndFlush(msg);
    }

}
