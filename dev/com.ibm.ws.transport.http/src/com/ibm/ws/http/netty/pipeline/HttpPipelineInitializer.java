/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.NettyConfigBuilder;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.LibertySslHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyNettyALPNHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;
import com.ibm.ws.http.netty.pipeline.inbound.TransportInboundHandler;
import com.ibm.ws.http.netty.pipeline.TransportOutboundHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.pipeline.configurators.H2CConfigurator;
import io.openliberty.http.pipeline.configurators.H2Configurator;
import io.openliberty.http.pipeline.configurators.Http11Configurator;
import io.openliberty.http.pipeline.configurators.Https11Configurator;
import io.openliberty.http.pipeline.configurators.PipelineConfigurator;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

/**
 * HttpPipelineInitializer selects and applies the appropriate pipeline configuration 
 * for a given channel based on whether the connection is HTTP/1.1, HTTPS, HTTP/2, or H2C.
 *
 * Responsibilities:
 * - Initialize the basic pipeline (via the chain's bootstrap initializer).
 * - Determine protocol (HTTP or HTTPS, HTTP/1.1 or HTTP/2) and select the right configurator.
 * - Provide necessary configuration maps, including sslOptions, to the chosen configurator.
 * - Keep logic focused on selection and delegation, letting protocol-specific configurators 
 *   handle the actual handler arrangement.
 */
public class HttpPipelineInitializer extends ChannelInitializerWrapper {

