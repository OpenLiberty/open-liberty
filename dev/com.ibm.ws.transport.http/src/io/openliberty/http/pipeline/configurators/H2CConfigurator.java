/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.pipeline.configurators;

import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.netty.internal.exception.NettyException;

import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent;

/**
 * H2CConfigurator sets up a ChannelPipeline for HTTP/2 cleartext (H2C).
 *
 * Responsibilities:
 * - Start with an HTTP dispatcher.
 * - Add a cleartext upgrade handler that attempts to upgrade from HTTP/1.1 to HTTP/2.
 * - If no upgrade occurs, fall back to HTTP/1.1 by adding the necessary handlers via PipelineHandlerUtility.
 * - Disable half-closure by default, only re-enabling it if the pipeline falls back to HTTP/1.1.
 */
public class H2CConfigurator implements PipelineConfigurator {

    private static final String HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME = "H2C_UPGRADE_HANDLER";
    private static final String NO_UPGRADE_OCCURRED_HANDLER_NAME = "UPGRADE_HANDLER_CHECK";

    private final NettyHttpChannelConfig httpConfig;

    /**
     * Constructs an H2CConfigurator.
     *
     * @param httpConfig The HTTP channel configuration with server-specific settings.
     */
    public H2CConfigurator(NettyHttpChannelConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    @Override
    public void configure(ChannelPipeline pipeline) throws NettyException {
        // Start with the dispatcher handler
        pipeline.addLast(PipelineHandlerUtility.HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));

        // Add pre-HTTP codec handlers (e.g., access logging if enabled)
        PipelineHandlerUtility.addPreHttpCodecHandlers(pipeline, httpConfig);

        // Create the cleartext upgrade handler for HTTP/2
        CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler =
                LibertyUpgradeCodec.createCleartextUpgradeHandler(httpConfig, pipeline.channel());

        pipeline.addBefore(PipelineHandlerUtility.HTTP_DISPATCHER_HANDLER_NAME, 
                           HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME, cleartextHttp2ServerUpgradeHandler);

        // Add a fallback handler to handle the scenario when no HTTP/2 upgrade occurs
        pipeline.addBefore(PipelineHandlerUtility.HTTP_DISPATCHER_HANDLER_NAME, 
                           NO_UPGRADE_OCCURRED_HANDLER_NAME, new H2CNoUpgradeFallbackHandler(httpConfig));

        // By default, disable half-closure for H2C until fallback occurs
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);

        // Add pre-dispatcher handlers for HTTP/2 scenario
        PipelineHandlerUtility.addPreDispatcherHandlers(pipeline, true, httpConfig);
    }

    /**
     * H2CNoUpgradeFallbackHandler handles the scenario where the HTTP/2 upgrade does not occur.
     * It uses PipelineHandlerUtility methods to set up HTTP/1.1 handlers (keep-alive, aggregator, request handler),
     * then removes itself from the pipeline.
     */
    static class H2CNoUpgradeFallbackHandler extends SimpleChannelInboundHandler<HttpMessage> {
        private final NettyHttpChannelConfig httpConfig;

        H2CNoUpgradeFallbackHandler(NettyHttpChannelConfig httpConfig) {
            this.httpConfig = httpConfig;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
            String protocol = ctx.pipeline().channel().attr(NettyHttpConstants.PROTOCOL).get();

            if ("HTTP2".equals(protocol)) {
                // Upgrade succeeded, just forward the message
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                return;
            }

            // No upgrade; fallback to HTTP/1.1
            ctx.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);

            // Add the HTTP/1.1 handlers using a shared utility method
            PipelineHandlerUtility.addHttp11FallbackHandlers(ctx.pipeline(), httpConfig);

            // Remove the fallback handler now that HTTP/1.1 is established
            ctx.pipeline().remove(NO_UPGRADE_OCCURRED_HANDLER_NAME);

            // Forward the message to the newly configured HTTP/1.1 pipeline
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof PriorKnowledgeUpgradeEvent) {
                // If prior knowledge upgrade succeeded, remove this fallback handler
                ctx.pipeline().remove(this);
            }
            super.userEventTriggered(ctx, evt);
        }
    }
}
