/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
import com.ibm.ws.http.netty.pipeline.CRLFValidationHandler;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.openliberty.http.pipeline.configurators.PipelineHandlerUtility;

/**
 * ALPN Handler for negotiating what protocol (HTTP/2 or HTTP/1.1) to use.
 * This class checks the negotiated protocol and reconfigures the pipeline accordingly.
 */
public class LibertyNettyALPNHandler extends ApplicationProtocolNegotiationHandler {

    private static final TraceComponent tc = Tr.register(LibertyNettyALPNHandler.class);

    private final NettyHttpChannelConfig httpConfig;

    /**
     * Defaults to HTTP/1.1 if no protocol is negotiated.
     */
    public LibertyNettyALPNHandler(NettyHttpChannelConfig httpConfig) {
        super(ApplicationProtocolNames.HTTP_1_1);
        this.httpConfig = httpConfig;
    }

     @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configuring pipeline with HTTP/2 for incoming connection " + ctx.channel());
            }

            // For HTTP/2 negotiation, we use a LibertyUpgradeCodec to build an Http2ConnectionHandler
            LibertyUpgradeCodec codec = new LibertyUpgradeCodec(httpConfig, ctx.channel());
            HttpToHttp2ConnectionHandler handler = codec.buildHttp2ConnectionHandler(httpConfig, ctx.channel());

            // Insert the HTTP/2 handler after the ALPN handler
            ctx.pipeline().addAfter(PipelineHandlerUtility.HTTP_ALPN_HANDLER_NAME, null, handler);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configured pipeline with " + ctx.pipeline().names());
            }
            return;
        }

        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configuring pipeline with HTTP/1.1 for incoming connection " + ctx.channel());
            }

            // Add the HTTP server codec after ALPN handler
            ctx.pipeline().addAfter(
                PipelineHandlerUtility.HTTP_ALPN_HANDLER_NAME,
                PipelineHandlerUtility.NETTY_HTTP_SERVER_CODEC,
                new HttpServerCodec(8192, Integer.MAX_VALUE, httpConfig.getIncomingBodyBufferSize())
            );

            // Add the dispatcher handler for HTTP/1.1 scenario
            ctx.pipeline().addLast(PipelineHandlerUtility.HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));

            // Now reuse the existing utility methods to add all HTTP/1.1 handlers
            // Pre-HTTP codec handlers (e.g., logging if enabled)
            PipelineHandlerUtility.addPreHttpCodecHandlers(ctx.pipeline(), httpConfig);

            // Pre-dispatcher handlers for HTTP/1.1 (keep-alive, aggregator, request handler)
            PipelineHandlerUtility.addPreDispatcherHandlers(ctx.pipeline(), false, httpConfig);

            // Allow half-closure for HTTP/1.1
            ctx.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configured pipeline with " + ctx.pipeline().names());
            }
            return;
        }

        // If neither HTTP/2 nor HTTP/1.1 was negotiated, this is unexpected
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Pipeline unconfigured for protocol " + protocol);
        }
        throw new IllegalStateException("Unknown protocol: " + protocol);
    }
}          