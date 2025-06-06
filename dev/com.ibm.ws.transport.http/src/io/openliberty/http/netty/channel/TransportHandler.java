/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.channel;

import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.ByteBufferCodec;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.openliberty.netty.internal.tcp.MaxOpenConnectionsHandler;

/**
 * Stateless sharable handler for transport operations.
 */
@Sharable
public final class TransportHandler extends ChannelDuplexHandler{

    public static final TransportHandler INSTANCE = new TransportHandler();

    public TransportHandler(){}

    @Override
    public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
        if(message instanceof FullHttpRequest){
            FullHttpRequest request = (FullHttpRequest) message;
            String acceptEncoding = request.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
            if(acceptEncoding != null){
                context.channel().attr(NettyHttpConstants.ACCEPT_ENCODING).set(acceptEncoding);
            }
        }
        context.fireChannelRead(message);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception{
        if(!(message instanceof HttpResponse)){
            context.write(message, promise);
            return;
        }

        HttpResponse response = (HttpResponse) message;
        boolean switching = response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS);
        ChannelFuture future = context.write(message, promise);

        if(!switching){
            return;
        }
        future.addListener(f -> {
            if(f.isSuccess()){
                ChannelPipeline pipeline = context.pipeline();
                pipeline.remove(TransportHandler.this);
                removeIfPresent(pipeline, HttpServerCodec.class);
                removeIfPresent(pipeline, MaxOpenConnectionsHandler.class);
                removeIfPresent(pipeline, ChunkedWriteHandler.class);
                removeIfPresent(pipeline, ByteBufferCodec.class);

                if(pipeline.get(NettyServletUpgradeHandler.class) == null){
                    pipeline.addLast(new NettyServletUpgradeHandler(context.channel()));
                }
            }
        });
    }

    private static void removeIfPresent(ChannelPipeline pipeline, Class<? extends ChannelHandler> handler){
        if(pipeline.get(handler) != null){
            pipeline.remove(handler);
        }
    }

}
