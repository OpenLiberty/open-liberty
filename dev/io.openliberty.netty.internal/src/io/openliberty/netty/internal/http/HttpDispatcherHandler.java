/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

/**
 * FullHttpRequest handler that passes complete HTTP requests to the Liberty HttpDispatcher
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    //private HttpDispatcherLink link;
    private final String responseMessage = "Hello from Open Liberty\n";

    public HttpDispatcherHandler() {
    }

    public void newRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        /*
         * TODO expose the HttpDispatcherLink and provide compatible hooks
         */
        //link = new HttpDispatcherLink();
        //link.init(ctx, request);
        //link.ready();

        // for now just write out a simple message
        FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, Unpooled.copiedBuffer(responseMessage, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 
                response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        newRequest(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //System.out.println("HttpDispatcherHandler.exceptionCaught:\n" + cause.getLocalizedMessage());
        ctx.close();
    }

}
