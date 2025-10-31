/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import java.util.concurrent.TimeUnit;

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
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.http2.LibertyNettyALPNHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.channel.AllocatorContextSetter;
import io.openliberty.http.netty.channel.LoggingRecvByteBufAllocator;
import io.openliberty.http.netty.channel.TransportHandler;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;
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
        ACCESS_LOG, 
        TCP_OPTIONS
    }

    private final NettyChain chain;
    private final NettyHttpChannelConfig httpConfig;
    private final Map<ConfigElement, Map<String, Object>> configOptions;

    public static final String NO_UPGRADE_OCURRED_HANDLER_NAME = "UPGRADE_HANDLER_CHECK";
    public static final String NETTY_HTTP_SERVER_CODEC = "HTTP_SERVER_HANDLER";
    public static final String HTTP_DISPATCHER_HANDLER_NAME = "HTTP_DISPATCHER";
    public static final String HTTP_SSL_HANDLER_NAME = "SSL_HANDLER";
    public static final String HTTP_ALPN_HANDLER_NAME = "ALPN_HANDLER";
    public static final String HTTP_KEEP_ALIVE_HANDLER_NAME = "httpKeepAlive";
    public static final String HTTP_AGGREGATOR_HANDLER_NAME = "LIBERTY_OBJECT_AGGREGATOR";
    public static final String HTTP_REQUEST_HANDLER_NAME = "LIBERTY_REQUEST_HANDLER";
    public static final String HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME = "H2C_UPGRADE_HANDLER";
    public static final String CRLF_VALIDATION_HANDLER = "CRLFValidationHandler";

    public static final long maxContentLength = Long.MAX_VALUE;

    private HttpPipelineInitializer(NettyChain chain, NettyHttpChannelConfig httpConfig, Map<ConfigElement, Map<String, Object>> configOptions) {
        this.chain = chain;
        this.httpConfig = httpConfig;
        this.configOptions = configOptions;

        httpConfig.registerAccessLog(chain.getOwner().getName());
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        Tr.entry(tc, "initChannel");
        
        ChannelPipeline pipeline = channel.pipeline();

        // Initialize with the parent bootstrap initializer
        this.chain.getBootstrap().getBaseInitializer().init(channel);

        channel.attr(NettyHttpConstants.IS_OUTBOUND_KEY).set(false);
        channel.attr(NettyHttpConstants.ENDPOINT_PID).set(chain.getEndpointPID());

        FixedRecvByteBufAllocator channelAllocator = new FixedRecvByteBufAllocator(httpConfig.getIncomingBodyBufferSize());
        LoggingRecvByteBufAllocator loggingAllocator = new LoggingRecvByteBufAllocator(channelAllocator);
        channel.config().setRecvByteBufAllocator(loggingAllocator);

        pipeline.addLast("AllocatorContextSetter", AllocatorContextSetter.INSTANCE);

        pipeline.addLast("writeTimeoutHandler", new WriteTimeoutHandler(httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS));

        if(chain.isHttps()){
            setupSecurePipeline(pipeline);
        } else {
            setupUnsecurePipeline(pipeline);
        }
        if(Objects.nonNull(pipeline.get(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME))){
            pipeline.remove(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME);
        }
       

        Tr.exit(tc, "initChannel");
    }

    private void setupSecurePipeline(ChannelPipeline pipeline) throws NettyException{
        if(chain.isHttp2Enabled()){
            setupH2Pipeline(pipeline);
        } else {
            setupHttpsPipeline(pipeline);
        }
    }

    private void setupUnsecurePipeline(ChannelPipeline pipeline) {
        if(chain.isHttp2Enabled()){
            setupH2cPipeline(pipeline);
        } else {
            setupHttp11Pipeline(pipeline);
        }
    }

    private void setupH2Pipeline(ChannelPipeline pipeline) throws NettyException {

        SslHandler handler = getSslHandler(pipeline.channel());

        pipeline.addFirst(HTTP_SSL_HANDLER_NAME, handler);
        addPreHttpCodecHandlers(pipeline);
        pipeline.addLast(HTTP_ALPN_HANDLER_NAME, new LibertyNettyALPNHandler(httpConfig));
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreDispatcherHandlers(pipeline, true);
        pipeline.channel().attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);
        // Turn off half closure with H2
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }

    private void setupHttpsPipeline(ChannelPipeline pipeline) throws NettyException {
        SslHandler handler = getSslHandler(pipeline.channel());

        pipeline.addFirst(HTTP_SSL_HANDLER_NAME, handler);
        pipeline.channel().attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);
        setupHttp11Pipeline(pipeline);
    }

    private SslHandler getSslHandler(Channel channel) throws NettyException {
        NettyTlsProvider tlsProvider = chain.getOwner().getNettyTlsProvider();

        SslHandler handler = null;

        if(tlsProvider == null){
            throw new NettyException("TLS Provider is not loaded");
        }
        EndPointInfo ep = this.chain.getEndpointInfo();
        String host = ep.getHost();
        String port = Integer.toString(ep.getPort());

        if(chain.isHttp2Enabled())
            handler = tlsProvider.getInboundALPNSSLContext(configOptions.get(ConfigElement.SSL_OPTIONS), host, port, channel);
        else {
            handler = tlsProvider.getInboundSSLContext(configOptions.get(ConfigElement.SSL_OPTIONS), host, port, channel);
        }

        if (handler == null) {
            throw new NettyException("Failed to create SSL handler for endpoint: " + ep.getHost() + ":" + ep.getPort());
        }

        return handler;
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
        // Turn off half closure with H2
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }

    /**
     * Utility method for building an HTTP1.1 pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttp11Pipeline(ChannelPipeline pipeline) {

        // 8192 is used instead 4096 of for the maxInitialLineLength to avoid io.netty.handler.codec.http.TooLongHttpLineException 
        // Needed to pass JWT tests with long tokens
        HttpServerCodec sourceCodec = new HttpServerCodec(8192, httpConfig.getIncomingBodyBufferSize(), httpConfig.getLimitOfFieldSize(), httpConfig.getLimitOnNumberOfHeaders());
        pipeline.addLast(CRLF_VALIDATION_HANDLER, CRLFValidationHandler.INSTANCE);
        pipeline.addLast(NETTY_HTTP_SERVER_CODEC, sourceCodec);
        pipeline.addLast(HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline, false);
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
                if ("HTTP2".equals(ctx.pipeline().channel().attr(NettyHttpConstants.PROTOCOL).get())) {

                    ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                    return;
                }
                // Turn on half closure for H1
                ctx.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);

                pipeline.addBefore("transportHandler", HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
                ctx.channel().attr(NettyHttpConstants.PROTOCOL).set(ProtocolName.HTTP1.name());
                //TODO: this is a very large number (under https://github.com/OpenLiberty/open-liberty/issues/33114)
                pipeline.addAfter(HTTP_KEEP_ALIVE_HANDLER_NAME, HTTP_AGGREGATOR_HANDLER_NAME,
                                  new LibertyHttpObjectAggregator(httpConfig.getMessageSizeLimit() == -1 ? maxContentLength : httpConfig.getMessageSizeLimit()));
                pipeline.addAfter(HTTP_AGGREGATOR_HANDLER_NAME, HTTP_REQUEST_HANDLER_NAME, new LibertyHttpRequestHandler(httpConfig));
                ctx.pipeline().remove(this);

                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));

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
            if (pipeline.names().contains(NETTY_HTTP_SERVER_CODEC)){        
                pipeline.addLast(new AccessLoggerHandler(httpConfig));
            }
        }
    }

    /**
     * Utility method for adding all the handlers that need to go just before the HTTP Dispatcher Handler
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addPreDispatcherHandlers(ChannelPipeline pipeline, boolean isHttp2) {

        if (!isHttp2) {
            pipeline.addAfter(NETTY_HTTP_SERVER_CODEC, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
            //TODO: this is a very large number (under https://github.com/OpenLiberty/open-liberty/issues/33114)
            pipeline.addAfter(HTTP_KEEP_ALIVE_HANDLER_NAME, HTTP_AGGREGATOR_HANDLER_NAME,
                              new LibertyHttpObjectAggregator(httpConfig.getMessageSizeLimit() == -1 ? maxContentLength : httpConfig.getMessageSizeLimit()));
            pipeline.addAfter(HTTP_AGGREGATOR_HANDLER_NAME, HTTP_REQUEST_HANDLER_NAME, new LibertyHttpRequestHandler(httpConfig));
            
        }
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME,"transportHandler", TransportHandler.INSTANCE);

        if (pipeline.get(TimeoutHandler.class) == null) {
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, TimeoutHandler.NAME, new TimeoutHandler(httpConfig));
        }
        if (httpConfig.useForwardingHeaders()) {
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, null, new RemoteIpHandler(httpConfig));
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
                case TCP_OPTIONS:
                    return "defaultTCPOptions".equalsIgnoreCase(id);
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
     *
     */
    public void clearConfig() {
        this.httpConfig.clear();

    }
}
