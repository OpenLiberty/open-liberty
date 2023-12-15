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

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.NettyConfigBuilder;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.http2.LibertyNettyALPNHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;
import com.ibm.ws.http.netty.pipeline.inbound.TransportInboundHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

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
    public static final String HTTP_ALPN_HANDLER_NAME = "ALPN_HANDLER";
    public static final String HTTP_KEEP_ALIVE_HANDLER_NAME = "httpKeepAlive";
    public static final String HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME = "H2C_UPGRADE_HANDLER";

    long maxContentLength = Long.MAX_VALUE;

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
     * @throws NettyException
     */
    private void setupH2Pipeline(ChannelPipeline pipeline) throws NettyException {
        NettyTlsProvider tlsProvider = this.chain.getOwner().getNettyTlsProvider();
        if (tlsProvider == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configuration requires SSL and TLS Provider is not yet loaded: " + this.chain);
            }
            return;
        }
        // Needs appropriate ciphers and ALPN negotiator
        EndPointInfo ep = this.chain.getEndpointInfo();
        String host = ep.getHost();
        String port = Integer.toString(ep.getPort());
        SslContext context = tlsProvider.getInboundALPNSSLContext(chain.getOwner().getSslOptions(), host, port);
        if (context == null) {
            throw new NettyException("Problems creating SSL context for endpoint: " + ep.getHost() + ":" + ep.getPort() + " - " + ep);
        }
        pipeline.addFirst(HTTP_SSL_HANDLER_NAME, context.newHandler(pipeline.channel().alloc()));
        addPreHttpCodecHandlers(pipeline);
        pipeline.addLast(HTTP_ALPN_HANDLER_NAME, new LibertyNettyALPNHandler(httpConfig));
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreDispatcherHandlers(pipeline, true);
        pipeline.channel().attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);
    }

    /**
     * Utility method for building and H2C pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupH2cPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addH2CCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline, true);
    }

    /**
     * Utility method for building an HTTP1.1 pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttp11Pipeline(ChannelPipeline pipeline) {
//        long maxHeaderSize = httpConfig.getLimitOfFieldSize() * httpConfig.getLimitOnNumberOfHeaders() * 1L;
//        if (maxHeaderSize > Integer.MAX_VALUE)
//            maxHeaderSize = Integer.MAX_VALUE;
//        pipeline.addLast(NETTY_HTTP_SERVER_CODEC,
//                         new HttpServerCodec(HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH, (int) maxHeaderSize, httpConfig.getIncomingBodyBufferSize()));
        // POC Codec for custom validation?
//        HttpServerCodec sourceCodec = new HttpServerCodec(HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH, HttpObjectDecoder.DEFAULT_MAX_HEADER_SIZE, httpConfig.getIncomingBodyBufferSize(), httpConfig.getLimitOfFieldSize(), httpConfig.getLimitOnNumberOfHeaders());
        HttpServerCodec sourceCodec = new HttpServerCodec(HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH, Integer.MAX_VALUE, httpConfig.getIncomingBodyBufferSize());
        pipeline.addLast(NETTY_HTTP_SERVER_CODEC, sourceCodec);
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline, false);
    }

    /**
     * Utility method for building and HTTPS pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttpsPipeline(ChannelPipeline pipeline) {
        NettyTlsProvider tlsProvider = this.chain.getOwner().getNettyTlsProvider();
        if (tlsProvider == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Configuration requires SSL and TLS Provider is not yet loaded: " + this.chain);
            }
            return;
        }
        EndPointInfo ep = this.chain.getEndpointInfo();
        String host = ep.getHost();
        String port = Integer.toString(ep.getPort());
        SslContext context = tlsProvider.getInboundSSLContext(chain.getOwner().getSslOptions(), host, port);
        Channel channel = pipeline.channel();
        if (context == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(this, tc, "setupHttpsPipeline", "Error adding TLS Support, found null context");
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

        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME, cleartextHttp2ServerUpgradeHandler);

        // Handler to decide if an upgrade occurred or not and to add HTTP1 handlers on top
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, NO_UPGRADE_OCURRED_HANDLER_NAME, new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP 1.1.
                ctx.pipeline().remove(HttpServerUpgradeHandler.class);
                ctx.pipeline().addBefore("chunkWriteHandler", "objectAggregator", new LibertyHttpObjectAggregator(maxContentLength));
                ctx.pipeline().addBefore("objectAggregator", HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
                ctx.fireChannelRead(msg);
                // Remove unused handlers
                ctx.pipeline().remove(NO_UPGRADE_OCURRED_HANDLER_NAME);
            }

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof PriorKnowledgeUpgradeEvent) {
                    ctx.pipeline().remove(NO_UPGRADE_OCURRED_HANDLER_NAME);
                }
                super.userEventTriggered(ctx, evt);
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
    private void addPreDispatcherHandlers(ChannelPipeline pipeline, boolean isHttp2) {

        if (!isHttp2) {
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
            //TODO: this is a very large number, check best practice
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null,
                               new LibertyHttpObjectAggregator(httpConfig.getMessageSizeLimit() == -1 ? maxContentLength : httpConfig.getMessageSizeLimit()));
        }

        //pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new HttpObjectAggregator(maxContentLength);
        //pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new HttpObjectAggregator(64 * 1024));
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, "chunkLoggingHandler", new ChunkSizeLoggingHandler());
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, "chunkWriteHandler", new ChunkedWriteHandler());
        // if (httpConfig.useAutoCompression()) {
        //   pipeline.addLast(new NettyHttpContentCompressor());
        //}
//        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new ByteBufferCodec());
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

            if (httpConfig != null) {
                httpConfig = null;
            }
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

    /**
     *
     */
    public void clearConfig() {
        this.httpConfig.clear();

    }

}
