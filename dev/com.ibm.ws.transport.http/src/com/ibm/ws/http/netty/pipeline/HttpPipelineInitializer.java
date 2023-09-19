/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.SSLEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.NettyConfigBuilder;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.TransportInboundHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.openliberty.netty.internal.ChannelInitializerWrapper;

/**
 * Initializes a Netty Pipeline for an HTTP Endpoint. Configuration options may be
 * passed into it.
 */
public class HttpPipelineInitializer extends ChannelInitializerWrapper {

    private static final TraceComponent tc = Tr.register(HttpPipelineInitializer.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public enum ConfigElement {
        HTTP_OPTIONS,
        SSL_OPTIONS,
        REMOTE_IP,
        COMPRESSION,
        SAMESITE,
        HEADERS,
        ACCESS_LOG
    }

    NettyChain chain;
    Map<String, Object> tcpOptions;
    Map<String, Object> sslOptions;
    Map<String, Object> httpOptions;
    Map<String, Object> remoteIp;
    Map<String, Object> compression;
    Map<String, Object> samesite;
    Map<String, Object> headers;
    Map<String, Object> endpointOptions;

    NettyHttpChannelConfig httpConfig;

    public static final String NO_UPGRADE_OCURRED_HANDLER_NAME = "UPGRADE_HANDLER_CHECK";
    public static final String NETTY_HTTP_SERVER_CODEC = "HTTP_SERVER_HANDLER";
    public static final String HTTP_DISPATCHER_HANDLER_NAME = "HTTP_DISPATCHER";
    public static final String HTTP_SSL_HANDLER_NAME = "SSL_HANDLER";
    public static final String HTTP_KEEP_ALIVE_HANDLER_NAME = "httpKeepAlive";

    private HttpPipelineInitializer(HttpPipelineBuilder builder) {
        Objects.requireNonNull(builder);
        this.chain = builder.chain;
        this.httpConfig = builder.httpConfig;
    }

    public void updateConfig(ConfigElement config, Map<String, Object> options) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(options);
        Tr.entry(tc, "updateConfig");

        switch (config) {
            case COMPRESSION: {
                if (!HttpConfigConstants.DEFAULT_COMPRESSION.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID)))) {
                    this.httpConfig.updateConfig(config, options);

                }
                break;

            }
            case HEADERS: {
                if (!HttpConfigConstants.DEFAULT_HEADERS.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID)))) {
                    MSP.log("updating headers config...");
                    this.httpConfig.updateConfig(config, options);

                }
                break;
            }
            case HTTP_OPTIONS: {
                this.httpConfig.updateConfig(config, options);

                break;
            }
            case REMOTE_IP: {
                if (!HttpConfigConstants.DEFAULT_REMOTE_IP.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID)))) {

                    this.httpConfig.updateConfig(config, options);
                }
                break;
            }
            case SAMESITE: {

                if (!HttpConfigConstants.DEFAULT_SAMESITE.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID)))) {
                    MSP.log("updating samesite config");
                    this.httpConfig.updateConfig(config, options);
                }
                break;
            }
            default:
                break;

        }
        Tr.exit(tc, "updateConfig");
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        Tr.entry(tc, "initChannel");

        ChannelPipeline pipeline = channel.pipeline();

        // Initialize with the parent bootstrap initializer
        this.chain.getBootstrap().getBaseInitializer().init(channel);

        if (chain.isHttps()) {

            if (chain.isHttp2Enabled()) { // h2 setup starts here
                // Need to setup ALPN
                setupH2Pipeline(pipeline);
            } else { // https setup starts here
                // No ALPN because only HTTP1.1, just need to add SSL handler here
                setupHttpsPipeline(pipeline);
            }
        } else {
            if (chain.isHttp2Enabled()) { //h2c setup starts here
                setupH2cPipeline(pipeline);
            } else { // http 1.1 setup starts here
                setupHttp11Pipeline(pipeline);

            }

        }

