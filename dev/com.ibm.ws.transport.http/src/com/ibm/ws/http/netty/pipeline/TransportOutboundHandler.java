/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.openliberty.http.pipeline.configurators.PipelineHandlerUtility;

/**
 * This adapter handles outbound HTTP responses.
 * If the response status indicates a protocol switch (e.g., WebSocket upgrade),
 * this handler removes itself and certain HTTP/1.1-related handlers from the pipeline,
 * preparing the pipeline for the upgraded protocol.
 */
public class TransportOutboundHandler extends ChannelOutboundHandlerAdapter {

    private final HttpChannelConfig config;

    /**
     * Constructs a TransportOutboundHandler with the given configuration.
     *
     * @param config The HttpChannelConfig containing server configuration settings.
     */
    public TransportOutboundHandler(HttpChannelConfig config) {
        this.config = Objects.requireNonNull(config, "HttpChannelConfig cannot be null");
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {


        if (msg instanceof HttpResponse) {

            HttpResponse response = (HttpResponse) msg;

            final boolean isSwitching = response.status().equals(HttpResponseStatus.SWITCHING_PROTOCOLS);
            ChannelFuture future = ctx.writeAndFlush(msg, promise);

            future.addListener((ChannelFutureListener) f -> {

                if(f.isSuccess()){
                System.out.println("MSP HTTP11CONFIG -> " + ctx.pipeline().names());
                System.out.println("Switching: " + isSwitching);
                }

                if (f.isSuccess() && isSwitching) {
                    // On successful protocol switch, remove TransportOutboundHandler and HTTP codec
                    removeHandlerIfPresent(ctx.pipeline(), TransportOutboundHandler.class);
                    removeHandlerIfPresent(ctx.pipeline(), HttpServerCodec.class);
                    //TODO: set a PipelineHandlerUtility name to these. 

                    // Remove other handlers that were part of the HTTP pipeline
                    removeHandlerIfPresent(ctx.pipeline(), PipelineHandlerUtility.MAX_CONNECTION_HANDLER_NAME);
                    removeHandlerIfPresent(ctx.pipeline(), PipelineHandlerUtility.CHUNK_LOGGING_HANDLER_NAME);
                    removeHandlerIfPresent(ctx.pipeline(), PipelineHandlerUtility.CHUNK_WRITE_HANDLER_NAME);
                    removeHandlerIfPresent(ctx.pipeline(), PipelineHandlerUtility.BYTE_BUFFER_CODEC_HANDLER_NAME);

                    // If NettyServletUpgradeHandler is not present, add it
                    if (ctx.pipeline().get(PipelineHandlerUtility.NETTY_SERVLET_UPGRADE_HANDLER_NAME) == null) {
                        NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(ctx.channel());
                        ctx.pipeline().addLast(PipelineHandlerUtility.NETTY_SERVLET_UPGRADE_HANDLER_NAME, upgradeHandler);
                    }
                    System.out.println("MSP after switching-> " + ctx.pipeline().names());
                } else{
                    System.out.println("MSP OUTBOUND NOT SUCCESS or not switching-> " + ctx.pipeline());
    
                }
            });
        } else {
            // For non-HttpResponse messages, just pass through
            super.write(ctx, msg, promise);
        }
    }

    /**
     * Removes a handler by name if it exists in the pipeline.
     *
     * @param pipeline    The ChannelPipeline to modify.
     * @param handlerName The name of the handler to remove.
     */
    private void removeHandlerIfPresent(ChannelPipeline pipeline, String handlerName) {
        if (pipeline.context(handlerName) != null) {
            pipeline.remove(handlerName);
        }
    }

    /**
     * Removes a handler by class if it exists in the pipeline.
     *
     * @param pipeline     The ChannelPipeline to modify.
     * @param handlerClass The class of the handler to remove.
     */
    private void removeHandlerIfPresent(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerClass) {
        if (pipeline.context(handlerClass) != null) {
            pipeline.remove(handlerClass);
        }
    }
}