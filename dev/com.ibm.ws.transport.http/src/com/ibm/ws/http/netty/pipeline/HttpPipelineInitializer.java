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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.AccessLoggerHandler;
import com.ibm.ws.http.netty.HttpDispatcherHandler;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.NettyConfigBuilder;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeEvent;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
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

    HttpChannelConfig httpConfig;

    private final static String NO_UPGRADE_OCURRED_HANDLER_NAME = "UPGRADE_HANDLER_CHECK";
    private final static String NETTY_HTTP_SERVER_CODEC = "HTTP_SERVER_HANDLER";

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
            case HTTP_OPTIONS: {
                this.httpConfig.updateConfig(options);

                break;
            }
            case REMOTE_IP: {
                this.httpConfig.updateConfig(options);

            }
            default:
                break;

        }
        Tr.exit(tc, "updateConfig");
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        Tr.entry(tc, "initChannel");
        System.out.println("Got init channel now!");
        // Initialize with the parent bootstrap initializer
        this.chain.getBootstrap().getBaseInitializer().init(channel);

        ChannelPipeline pipeline = channel.pipeline();

        // Configure and add SSL Engine
        if (chain.isHttps()) {
            // SSLEngine engine = channel.newEngine(channel.alloc());
            // pipeline.addFirst("ssl", new SslHandler(engine, false));
//            pipeline.addLast(context.newHandler(channel.alloc()), new NettyProtocolNegotiationHandler());
            // Check to see if http/2 is enabled for this connection and save the result
            if (chain.isHttp2Enabled()) { // h2 setup starts here
                // Need to setup ALPN
//                setupH2Pipeline(pipeline);
            } else { // https setup starts here
                // No ALPN because only HTTP1.1, just need to add SSL handler here
//                setupHttpsPipeline(pipeline);
            }
        } else {
            if (chain.isHttp2Enabled()) { //h2c setup starts here
                setupH2cPipeline(pipeline);
            } else { // http 1.1 setup starts here
                setupHttp11Pipeline(pipeline);

            }
        }

        System.out.println("Pipeline built with handlers: ");
        pipeline.names().forEach(handler -> System.out.println(handler));

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
        addH2CCodecHandlers(pipeline);
        addPreHttpCodecHandlers(pipeline);
        pipeline.addLast(new ByteBufferCodec());
        pipeline.addLast(HttpDispatcherHandler.HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
    }

    /**
     * Utility method for building an HTTP1.1 pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttp11Pipeline(ChannelPipeline pipeline) {
        pipeline.addLast(NETTY_HTTP_SERVER_CODEC, new HttpServerCodec());
        pipeline.addLast(HttpDispatcherHandler.HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline);
    }

    /**
     * Utility method for building and HTTPS pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttpsPipeline(ChannelPipeline pipeline) {
        throw new UnsupportedOperationException("HTTPS not yet configured!!");
    }

    /**
     * Utility method for adding the Netty handlers needed for h2c connections
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addH2CCodecHandlers(ChannelPipeline pipeline) {
        HttpServerCodec sourceCodec = new HttpServerCodec();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory);
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler = new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, buildHttp2ConnectionHandler());

        pipeline.addLast(cleartextHttp2ServerUpgradeHandler);
        // TODO: Remove this, temp to see how upgrades are going
        pipeline.addLast("Upgrade Detector", new SimpleUserEventChannelHandler<UpgradeEvent>() {

            @Override
            protected void eventReceived(ChannelHandlerContext ctx, UpgradeEvent evt) throws Exception {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Got upgrade event for channel " + ctx.channel(), evt);
                }
                ctx.fireUserEventTriggered(evt.retain());
            }
        });
        pipeline.addLast("Prior Knowledge Upgrade Detector", new SimpleUserEventChannelHandler<PriorKnowledgeUpgradeEvent>() {

            @Override
            protected void eventReceived(ChannelHandlerContext ctx, PriorKnowledgeUpgradeEvent evt) throws Exception {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Got priorKnowledge upgrade event for channel " + ctx.channel(), evt);
                }
                ctx.fireUserEventTriggered(evt);
            }
        });

        // Handler to decide if an upgrade occurred or not and to add HTTP1 handlers on top
        pipeline.addLast(NO_UPGRADE_OCURRED_HANDLER_NAME, new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP 1.1.
                // TODO Check if we need to add common handlers here
                addPreDispatcherHandlers(ctx.pipeline());
                System.out.println("Firing channel read from " + NO_UPGRADE_OCURRED_HANDLER_NAME + " to pipeline: ");
                pipeline.names().forEach(handler -> System.out.println(handler));
                // TODO Check if we need to retain this before passing to dispatcher
                ctx.fireChannelRead(msg);
                // Remove unused handlers
                ctx.pipeline().remove(upgradeHandler);
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
            pipeline.addBefore(NETTY_HTTP_SERVER_CODEC, null, new AccessLoggerHandler(httpConfig));
        }
    }

    /**
     * Utility method for adding all the handlers that need to go just before the HTTP Dispatcher Handler
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addPreDispatcherHandlers(ChannelPipeline pipeline) {
        pipeline.addBefore(HttpDispatcherHandler.HTTP_DISPATCHER_HANDLER_NAME, null, new ChunkedWriteHandler());
        pipeline.addBefore(HttpDispatcherHandler.HTTP_DISPATCHER_HANDLER_NAME, null, new HttpObjectAggregator(64 * 1024));
        pipeline.addBefore(HttpDispatcherHandler.HTTP_DISPATCHER_HANDLER_NAME, null, new ByteBufferCodec());
        if (httpConfig.useForwardingHeaders()) {
            pipeline.addBefore(HttpDispatcherHandler.HTTP_DISPATCHER_HANDLER_NAME, null, new RemoteIpHandler(httpConfig));
        }
    }

    public class NettyProtocolNegotiationHandler extends ApplicationProtocolNegotiationHandler {

        /**
         * @param fallbackProtocol
         */
        protected NettyProtocolNegotiationHandler(String fallbackProtocol) {
            super(fallbackProtocol);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
            if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {

                ctx.pipeline().addLast(buildHttp2ConnectionHandler());

                ctx.pipeline().addLast(new HttpDispatcherHandler(httpConfig));

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Configured pipeline with " + ctx.pipeline().names());
                }

                return;
            }

            if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Configuring with HTTP 1.1 pipeline for incoming connection " + ctx.channel());
                }
                setupHttp11Pipeline(ctx.pipeline());
                return;
            }

            throw new IllegalStateException("unknown protocol: " + protocol);
        }

    }

    private HttpToHttp2ConnectionHandler buildHttp2ConnectionHandler() {
        DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
        int maxContentlength = (int) httpConfig.getMessageSizeLimit();
        InboundHttp2ToHttpAdapterBuilder builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).validateHttpHeaders(false);
        if (maxContentlength > 0)
            builder.maxContentLength(maxContentlength);
        System.out.println("Found length to be: " + maxContentlength);
        builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).maxContentLength(64 * 1024).validateHttpHeaders(false);
        return new HttpToHttp2ConnectionHandlerBuilder().frameListener(builder.build()).frameLogger(LOGGER).connection(connection).build();
    }

    protected static final Http2FrameLogger LOGGER = new Http2FrameLogger(io.netty.handler.logging.LogLevel.TRACE, HttpToHttp2ConnectionHandler.class);

    private final UpgradeCodecFactory upgradeCodecFactory = new UpgradeCodecFactory() {
        @Override
        public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "New upgrade codec called for protocol " + protocol);
            }
            if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Valid h2c protocol, setting up http2 clear text " + protocol);
                }
                return new Http2ServerUpgradeCodec(buildHttp2ConnectionHandler()) {
                    @Override
                    public void upgradeTo(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request) {
                        // Remove http1 handler adder
                        ctx.pipeline().remove(NO_UPGRADE_OCURRED_HANDLER_NAME);
                        // Call upgrade
                        super.upgradeTo(ctx, request);
                        // Set as stream 1 as defined in RFC
                        request.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 1);
                        // Forward request to dispatcher
                        ctx.fireChannelRead(ReferenceCountUtil.retain(request));
                    };
                };

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Returning null since no valid protocol was found: " + protocol);
                }
                return null;
            }
        }
    };

    public static class HttpPipelineBuilder {

        private final NettyChain chain;

        private Map<String, Object> compression;
        private Map<String, Object> headers;
        private Map<String, Object> httpOptions;
        private Map<String, Object> remoteIp;
        private Map<String, Object> samesite;

        HttpChannelConfig httpConfig;

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
                    this.samesite = options;
                    break;
                }

                default:
                    break;
            }
            Tr.exit(tc, "with");
            return this;
        }

        private HttpChannelConfig generateHttpOptions() {
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
