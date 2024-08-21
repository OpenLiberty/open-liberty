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

import java.util.Objects;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.http.netty.MSP;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Exception.StreamException;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.util.ReferenceCountUtil;

/**
 *
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

//    HttpDispatcherLink link;
    HttpChannelConfig config;

    public HttpDispatcherHandler(HttpChannelConfig config) {

        super(false);

        Objects.requireNonNull(config);
        this.config = config;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        // TODO Need to see if we need to check decoder result from request to ensure data is properly parsed as expected
        if (request.decoderResult().isFinished() && request.decoderResult().isSuccess()) {
//          FullHttpRequest msg = request.duplicate();
//          newRequest(context, request);
            FullHttpRequest msg = ReferenceCountUtil.retain(request, 1);
            HttpDispatcher.getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println("New request on pipeline for : " + context + " channel: " + context.channel());
                        newRequest(context, msg);
                    } catch (Throwable t) {
                        try {
                            exceptionCaught(context, t);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                            e.printStackTrace();
                            context.close();
                        }
                    } finally {
                        ReferenceCountUtil.release(msg);
                    }
                }
            });
        } else {
            System.out.println("Caught an unsuccesful decode while decoding result! " + request.decoderResult().cause() + " so will ignore message: " + request);
            if (request.decoderResult().cause() != null)
                request.decoderResult().cause().printStackTrace();
        }

    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("Writeability changed!!");
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        System.out.println("Closing context because of: ");
        cause.printStackTrace();
        if (cause instanceof Http2Exception.StreamException) {
            System.out.println("Got a HTTP2 stream exception!! Need to close the stream");
            StreamException c = (Http2Exception.StreamException) cause;
            HttpToHttp2ConnectionHandler handler = context.pipeline().get(HttpToHttp2ConnectionHandler.class);
            Http2Connection connection = handler.connection();
            connection.stream(c.streamId()).close();
            return;
        }
        context.close();
    }

    public void newRequest(ChannelHandlerContext context, FullHttpRequest request) {
        MSP.log("Shiny new dispatcher link");
        HttpDispatcherLink link = new HttpDispatcherLink();
        context.channel().closeFuture().addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture arg0) throws Exception {
                System.out.println("Closing link because channel was closed!! " + arg0.channel() + " future: " + arg0 + " done? " + arg0.isDone() + " cancelled? "
                                   + arg0.isCancelled() + " success? "
                                   + arg0.isSuccess());
                link.close(null, null);
            }
        });
        link.init(context, request, config);
        link.ready();

//        link = new HttpDispatcherLink();
//        link.init(context, request, config);
//        link.ready();
    }

}
