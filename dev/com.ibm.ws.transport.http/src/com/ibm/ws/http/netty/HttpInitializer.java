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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 *
 */
public class HttpInitializer extends ChannelInitializer<Channel> {

    private static final TraceComponent tc = Tr.register(HttpInitializer.class);

    NettyChain chain;

    public HttpInitializer(NettyChain chain) {
        this.chain = chain;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "starting a new http pipeline " + this);
        }

        ChannelPipeline pipeline = channel.pipeline();

        if (chain.isHttps()) {
            // SSLEngine engine = channel.newEngine(channel.alloc());
            // pipeline.addFirst("ssl", new SslHandler(engine, false));
        }

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new ByteBufferCodec());
        pipeline.addLast(new HttpDispatcherHandler());
    }

}
