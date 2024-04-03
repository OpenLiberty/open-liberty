/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.http2;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 * ALPN Handler for negotiating what protocol to use
 */
public class LibertyNettyALPNHandler extends ApplicationProtocolNegotiationHandler {

    private static final TraceComponent tc = Tr.register(LibertyNettyALPNHandler.class);

    private final NettyHttpChannelConfig httpConfig;

    /**
     * Default to HTTP 2.0 for now
     */
    public LibertyNettyALPNHandler(NettyHttpChannelConfig httpConfig) {
//        super(ApplicationProtocolNames.HTTP_2);
        super(ApplicationProtocolNames.HTTP_1_1);
        this.httpConfig = httpConfig;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        System.out.println("Configuring pipeline!! " + protocol);
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configuring pipeline with HTTP 2 for incoming connection " + ctx.channel());
            }
            LibertyUpgradeCodec codec = new LibertyUpgradeCodec(httpConfig, ctx.channel());
            HttpToHttp2ConnectionHandler handler = codec.buildHttp2ConnectionHandler(httpConfig, ctx.channel());
            // HTTP2 to HTTP 1.1 and back pipeline
            ctx.pipeline().addAfter(HttpPipelineInitializer.HTTP_ALPN_HANDLER_NAME, null, handler);
//            ctx.pipeline().replace(this, null, handler);
            System.out.println("Configured H2 pipeline with " + ctx.pipeline().names());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configured pipeline with " + ctx.pipeline().names());
            }
            return;
        }
        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configuring pipeline with HTTP 1.1 for incoming connection " + ctx.channel());
            }
            ctx.pipeline().addAfter(HttpPipelineInitializer.HTTP_ALPN_HANDLER_NAME, HttpPipelineInitializer.NETTY_HTTP_SERVER_CODEC, new HttpServerCodec());
            ctx.pipeline().addAfter(HttpPipelineInitializer.NETTY_HTTP_SERVER_CODEC, HttpPipelineInitializer.HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
            //TODO: this is a very large number, check best practice
            ctx.pipeline().addAfter(HttpPipelineInitializer.HTTP_KEEP_ALIVE_HANDLER_NAME, null,
                                    new LibertyHttpObjectAggregator(httpConfig.getMessageSizeLimit() == -1 ? HttpPipelineInitializer.maxContentLength : httpConfig.getMessageSizeLimit()));
            System.out.println("Configured Http1 pipeline with " + ctx.pipeline().names());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configured pipeline with " + ctx.pipeline().names());
            }
            return;
        }
        System.out.println("Oh no pipeline unconfigured for protocol: " + protocol + "!");
        throw new IllegalStateException("unknown protocol: " + protocol);
    }

}
