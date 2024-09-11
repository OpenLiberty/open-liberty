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
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 *
 */
public class TransportOutboundHandler extends ChannelOutboundHandlerAdapter {

    HttpChannelConfig config;

    FullHttpResponse fullResponse;

    public TransportOutboundHandler(HttpChannelConfig config) {
        Objects.requireNonNull(config);
        this.config = config;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {


        if (msg instanceof HttpResponse) {

            HttpResponse response = (HttpResponse) msg;

            final boolean isSwitching = response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS);

            ChannelFuture future = ctx.writeAndFlush(msg);

            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess() && isSwitching) {

                        ctx.pipeline().remove(TransportOutboundHandler.class);
                        if (Objects.nonNull(ctx.pipeline().get(HttpServerCodec.class))) {
                            ctx.pipeline().remove(HttpServerCodec.class);
                        }

                        ctx.pipeline().remove("maxConnectionHandler");
                        ctx.pipeline().remove("chunkLoggingHandler");
                        ctx.pipeline().remove("chunkWriteHandler");
                        ctx.pipeline().remove(ByteBufferCodec.class);

                        if (ctx.pipeline().get(NettyServletUpgradeHandler.class) == null) {

                            NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(ctx.channel());
                            ctx.pipeline().addLast(upgradeHandler);
                        }
                    }
                }
            });
        }

        else {
            super.write(ctx, msg, promise);
        }
    }

}
