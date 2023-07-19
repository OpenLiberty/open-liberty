/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Initializes a Netty Pipeline for an HTTP Endpoint. Configuration options may be
 * passed into it.
 */
public class HttpInitializer extends ChannelInitializer<Channel> {

    private static final TraceComponent tc = Tr.register(HttpInitializer.class);

    public enum ConfigElement {
        HTTP_OPTIONS,
        SSL_OPTIONS,
        REMOTEIP,
        COMPRESSION,
        SAMESITE,
        HEADERS
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

    private HttpInitializer(HttpPipelineBuilder builder) {
        this.chain = builder.chain;
        this.httpConfig = builder.httpConfig;
    }

    public HttpInitializer(NettyChain chain) {
        this.chain = chain;
        tcpOptions = Collections.emptyMap();
        sslOptions = Collections.emptyMap();
        httpOptions = Collections.emptyMap();
        remoteIp = Collections.emptyMap();
        compression = Collections.emptyMap();
        samesite = Collections.emptyMap();
        headers = Collections.emptyMap();
        endpointOptions = Collections.emptyMap();

        this.httpConfig = new NettyHttpChannelConfig();

    }

    public void updateConfig(ConfigElement config, Map<String, Object> options) {
        switch (config) {
            case HTTP_OPTIONS: {
                if (this.httpConfig != null) {
                    this.httpConfig.updateConfig(options);
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "starting a new http pipeline " + this);
        }

        ChannelPipeline pipeline = channel.pipeline();

        if (chain.isHttps()) {
            // SSLEngine engine = channel.newEngine(channel.alloc());
            // pipeline.addFirst("ssl", new SslHandler(engine, false));
        }

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        pipeline.addLast(new ByteBufferCodec());
        if (httpConfig.useForwardingHeaders()) {
            pipeline.addLast(new RemoteIpHandler(httpConfig));
        }
        pipeline.addLast(new HttpDispatcherHandler(httpConfig));
    }

    public static class HttpPipelineBuilder {

        private final NettyChain chain;
        private final Map<String, Object> tcpOptions;
        private final Map<String, Object> sslOptions;
        private Map<String, Object> httpOptions;
        private final Map<String, Object> remoteIp;
        private final Map<String, Object> compression;
        private final Map<String, Object> samesite;
        private final Map<String, Object> headers;
        private final Map<String, Object> endpointOptions;

        HttpChannelConfig httpConfig;

        public HttpPipelineBuilder(NettyChain chain) {
            this.chain = chain;
            tcpOptions = Collections.emptyMap();
            sslOptions = Collections.emptyMap();
            httpOptions = Collections.emptyMap();
            remoteIp = Collections.emptyMap();
            compression = Collections.emptyMap();
            samesite = Collections.emptyMap();
            headers = Collections.emptyMap();
            endpointOptions = Collections.emptyMap();

        }

        public HttpPipelineBuilder with(ConfigElement config, Map<String, Object> options) {
            switch (config) {
                case HTTP_OPTIONS: {
                    this.httpOptions = options;
                    initializeHttpConfig();
                    break;
                }
                default:
                    break;
            }
            return this;
        }

        private void initializeHttpConfig() {

            Map<String, Object> config = new HashMap<String, Object>();
            config.forEach(httpOptions::putIfAbsent);
            config.forEach(remoteIp::putIfAbsent);
            config.forEach(compression::putIfAbsent);
            config.forEach(samesite::putIfAbsent);
            config.forEach(headers::putIfAbsent);

            httpConfig = new NettyHttpChannelConfig(config);
        }

        public HttpInitializer build() {

            return new HttpInitializer(this);
        }
    }

}
