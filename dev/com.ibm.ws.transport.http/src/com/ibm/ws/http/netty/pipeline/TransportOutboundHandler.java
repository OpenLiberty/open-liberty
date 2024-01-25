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
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.openliberty.netty.internal.tcp.InactivityTimeoutHandler;

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

        MSP.log(Thread.currentThread().getStackTrace().toString());

        MSP.log("Writing outbound, msg is: " + msg);
        //TODO: only if first time running through here (persist needs to clear)

        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            HeaderHandler headerHandler = new HeaderHandler(config, response);
            headerHandler.complianceCheck();

            if (HttpUtil.isContentLengthSet(response)) {
                if (HttpUtil.isContentLengthSet(response)) {
                    MSP.log("Setting content length attribute");
                    ctx.channel().attr(NettyHttpConstants.CONTENT_LENGTH).set(Long.valueOf(HttpUtil.getContentLength(response)));
                }
            }

//            if (ctx.channel().hasAttr(NettyHttpConstants.ACCEPT_ENCODING)) {
//                String acceptEncoding = ctx.channel().attr(NettyHttpConstants.ACCEPT_ENCODING).get();
//                ResponseCompressionHandler compressionHandler = new ResponseCompressionHandler(config, response, acceptEncoding);
//                compressionHandler.process();
//                if (compressionHandler.getEncoding() != null) {
//                    MSP.log("setting compression attribute -> " + compressionHandler.getEncoding());
//                    ctx.channel().attr(NettyHttpConstants.COMPRESSION_ENCODING).set(compressionHandler.getEncoding());
//                }
//            }
            
            final boolean isSwitching = response.status() == HttpResponseStatus.SWITCHING_PROTOCOLS;

//            if (response.status() == HttpResponseStatus.SWITCHING_PROTOCOLS) {
////               
//                isSwitching = true;
//
//            }
            ChannelFuture future = ctx.writeAndFlush(msg);
            
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess() && isSwitching) {
                        // Handle the successful write operation
                        ctx.pipeline().remove("maxConnectionHandler");
                        // ctx.pipeline().remove("HTTP_SERVER_HANDLER");
                        ctx.pipeline().remove("chunkLoggingHandler");
                        ctx.pipeline().remove("chunkWriteHandler");
                        ctx.pipeline().remove(ByteBufferCodec.class);
                        ctx.pipeline().remove(TransportOutboundHandler.class);
                        ctx.pipeline().remove(InactivityTimeoutHandler.class);
                        ctx.pipeline().remove(HttpServerCodec.class);
                        if (ctx.pipeline().get(NettyServletUpgradeHandler.class) == null) {

                            NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(ctx.channel());
                            ctx.pipeline().addLast(upgradeHandler);
                        }
                        
                        System.out.println(ctx.pipeline().names());
                    }
                }
            });

        }

        else {

            if (msg instanceof ByteBuf) {
                // Optionally manipulate the data
                ByteBuf data = (ByteBuf) msg;
                // Proceed with writing
                super.write(ctx, data, promise);
            } else {
                // Forward to the next handler if not a ByteBuf
                super.write(ctx, msg, promise);

            }

            MSP.log("writeAndFlush for message: " + msg.toString());

            ctx.writeAndFlush(msg);
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        // Log or dump caller information before closing
        MSP.log("Intercepting close");
        Thread.dumpStack();
        Exception callerInfo = new Exception("Closing channel triggered by:");
        callerInfo.printStackTrace();
        promise.setFailure(new IllegalStateException("Channel close is prevented"));

        // Proceed with the actual close operation
        // super.close(ctx, promise);
    }

}