//        pipeline.remove(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME);

        Tr.exit(tc, "initChannel");
    }

    /**
     * Utility method for building an H2 pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupH2Pipeline(ChannelPipeline pipeline) {
        throw new UnsupportedOperationException("H2 not yet configured!!");
    }

    /**
     * Utility method for building and H2C pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupH2cPipeline(ChannelPipeline pipeline) {
//        pipeline.addLast(NETTY_HTTP_SERVER_CODEC, new HttpServerCodec());
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addH2CCodecHandlers(pipeline);
        addPreHttpCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline);
    }

    /**
     * Utility method for building an HTTP1.1 pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttp11Pipeline(ChannelPipeline pipeline) {
        pipeline.addLast(NETTY_HTTP_SERVER_CODEC, new HttpServerCodec());
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline);
    }

    /**
     * Utility method for building and HTTPS pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttpsPipeline(ChannelPipeline pipeline) {
        SslContext context = chain.getOwner().getNettyTlsProvider().getInboundSSLContext(chain.getOwner().getSslOptions(), chain.getActiveHost(),
                                                                                         Integer.toString(chain.getActivePort()));
        Channel channel = pipeline.channel();
        if (context == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(this, tc, "initChannel", "Error adding TLS Support");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "initChannel");
            channel.close();
            return;
        }
        SSLEngine engine = context.newEngine(channel.alloc());
        pipeline.addFirst(HTTP_SSL_HANDLER_NAME, new SslHandler(engine, false));
        channel.attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);
        setupHttp11Pipeline(pipeline);
    }

    /**
     * Utility method for adding the Netty handlers needed for h2c connections
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addH2CCodecHandlers(ChannelPipeline pipeline) {
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler = LibertyUpgradeCodec.createCleartextUpgradeHandler(httpConfig, pipeline.channel());

        if (pipeline.names().contains(NETTY_HTTP_SERVER_CODEC))
            pipeline.addAfter(NETTY_HTTP_SERVER_CODEC, "H2C_UPGRADE_HANDLER", cleartextHttp2ServerUpgradeHandler);
        else
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, "H2C_UPGRADE_HANDLER", cleartextHttp2ServerUpgradeHandler);

        // TODO: Remove this, temp to see how upgrades are going
//        pipeline.addAfter("H2C_UPGRADE_HANDLER", "Upgrade Detector", new SimpleUserEventChannelHandler<UpgradeEvent>() {
//
//            @Override
//            protected void eventReceived(ChannelHandlerContext ctx, UpgradeEvent evt) throws Exception {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(this, tc, "Got upgrade event for channel " + ctx.channel(), evt);
//                }
//                ctx.fireUserEventTriggered(evt.retain());
//            }
//        });
//        pipeline.addAfter("Upgrade Detector", "Prior Knowledge Upgrade Detector", new SimpleUserEventChannelHandler<PriorKnowledgeUpgradeEvent>() {
//
//            @Override
//            protected void eventReceived(ChannelHandlerContext ctx, PriorKnowledgeUpgradeEvent evt) throws Exception {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(this, tc, "Got priorKnowledge upgrade event for channel " + ctx.channel(), evt);
//                }
//                ctx.fireUserEventTriggered(evt);
//            }
//        });

        // Handler to decide if an upgrade occurred or not and to add HTTP1 handlers on top
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, NO_UPGRADE_OCURRED_HANDLER_NAME, new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP 1.1.
                // TODO Check if we need to add common handlers here
//                addPreDispatcherHandlers(ctx.pipeline());
                System.out.println("Firing channel read from " + NO_UPGRADE_OCURRED_HANDLER_NAME + " to pipeline: ");
                pipeline.names().forEach(handler -> System.out.println(handler));
//                ctx.pipeline().remove("H2C_UPGRADE_HANDLER");
                ctx.pipeline().remove(HttpServerUpgradeHandler.class);
                // TODO Check if we need to retain this before passing to dispatcher
                ctx.fireChannelRead(msg);
                // Remove unused handlers
                ctx.pipeline().remove(NO_UPGRADE_OCURRED_HANDLER_NAME);
            }
        });
    }

    /**
     * Utility method for adding all the handlers that need to go before the HTTP Server codec
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addPreHttpCodecHandlers(ChannelPipeline pipeline) {
        if (httpConfig.isAccessLoggingEnabled()) {
            if (pipeline.names().contains(NETTY_HTTP_SERVER_CODEC))
                pipeline.addBefore(NETTY_HTTP_SERVER_CODEC, null, new AccessLoggerHandler(httpConfig));
            else
                pipeline.addLast(new AccessLoggerHandler(httpConfig));
        }
    }

    /**
     * Utility method for adding all the handlers that need to go just before the HTTP Dispatcher Handler
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addPreDispatcherHandlers(ChannelPipeline pipeline) {
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new HttpObjectAggregator(64 * 1024));
//        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new ChunkedWriteHandler());
        // if (httpConfig.useAutoCompression()) {
        //   pipeline.addLast(new NettyHttpContentCompressor());
        //}
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new ByteBufferCodec());
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new TransportInboundHandler(httpConfig));
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new TransportOutboundHandler(httpConfig));
        if (httpConfig.useForwardingHeaders()) {
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new RemoteIpHandler(httpConfig));
        }
    }

    public static class HttpPipelineBuilder {

        private final NettyChain chain;

        private Map<String, Object> compression;
        private Map<String, Object> headers;
        private Map<String, Object> httpOptions;
        private Map<String, Object> remoteIp;
        private Map<String, Object> samesite;

        NettyHttpChannelConfig httpConfig;

        private boolean useCompression;
        private boolean useHeaders;
        private boolean useRemoteIp;
        private boolean useSameSite;

        public HttpPipelineBuilder(NettyChain chain) {
            this.chain = chain;
            httpOptions = Collections.emptyMap();
            remoteIp = Collections.emptyMap();
            compression = Collections.emptyMap();
            samesite = Collections.emptyMap();
            headers = Collections.emptyMap();

        }

        public HttpPipelineBuilder with(ConfigElement config, Map<String, Object> options) {
            Tr.entry(tc, "with");
            if (Objects.isNull(config) || Objects.isNull(options)) {
                Tr.debug(tc, "with", "A bad configuration was attempted. Config: " + config + " options: " + options);
                return this;
            }

            switch (config) {

                case COMPRESSION: {
                    useCompression = HttpConfigConstants.DEFAULT_COMPRESSION.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID))) ? Boolean.FALSE : Boolean.TRUE;
                    this.compression = options;
                    break;
                }

                case HEADERS: {
                    useHeaders = HttpConfigConstants.DEFAULT_HEADERS.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID))) ? Boolean.FALSE : Boolean.TRUE;
                    this.headers = options;
                    break;
                }

                case HTTP_OPTIONS: {
                    this.httpOptions = options;
                    break;
                }
                case REMOTE_IP: {

                    useRemoteIp = HttpConfigConstants.DEFAULT_REMOTE_IP.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID))) ? Boolean.FALSE : Boolean.TRUE;
                    this.remoteIp = options;

                    break;
                }
                case SAMESITE: {

                    useSameSite = HttpConfigConstants.DEFAULT_SAMESITE.equalsIgnoreCase(String.valueOf(options.get(HttpConfigConstants.ID))) ? Boolean.FALSE : Boolean.TRUE;

                    MSP.log("Pipeline Samesite enabled: " + useSameSite);
                    this.samesite = options;
                    break;
                }

                default:
                    break;
            }
            Tr.exit(tc, "with");
            return this;
        }

        private NettyHttpChannelConfig generateHttpOptions() {
            Tr.entry(tc, "generateHttpOptions");

            NettyConfigBuilder builder = new NettyHttpChannelConfig.NettyConfigBuilder();

            if (Objects.nonNull(httpOptions)) {

                builder.with(ConfigElement.HTTP_OPTIONS, this.httpOptions);

            }

            if (useCompression) {
                builder.with(ConfigElement.COMPRESSION, compression);
            }

            if (useHeaders) {
                builder.with(ConfigElement.HEADERS, headers);
            }

            if (useRemoteIp) {
                builder.with(ConfigElement.REMOTE_IP, remoteIp);
            }

            if (useSameSite) {
                builder.with(ConfigElement.SAMESITE, samesite);
                MSP.log("Building config with samesite");
            }
            Tr.exit(tc, "generateHttpOptions");

            return builder.build();
        }

        public HttpPipelineInitializer build() {

            this.httpConfig = generateHttpOptions();

            return new HttpPipelineInitializer(this);
        }
    }

}
