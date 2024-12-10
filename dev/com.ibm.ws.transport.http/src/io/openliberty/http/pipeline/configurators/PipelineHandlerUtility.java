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

import com.ibm.ws.http.netty.NettyHttpChannelConfig;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.openliberty.netty.internal.exception.NettyException;

import com.ibm.ws.http.netty.pipeline.AccessLoggerHandler;
import com.ibm.ws.http.netty.pipeline.ByteBufferCodec;
import com.ibm.ws.http.netty.pipeline.ChunkSizeLoggingHandler;
import com.ibm.ws.http.netty.pipeline.RemoteIpHandler;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpRequestHandler;
import com.ibm.ws.http.netty.pipeline.inbound.TransportInboundHandler;
import com.ibm.ws.http.netty.pipeline.TransportOutboundHandler;

/**
 * A utility class for adding common handlers to the Netty ChannelPipeline before and after
 * specific "anchor" handlers like the HTTP codec or the dispatcher handler.
 * 
 * This class centralizes repeated logic and helps avoid code duplication across different
 * protocol configurators (e.g., HTTP/1.1, HTTPS, H2, H2C).
 */
public final class PipelineHandlerUtility {

    // Pipeline Name Constants for handlers used in various configurators
    public static final String HTTP_DISPATCHER_HANDLER_NAME = "HTTP_DISPATCHER";
    public static final String NETTY_HTTP_SERVER_CODEC = "HTTP_SERVER_HANDLER";
    public static final String CRLF_VALIDATION_HANDLER = "CRLFValidationHandler";
    public static final String HTTP_KEEP_ALIVE_HANDLER_NAME = "HTTP_KEEP_ALIVE_HANDLER";
    public static final String HTTP_AGGREGATOR_HANDLER_NAME = "LIBERTY_OBJECT_AGGREGATOR";
    public static final String HTTP_REQUEST_HANDLER_NAME = "LIBERTY_REQUEST_HANDLER";
    public static final String CHUNK_LOGGING_HANDLER_NAME = "CHUNK_LOGGING_HANDLER";
    public static final String CHUNK_WRITE_HANDLER_NAME = "CHUNK_WRITE_HANDLER";
    public static final String BYTE_BUFFER_CODEC_HANDLER_NAME = "BYTE_BUFFER_CODEC";
    public static final String TRANSPORT_INBOUND_HANDLER_NAME = "TRANSPORT_INBOUND_HANDLER";
    public static final String TRANSPORT_OUTBOUND_HANDLER_NAME = "TRANSPORT_OUTBOUND_HANDLER";
    public static final String REMOTE_IP_HANDLER_NAME = "REMOTE_IP_HANDLER";
    public static final String MAX_CONNECTION_HANDLER_NAME = "maxConnectionHandler";
    public static final String NETTY_SERVLET_UPGRADE_HANDLER_NAME = "NettyServletUpgradeHandler";

    // Additional constants for HTTPS/HTTP2 scenarios
    public static final String HTTP_SSL_HANDLER_NAME = "SSL_HANDLER";
    public static final String HTTP_ALPN_HANDLER_NAME = "ALPN_HANDLER";

    // Default maximum content length for request aggregation
    public static final long DEFAULT_MAX_CONTENT_LENGTH = Long.MAX_VALUE;

    // Private constructor to prevent instantiation of this utility class.
    private PipelineHandlerUtility() {
        // Utility class: No instances allowed.
    }

    /**
     * Adds handlers that need to appear before the HTTP codec.
     * In the original code, this included logging if enabled.
     * 
     * @param pipeline   The ChannelPipeline to which handlers will be added.
     * @param httpConfig The NettyHttpChannelConfig containing server configuration.
     */
    public static void addPreHttpCodecHandlers(ChannelPipeline pipeline, NettyHttpChannelConfig httpConfig) {
        if (httpConfig.isAccessLoggingEnabled()) {
            pipeline.addLast(new AccessLoggerHandler(httpConfig));
        }
    }

