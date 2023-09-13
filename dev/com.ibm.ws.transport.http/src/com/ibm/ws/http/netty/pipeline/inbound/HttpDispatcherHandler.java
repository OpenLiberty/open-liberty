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
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.http.netty.MSP;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 *
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    HttpDispatcherLink link;
    HttpChannelConfig config;

    public HttpDispatcherHandler(HttpChannelConfig config) {

        Objects.requireNonNull(config);
        this.config = config;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {

        newRequest(context, request);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
        MSP.log("Closing context because of: ");
        cause.printStackTrace();

        //context.close();
    }

    public void newRequest(ChannelHandlerContext context, FullHttpRequest request) {
        MSP.log("Shiny new dispatcher link");

        link = new HttpDispatcherLink();
        link.init(context, request, config);
        link.ready();
    }

}
