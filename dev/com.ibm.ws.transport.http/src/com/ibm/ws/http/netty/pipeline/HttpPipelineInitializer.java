/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.ConfigElement;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.ProtocolState;
import com.ibm.ws.http.netty.ProtocolState.ProtocolSource;
import com.ibm.ws.http.netty.pipeline.http2.LibertyNettyALPNHandler;
import com.ibm.ws.http.netty.pipeline.http2.LibertyUpgradeCodec;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler.PriorKnowledgeUpgradeEvent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.channel.LoggingRecvByteBufAllocator;
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

    private final NettyChain chain;
    private final NettyHttpChannelConfig httpConfig;
    private final Map<ConfigElement, Map<String, Object>> configOptions;

    public static final String NO_UPGRADE_OCURRED_HANDLER_NAME = "upgradeCheckHandler";
    public static final String NETTY_HTTP_SERVER_CODEC = "httpServerCodec";
    public static final String HTTP_SSL_HANDLER_NAME = "sslHandler";
    public static final String HTTP_KEEP_ALIVE_HANDLER_NAME = "httpKeepAlive";
    public static final String CRLF_VALIDATION_HANDLER = "CRLFValidationHandler";
    public static final String FLOW_CONTROL_HANDLER_NAME = "flowControlHandler";
    public static final String HTTP_AGGREGATOR_HANDLER_NAME = "objectAggregator";
    public static final String HTTP_REQUEST_HANDLER_NAME = "requestHandler";
    public static final String HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME = "h2cUpgradeHandler";
    public static final String HTTP1_PROTOCOL_HANDLER_NAME = "http1ProtocolHandler";
    public static final String WRITE_TIMEOUT_HANDER_NAME = "writeTimeoutHandler";

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
        LoggingRecvByteBufAllocator loggingAllocator = new LoggingRecvByteBufAllocator(channelAllocator, channel);
        channel.config().setRecvByteBufAllocator(loggingAllocator);

        pipeline.addLast(WRITE_TIMEOUT_HANDER_NAME, new WriteTimeoutHandler(httpConfig.getWriteTimeout(), TimeUnit.MILLISECONDS));

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
        pipeline.addLast(LibertyNettyALPNHandler.NAME, new LibertyNettyALPNHandler(httpConfig));
        pipeline.addLast(HttpDispatcherHandler.NAME, new HttpDispatcherHandler(httpConfig));
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
        pipeline.addLast(HttpDispatcherHandler.NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addH2CCodecHandlers(pipeline);
        addH2cTimeoutHandler(pipeline);
        addPreDispatcherHandlers(pipeline, true);
        // Turn off half closure with H2
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, false);
    }

    private void addH2cTimeoutHandler(ChannelPipeline pipeline) {
        if (pipeline.get(TimeoutHandler.class) == null) {
            pipeline.addBefore(HttpDispatcherHandler.NAME, TimeoutHandler.NAME, new TimeoutHandler(httpConfig));
        }
    }

    /**
     * Utility method for building an HTTP1.1 pipeline
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void setupHttp11Pipeline(ChannelPipeline pipeline) {

        // 8192 is used instead 4096 of for the maxInitialLineLength to avoid io.netty.handler.codec.http.TooLongHttpLineException 
        // Needed to pass JWT tests with long tokens
        int maxLineLength = Integer.MAX_VALUE;
        if(httpConfig.getMessageSizeLimit() != -1 && httpConfig.getMessageSizeLimit() < Integer.MAX_VALUE) {
            maxLineLength = (int)httpConfig.getMessageSizeLimit();
        }
        HttpServerCodec sourceCodec = new HttpServerCodec(maxLineLength, httpConfig.getIncomingBodyBufferSize(), httpConfig.getLimitOfFieldSize(), httpConfig.getLimitOnNumberOfHeaders());
        pipeline.addLast(CRLFValidationHandler.NAME, CRLFValidationHandler.INSTANCE);
        pipeline.addLast(NETTY_HTTP_SERVER_CODEC, sourceCodec);
        pipeline.addLast(HttpDispatcherHandler.NAME, new HttpDispatcherHandler(httpConfig));
        addPreHttpCodecHandlers(pipeline);
        addPreDispatcherHandlers(pipeline, false);
        pipeline.addAfter(NETTY_HTTP_SERVER_CODEC, HTTP1_PROTOCOL_HANDLER_NAME, new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                establishHttp1Protocol(ctx, msg, Boolean.TRUE.equals(ctx.channel().attr(NettyHttpConstants.IS_SECURE).get()));
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            }
        });
        // Turn off auto read for HTTP/1.1
        pipeline.channel().config().setAutoRead(false);
    }

    /**
     * Utility method for adding the Netty handlers needed for h2c connections
     *
     * @param pipeline ChannelPipeline to update as necessary
     */
    private void addH2CCodecHandlers(ChannelPipeline pipeline) {
        final CleartextHttp2ServerUpgradeHandler cleartextHttp2ServerUpgradeHandler = LibertyUpgradeCodec.createCleartextUpgradeHandler(httpConfig, pipeline.channel());

        pipeline.addBefore(HttpDispatcherHandler.NAME, HTTP2_CLEARTEXT_UPGRADE_HANDLER_NAME, cleartextHttp2ServerUpgradeHandler);

        // Handler to decide if an upgrade occurred or not and to add HTTP1 handlers on top
        pipeline.addBefore(HttpDispatcherHandler.NAME, NO_UPGRADE_OCURRED_HANDLER_NAME, new SimpleChannelInboundHandler<HttpMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                if (ProtocolState.current(ctx.channel()) == ProtocolName.HTTP2) {

                    ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                    return;
                }
                // Turn on half closure for H1
                ctx.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);
                // Turn off auto read for H1
                ctx.channel().config().setAutoRead(false);

                TimeoutHandler timeoutHandler = pipeline.get(TimeoutHandler.class);

                // Add H1 handlers
                // TODO we should decide if the TimeoutHandler is optional or not for this check
                if(pipeline.get(ReadFlowHandler.class) == null){
                    pipeline.addBefore((timeoutHandler != null) ? TimeoutHandler.NAME : HttpDispatcherHandler.NAME, ReadFlowHandler.NAME, ReadFlowHandler.INSTANCE);
                }
                if(pipeline.get(HttpServerKeepAliveHandler.class) == null){
                    pipeline.addBefore(ReadFlowHandler.NAME, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
                }

                establishHttp1Protocol(ctx, msg, false);
      

                Tr.debug(tc, "Pipeline before H1 fallback after no H2C: "+ ctx.pipeline());

                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));

                // Add flow control handler after sending the message to hold up any other objects coming up but not
                // the first http request that reached this handler

                if(pipeline.get(FlowControlHandler.class) == null){
                    pipeline.addBefore(HTTP_KEEP_ALIVE_HANDLER_NAME, FLOW_CONTROL_HANDLER_NAME, new FlowControlHandler());
                }

                // Remove non-H1 handlers
                if (pipeline.get("h2cUpgradeHandler") != null) {
                    pipeline.remove("h2cUpgradeHandler");
                }
                if (pipeline.get("HttpServerUpgradeHandler#0") != null) {
                    pipeline.remove("HttpServerUpgradeHandler#0");
                }
                if (pipeline.get("upgradeCheckHandler") != null) {
                    pipeline.remove("upgradeCheckHandler");
                }

            }

            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof PriorKnowledgeUpgradeEvent) {
                    // Netty recognizes prior knowledge and installs H2 before emitting this event.
                    ProtocolState.establish(ctx.channel(), ProtocolName.HTTP2,
                                            ProtocolSource.H2C_PRIOR_KNOWLEDGE);
                    ctx.pipeline().remove(NO_UPGRADE_OCURRED_HANDLER_NAME);
                }
                super.userEventTriggered(ctx, evt);
            }
        });
    }

    private static void establishHttp1Protocol(ChannelHandlerContext context, HttpMessage message, boolean secure) {
        ProtocolName protocol = message.protocolVersion().equals(HttpVersion.HTTP_1_0)
                        ? ProtocolName.HTTP10 : ProtocolName.HTTP1;
        ProtocolSource source = protocol == ProtocolName.HTTP10
                        ? (secure ? ProtocolSource.TLS_HTTP10 : ProtocolSource.CLEARTEXT_HTTP10)
                        : (secure ? ProtocolSource.TLS_HTTP1 : ProtocolSource.CLEARTEXT_HTTP1);
        ProtocolState.establish(context.channel(), protocol, source);
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
            
            if(pipeline.get(FlowControlHandler.class) == null){
                pipeline.addAfter(NETTY_HTTP_SERVER_CODEC, FLOW_CONTROL_HANDLER_NAME, new FlowControlHandler());
            }

            if(pipeline.get(HttpServerKeepAliveHandler.class) == null){
                pipeline.addAfter(FLOW_CONTROL_HANDLER_NAME, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
            }
            
            if(pipeline.get(ReadFlowHandler.class) == null) {
                pipeline.addBefore(HttpDispatcherHandler.NAME, ReadFlowHandler.NAME, ReadFlowHandler.INSTANCE);
            }
        }

        if (httpConfig.useForwardingHeaders()) {
            pipeline.addBefore(HttpDispatcherHandler.NAME, RemoteIpHandler.NAME, new RemoteIpHandler(httpConfig));
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
