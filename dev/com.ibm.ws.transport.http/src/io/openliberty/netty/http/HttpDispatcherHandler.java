/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.http;

import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Interface the Netty pipeline into the HTTP Dispatcher
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    HttpDispatcherLink link;

    public HttpDispatcherHandler() {

    }

    @Override
    public void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        link = new HttpDispatcherLink();
        link.init(context, request);
        link.ready();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        //TODO: log this exception
        context.close();
    }

}
