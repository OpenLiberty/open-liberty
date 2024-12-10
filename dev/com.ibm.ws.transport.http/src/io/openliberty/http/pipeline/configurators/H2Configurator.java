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

import java.util.Map;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.LibertySslHandler;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyNettyALPNHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.ssl.SslContext;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

/**
 * H2Configurator sets up a ChannelPipeline for HTTP/2 over TLS (ALPN).
 *
 * Responsibilities:
 * - Establish a secure channel by adding an SSL handler first.
 * - Insert an ALPN handler (LibertyNettyALPNHandler) to negotiate HTTP/2 vs HTTP/1.1 dynamically.
 * - Add the HTTP dispatcher once the protocol is decided.
 * - Use PipelineHandlerUtility methods to insert pre-dispatcher handlers that ensure consistent behavior.
 * - Disable half-closure as is typical for HTTP/2.
 */
public class H2Configurator implements PipelineConfigurator {

    private final NettyHttpChannelConfig httpConfig;
    private final NettyTlsProvider tlsProvider;
    private final EndPointInfo endpointInfo;
    private final Map<String, Object> sslOptions;

    /**
     * Constructs an H2Configurator.
     *
     * @param httpConfig   The HTTP channel configuration with server-specific settings.
     * @param tlsProvider  The TLS provider responsible for creating ALPN-enabled SSL contexts.
     * @param endpointInfo Information about the current endpoint (host and port).
     * @param sslOptions   The map of SSL options retrieved from the configuration, used to obtain the SslContext.
     */
    public H2Configurator(NettyHttpChannelConfig httpConfig,
                          NettyTlsProvider tlsProvider,
                          EndPointInfo endpointInfo,
                          Map<String, Object> sslOptions) {
        this.httpConfig = httpConfig;
        this.tlsProvider = tlsProvider;
        this.endpointInfo = endpointInfo;
        this.sslOptions = sslOptions;
    }

    @Override
    public void configure(ChannelPipeline pipeline) throws NettyException {
        // Secure the channel first by adding an SSL handler
        addSslHandler(pipeline);

        // Add ALPN handler to negotiate protocols (HTTP/2 vs HTTP/1.1)
        pipeline.addLast(PipelineHandlerUtility.HTTP_ALPN_HANDLER_NAME, new LibertyNettyALPNHandler(httpConfig));

        // Add the dispatcher for handling requests after ALPN negotiation
        pipeline.addLast(PipelineHandlerUtility.HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));

        // Add pre-HTTP codec handlers if any (e.g., access logging)
        // Even though HTTP/2 doesn't use the HTTP server codec, this method is harmless if no logging is enabled.
        PipelineHandlerUtility.addPreHttpCodecHandlers(pipeline, httpConfig);

        // Add pre-dispatcher handlers for HTTP/2 scenario
        PipelineHandlerUtility.addPreDispatcherHandlers(pipeline, true, httpConfig);

        // Mark the channel as secure since we're using TLS
        pipeline.channel().attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);

        // HTTP/2 typically disallows half-closure
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }

    /**
     * Adds the SSL handler to the pipeline.
     * Obtains an ALPN-capable SSL context from the TLS provider and creates an SSLEngine.
     * The SSL handler is then inserted at the front of the pipeline, securing all traffic.
     *
     * @param pipeline The ChannelPipeline to which the SSL handler will be added.
     * @throws NettyException if the SSL context cannot be created or if the TLS provider is null.
     */
    private void addSslHandler(ChannelPipeline pipeline) throws NettyException {
        SslContext context = getSslContext();
        SSLEngine engine = context.newEngine(pipeline.channel().alloc());
        pipeline.addFirst(PipelineHandlerUtility.HTTP_SSL_HANDLER_NAME, new LibertySslHandler(engine, httpConfig));
    }

    /**
     * Retrieves the ALPN-enabled SSL context from the TLS provider for this endpoint.
     * If the TLS provider is not available or the SSL context cannot be created, a NettyException is thrown.
     *
     * @return A valid ALPN-capable SslContext for inbound TLS connections.
     * @throws NettyException if the TLS provider is missing or fails to produce an SslContext.
     */
    private SslContext getSslContext() throws NettyException {
        if (tlsProvider == null) {
            throw new NettyException("TLS Provider is not loaded for endpoint " 
                                     + endpointInfo.getHost() + ":" + endpointInfo.getPort());
        }

        String host = endpointInfo.getHost();
        String port = Integer.toString(endpointInfo.getPort());
        SslContext context = tlsProvider.getInboundALPNSSLContext(sslOptions, host, port);

        if (context == null) {
            throw new NettyException("Failed to create ALPN SSL context for endpoint: " + host + ":" + port);
        }

        return context;
    }
}