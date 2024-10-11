/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.debug;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 *
 */
public class DebugHandler extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            logHttpResponse(response);
        } else if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            logHttpRequest(request);
        } else {
            logOtherMessage(msg);
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            logHttpRequest(request);
        } else if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            logHttpResponse(response);
        } else {
            logOtherMessage(msg);
        }
        super.channelRead(ctx, msg);
    }

    private void logHttpResponse(HttpResponse response) {
        System.out.println("HTTP Response:");
        System.out.println("Status: " + response.status());
        System.out.println("Headers: ");
        response.headers().forEach(header -> System.out.println(header.getKey() + ": " + header.getValue()));
    }

    private void logHttpRequest(HttpRequest request) {
        System.out.println("HTTP Request:");
        System.out.println("Method: " + request.method());
        System.out.println("URI: " + request.uri());
        System.out.println("Headers: ");
        request.headers().forEach(header -> System.out.println(header.getKey() + ": " + header.getValue()));
    }

    private void logOtherMessage(Object msg) {
        System.out.println("Other Message: " + msg.getClass().getName());
    }
}
