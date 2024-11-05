/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound;

import java.util.List;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;

/**
 * A Netty channel handler that combines multiple Cookie headers into a single header.
 * 
 * According to RFC6265, an HTTP request should contain at most one Cookie header, with
 * individual cookies separated by semicolons. This handler checks for multiple Cookie headers,
 * and combines them into a single header. This ensures complliance with the HTTP specification.
 */
public class CombinedCookieHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            List<String> cookieHeaders = request.headers().getAll(HttpHeaderNames.COOKIE);
            if (cookieHeaders.size() > 1) {
                String combinedCookies = String.join("; ", cookieHeaders);
                request.headers().remove(HttpHeaderNames.COOKIE);
                request.headers().set(HttpHeaderNames.COOKIE, combinedCookies);
            }
        }
        ctx.fireChannelRead(msg);
    }
}