    /**
     * Adds handlers that should appear before the dispatcher handler.
     * Depending on whether we are handling HTTP/2 or HTTP/1.1, different sets of handlers are added.
     * 
     * For HTTP/1.1:
     * - Add a keep-alive handler
     * - Add an object aggregator with a configurable max message size
     * - Add a request handler that transforms aggregated messages into a more convenient format
     * 
     * For both HTTP/1.1 and HTTP/2:
     * - Add a chunk size logging handler for debugging large payloads
     * - Add a chunked write handler for sending large responses in chunks
     * - Add a ByteBufferCodec for processing ByteBuf content
     * - Add transport layer inbound/outbound handlers for uniform data handling
     * - If forwarding headers are enabled, add a RemoteIpHandler to record client addresses
     * 
     * @param pipeline   The ChannelPipeline to configure.
     * @param isHttp2    True if this pipeline is for HTTP/2; false for HTTP/1.1.
     * @param httpConfig The NettyHttpChannelConfig containing server configuration.
     * @throws NettyException if adding handlers fails unexpectedly
     */
    public static void addPreDispatcherHandlers(ChannelPipeline pipeline, boolean isHttp2, NettyHttpChannelConfig httpConfig) {
        if (!isHttp2) {
            // For HTTP/1.1, add keep-alive and request aggregation/handling
            pipeline.addAfter(NETTY_HTTP_SERVER_CODEC, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());

            long maxContentLength = (httpConfig.getMessageSizeLimit() == -1) 
                    ? DEFAULT_MAX_CONTENT_LENGTH 
                    : httpConfig.getMessageSizeLimit();

            pipeline.addAfter(HTTP_KEEP_ALIVE_HANDLER_NAME, HTTP_AGGREGATOR_HANDLER_NAME,
                    new LibertyHttpObjectAggregator(maxContentLength));
            pipeline.addAfter(HTTP_AGGREGATOR_HANDLER_NAME, HTTP_REQUEST_HANDLER_NAME, new LibertyHttpRequestHandler());
        }

        // Add common handlers for both HTTP/1.1 and HTTP/2 pipelines:
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, CHUNK_LOGGING_HANDLER_NAME, new ChunkSizeLoggingHandler());
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, CHUNK_WRITE_HANDLER_NAME, new ChunkedWriteHandler());
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, BYTE_BUFFER_CODEC_HANDLER_NAME, new ByteBufferCodec());
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, TRANSPORT_INBOUND_HANDLER_NAME, new TransportInboundHandler(httpConfig));
        pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, TRANSPORT_OUTBOUND_HANDLER_NAME, new TransportOutboundHandler(httpConfig));

        if (httpConfig.useForwardingHeaders()) {
            pipeline.addBefore(HTTP_DISPATCHER_HANDLER_NAME, REMOTE_IP_HANDLER_NAME, new RemoteIpHandler(httpConfig));
        }
    }

    public static void addHttp11FallbackHandlers(ChannelPipeline pipeline, NettyHttpChannelConfig httpConfig) {
        pipeline.addBefore(CHUNK_WRITE_HANDLER_NAME, HTTP_KEEP_ALIVE_HANDLER_NAME, new HttpServerKeepAliveHandler());
    
        long maxContentLength = (httpConfig.getMessageSizeLimit() == -1)
                ? DEFAULT_MAX_CONTENT_LENGTH 
                : httpConfig.getMessageSizeLimit();
    
        pipeline.addAfter(HTTP_KEEP_ALIVE_HANDLER_NAME, HTTP_AGGREGATOR_HANDLER_NAME,
                new LibertyHttpObjectAggregator(maxContentLength));
    
        pipeline.addAfter(HTTP_AGGREGATOR_HANDLER_NAME, HTTP_REQUEST_HANDLER_NAME, new LibertyHttpRequestHandler());
    }
}
