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

import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.openliberty.http.pipeline.configurators.PipelineHandlerUtility;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

/**
 * Https11Configurator sets up a ChannelPipeline for HTTPS (HTTP/1.1 over TLS).
 * 
 * Responsibilities:
 * - Secure the connection by adding an SSL handler at the start of the pipeline.
 * - Reuse the HTTP/1.1 configuration logic (via Http11Configurator) after establishing TLS.
 * - Ensure that HTTP/1.1 handlers (e.g., CRLF validation, HTTP codec, dispatcher)
 *   integrate seamlessly on top of the secure channel.
 * - Configure the pipeline to allow half-closure, if desired.
 * 
 */
public class Https11Configurator implements PipelineConfigurator {

    private final NettyHttpChannelConfig httpConfig;
    private final NettyTlsProvider tlsProvider;
    private final EndPointInfo endpointInfo;
    private final Map<String, Object> sslOptions;

    /**
     * Constructs an Https11Configurator.
     *
     * @param httpConfig   The HTTP channel configuration with server-specific settings.
     * @param tlsProvider  The TLS provider responsible for creating SSL contexts.
     * @param endpointInfo Information about the current endpoint (host and port).
     * @param sslOptions   A map of SSL options retrieved from configOptions.get(ConfigElement.SSL_OPTIONS).
     */
    public Https11Configurator(NettyHttpChannelConfig httpConfig,
                               NettyTlsProvider tlsProvider,
                               EndPointInfo endpointInfo,
                               Map<String, Object> sslOptions) {
        this.httpConfig = httpConfig;
        this.tlsProvider = tlsProvider;
        this.endpointInfo = endpointInfo;
        this.sslOptions = sslOptions;
    }

    /**
     * Configures the ChannelPipeline for HTTPS (HTTP/1.1 over TLS):
     * 1. Initialize TLS by adding an SSL handler at the pipeline's start.
     * 2. Mark the channel as secure.
     * 3. Leverage the Http11Configurator to add HTTP/1.1 logic (CRLF validation, codec, dispatcher, etc.)
     * 4. Add pre-codec and pre-dispatcher handlers (logging, keep-alive, request aggregation)
     *    just as we would for a plain HTTP/1.1 configuration.
     * 5. Allow half-closure if desired. In most HTTPS scenarios, this setting can remain consistent
     *    with HTTP/1.1 behavior.
     *
     * @param pipeline The ChannelPipeline to configure.
     * @throws NettyException if SSL context creation or pipeline modifications fail.
     */
    @Override
    public void configure(ChannelPipeline pipeline) throws NettyException {
        // Secure the channel first
        addSslHandler(pipeline);

        // Mark the channel as secure
        pipeline.channel().attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);

        // Reuse the HTTP/1.1 configuration on top of the secure channel
        new Http11Configurator(httpConfig).configure(pipeline);

        // Adjust options if necessary. HTTP/1.1 over TLS often supports half-closure.
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);
    }

    /**
     * Creates and adds the SSL handler to the pipeline.
     * This method encapsulates SSL context retrieval and engine creation
     *
     * @param pipeline The ChannelPipeline to which the SSL handler will be added.
     * @throws NettyException if the SSL context cannot be created or the TLS provider is null.
     */
    private void addSslHandler(ChannelPipeline pipeline) throws NettyException {
        SslContext context = getSslContext();
        SSLEngine engine = context.newEngine(pipeline.channel().alloc());

        // Insert the SSL handler at the pipeline's start, ensuring all inbound/outbound traffic is secure.
        pipeline.addFirst(PipelineHandlerUtility.HTTP_SSL_HANDLER_NAME, new LibertySslHandler(engine, httpConfig));
    }

    /**
     * Retrieves the SSL context from the TLS provider for this endpoint.
     * If the TLS provider is not available or the SSL context cannot be created, a NettyException is thrown.
     *
     * @return A valid SslContext for inbound SSL/TLS connections.
     * @throws NettyException if the TLS provider is missing or fails to produce an SslContext.
     */
    private SslContext getSslContext() throws NettyException {
        if (tlsProvider == null) {
            throw new NettyException("TLS Provider is not loaded for endpoint " 
                                     + endpointInfo.getHost() + ":" + endpointInfo.getPort());
        }

        String host = endpointInfo.getHost();
        String port = Integer.toString(endpointInfo.getPort());
        SslContext context = tlsProvider.getInboundSSLContext(sslOptions, host, port);

        if (context == null) {
            throw new NettyException("Failed to create SSL context for endpoint: " + host + ":" + port);
        }

        return context;
    }
}