    private static final TraceComponent tc = Tr.register(HttpPipelineInitializer.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final NettyChain chain;
    private final NettyHttpChannelConfig httpConfig;
    private final NettyTlsProvider tlsProvider;
    private final EndPointInfo endpointInfo;
    private final Map<ConfigElement, Map<String, Object>> configOptions;

    /**
     * Constructor for HttpPipelineInitializer.
     *
     * @param chain         The NettyChain representing this connection flow.
     * @param httpConfig    The HTTP channel configuration.
     * @param configOptions The configuration map keyed by ConfigElement that may include SSL_OPTIONS, etc.
     */
    HttpPipelineInitializer(NettyChain chain, NettyHttpChannelConfig httpConfig,
                            Map<ConfigElement, Map<String, Object>> configOptions) {
        this.chain = chain;
        this.httpConfig = httpConfig;
        this.configOptions = configOptions;
        this.tlsProvider = chain.getOwner().getNettyTlsProvider();
        this.endpointInfo = chain.getEndpointInfo();

        httpConfig.registerAccessLog(chain.getOwner().getName());
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        Tr.entry(tc, "initChannel");

        ChannelPipeline pipeline = channel.pipeline();
        this.chain.getBootstrap().getBaseInitializer().init(channel);

        // Set channel attributes to mark inbound/outbound nature and endpoint PID
        channel.attr(NettyHttpConstants.IS_OUTBOUND_KEY).set(false);
        channel.attr(NettyHttpConstants.ENDPOINT_PID).set(chain.getEndpointPID());

        // Setup logging for receive buffers
        RecvByteBufAllocator channelAllocator = channel.config().getRecvByteBufAllocator();
        LoggingRecvByteBufAllocator loggingAllocator = new LoggingRecvByteBufAllocator(channelAllocator);
        channel.config().setRecvByteBufAllocator(loggingAllocator);

        pipeline.addLast("AllocatorContextSetter", new AllocatorContextSetter(loggingAllocator));

        // Remove any default inactivity timeout handler if present
        pipeline.remove(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME);

        // Determine the protocol scenario and pick the appropriate configurator
        PipelineConfigurator configurator = selectConfigurator();

        // Configure the pipeline using the chosen configurator
        configurator.configure(pipeline);
    }

    /**
     * Selects the appropriate PipelineConfigurator based on whether the connection is HTTPS and/or HTTP/2.
     *
     * @return The correct PipelineConfigurator for the current protocol scenario.
     * @throws NettyException if TLS is required but tlsProvider is not available, or if other config issues arise.
     */
    private PipelineConfigurator selectConfigurator() throws NettyException {
        boolean isHttps = chain.isHttps();
        boolean isHttp2 = chain.isHttp2Enabled();

        // If HTTPS or H2 is needed, we likely need SSL options
        Map<String, Object> sslOptions = null;
        if (isHttps) {
            sslOptions = configOptions.get(ConfigElement.SSL_OPTIONS);
            if (sslOptions == null && (isHttps || isHttp2)) {
                throw new NettyException("SSL options are required but not provided for endpoint: "
                                        + endpointInfo.getHost() + ":" + endpointInfo.getPort());
            }
        }

        if (isHttps && isHttp2) {
            // HTTP/2 over TLS (ALPN)
            if (tlsProvider == null) {
                throw new NettyException("TLS Provider not loaded for HTTP/2 over TLS");
            }
            return new H2Configurator(httpConfig, tlsProvider, endpointInfo, sslOptions);
        } else if (isHttps) {
            // HTTPS/1.1
            if (tlsProvider == null) {
                throw new NettyException("TLS Provider not loaded for HTTPS/1.1");
            }
            return new Https11Configurator(httpConfig, tlsProvider, endpointInfo, sslOptions);
        } else if (isHttp2) {
            // H2C (cleartext HTTP/2)
            // No SSL required here
            return new H2CConfigurator(httpConfig);
        } else {
            // Plain HTTP/1.1
            return new Http11Configurator(httpConfig);
        }
    }

    public static class HttpPipelineBuilder {

        private final NettyChain chain;
        private final EnumMap<ConfigElement, Map<String, Object>> configOptions = new EnumMap<>(ConfigElement.class);
        private final Set<ConfigElement> activeConfigs = EnumSet.noneOf(ConfigElement.class);


        public HttpPipelineBuilder(NettyChain chain) {
            this.chain = Objects.requireNonNull(chain, "Netty chain cannot be null");
        }

        public HttpPipelineBuilder with(ConfigElement config, Map<String, Object> options) {
            Objects.requireNonNull(config, "ConfigElement cannot be null");
            Objects.requireNonNull(options, "Options cannot be null");

            String id = String.valueOf(options.get(HttpConfigConstants.ID));

            if (config == ConfigElement.SSL_OPTIONS || config == ConfigElement.HTTP_OPTIONS){
                configOptions.put(config, options);
                activeConfigs.add(config);
            } else if (!isDefaultConfig(config, id)){
                configOptions.put(config, options);
                activeConfigs.add(config);
            }

            return this;
        }

        private boolean isDefaultConfig(ConfigElement config, String id){
            switch (config) {
                case HTTP_OPTIONS:
                    return "defaultHttpOptions".equalsIgnoreCase(id);
                case REMOTE_IP:
                    return "defaultRemoteIp".equalsIgnoreCase(id);
                case COMPRESSION:
                    return "defaultCompression".equalsIgnoreCase(id);
                case SAMESITE:
                    return "defaultSameSite".equalsIgnoreCase(id);
                case HEADERS:
                    return "defaultHeaders".equalsIgnoreCase(id);
                case SSL_OPTIONS:
                    return "defaultSSLOptions".equalsIgnoreCase(id);
                default:
                    return false;
            }
        }

        public HttpPipelineInitializer build() {

            NettyHttpChannelConfig.NettyConfigBuilder configBuilder = new NettyHttpChannelConfig.NettyConfigBuilder();
            for (ConfigElement element : ConfigElement.values()) {

                if (activeConfigs.contains(element)) {
                    configBuilder.with(element, configOptions.get(element));
                }
            }

            NettyHttpChannelConfig httpConfig = configBuilder.build();


            return new HttpPipelineInitializer(chain, httpConfig, configOptions);
        }
    }

    /**
     * Clears configuration from the underlying httpConfig if needed.
     */
    public void clearConfig() {
        this.httpConfig.clear();

    }

    /**
     * ConfigElements represent different configuration aspects. SSL_OPTIONS is the one needed for TLS contexts.
     */
    public enum ConfigElement {
        HTTP_OPTIONS,
        SSL_OPTIONS,
        REMOTE_IP,
        COMPRESSION,
        SAMESITE,
        HEADERS,
        ACCESS_LOG
    }

    /**
     * AllocatorContextSetter associates the channel's RecvByteBufAllocator with the handler context,
     * enabling logging and debugging of inbound buffer allocations.
     */
    @Sharable
    private static class AllocatorContextSetter extends ChannelInboundHandlerAdapter{
        private final LoggingRecvByteBufAllocator loggingAllocator;

        AllocatorContextSetter(LoggingRecvByteBufAllocator loggingAllocator){
            this.loggingAllocator = loggingAllocator;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext context) throws Exception{
            super.handlerAdded(context);

            RecvByteBufAllocator.Handle handle = context.channel().unsafe().recvBufAllocHandle();
            if(handle instanceof LoggingRecvByteBufAllocator.LoggingHandle){
                ((LoggingRecvByteBufAllocator.LoggingHandle) handle).setChannelHandlerContext(context);
            }
        }
    }

}
