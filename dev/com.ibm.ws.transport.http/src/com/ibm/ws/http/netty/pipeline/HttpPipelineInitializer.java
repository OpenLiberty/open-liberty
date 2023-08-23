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
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyChain;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpChannelConfig.NettyConfigBuilder;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.ws.http.netty.pipeline.inbound.TransportInboundHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Initializes a Netty Pipeline for an HTTP Endpoint. Configuration options may be
 * passed into it.
 */
public class HttpPipelineInitializer extends ChannelInitializer<Channel> {

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

        ChannelPipeline pipeline = channel.pipeline();

        if (chain.isHttps()) {
            SslContext context = chain.getOwner().getNettyTlsProvider().getInboundSSLContext(chain.getOwner().getSslOptions(), chain.getActiveHost(),
                                                                                             Integer.toString(chain.getActivePort()));
            if (context == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.entry(this, tc, "initChannel", "Error adding TLS Support");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(this, tc, "initChannel");
                channel.close();
                return;
            }
            SSLEngine engine = context.newEngine(channel.alloc());
            pipeline.addFirst("SSL_HANDLER", new SslHandler(engine, false));
            channel.attr(NettyHttpConstants.IS_SECURE).set(Boolean.TRUE);
        } else {
            channel.attr(NettyHttpConstants.IS_SECURE).set(Boolean.FALSE);
        }

        if (httpConfig.isAccessLoggingEnabled()) {
            pipeline.addLast(new AccessLoggerHandler(httpConfig));
        }
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast("httpKeepAlive", new HttpServerKeepAliveHandler());

        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new ChunkedWriteHandler());
        // if (httpConfig.useAutoCompression()) {
        //   pipeline.addLast(new NettyHttpContentCompressor());
        //}
        pipeline.addLast(new ByteBufferCodec());
        pipeline.addLast(new TransportInboundHandler(httpConfig));
        if (httpConfig.useForwardingHeaders()) {
            pipeline.addLast(new RemoteIpHandler(httpConfig));
        }
        pipeline.addLast(new TransportOutboundHandler(httpConfig));
        pipeline.addLast(new HttpDispatcherHandler(httpConfig));

        Tr.exit(tc, "initChannel");
    }

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
