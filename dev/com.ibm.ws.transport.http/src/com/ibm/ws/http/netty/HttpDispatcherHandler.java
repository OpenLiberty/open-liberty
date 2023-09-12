/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.Objects;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Exception.StreamException;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;

/**
 *
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    HttpDispatcherLink link;
    HttpChannelConfig config;

    public static final String HTTP_DISPATCHER_HANDLER_NAME = "HTTP_DISPATCHER";

    public HttpDispatcherHandler(HttpChannelConfig config) {

        Objects.requireNonNull(config);
        this.config = config;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("Channel read fired from dispatcher!!");
        newRequest(context, request);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        System.out.println("Got exception from dispatcher now!!");
        System.out.println(cause.getClass().getCanonicalName());
        System.out.println(cause.getSuppressed());
        if (cause instanceof Http2Exception.StreamException) {
            System.out.println("Got a HTTP2 stream exception!! Need to close the stream");
            StreamException c = (Http2Exception.StreamException) cause;
            HttpToHttp2ConnectionHandler handler = context.pipeline().get(HttpToHttp2ConnectionHandler.class);
            Http2Connection connection = handler.connection();
//            handler.closeStream(connection.stream(c.streamId()), new VoidChannelPromise(context.channel(), true));
            connection.stream(c.streamId()).close();
            cause.printStackTrace();
            return;
        }
        cause.printStackTrace();
        context.close();
    }

    public void newRequest(ChannelHandlerContext context, FullHttpRequest request) {
        link = new HttpDispatcherLink();
        link.init(context, request, config);
        link.ready();
    }

}
