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
import com.ibm.ws.http.netty.pipeline.CRLFValidationHandler;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;

import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import io.openliberty.netty.internal.exception.NettyException;

/**
 * Http11Configurator sets up a ChannelPipeline for standard HTTP/1.1 requests.
 * 
 * Responsibilities:
 * - Validate incoming requests before decoding them into HTTP objects.
 * - Insert an HTTP server codec for parsing HTTP/1.1 messages from raw bytes.
 * - Add a dispatcher to handle decoded HTTP messages and produce responses.
 * - Utilize PipelineHandlerUtility methods to insert pre-codec and pre-dispatcher handlers
 *   that handle logging, keep-alive, request aggregation, and other common logic.
 * - Configure the pipeline to allow half-closure, which is generally acceptable for HTTP/1.1.
 * 
 */
public class Http11Configurator implements PipelineConfigurator {

    /**
     * Initial maximum line length for the HttpServerCodec.
     * A line is the initial request line or a header line.
     * 8192 bytes is a commonly used default that balances performance and compatibility.
     */
    private static final int INITIAL_LINE_LENGTH = 8192;

    /**
     * Maximum allowed size for HTTP headers.
     * Using Integer.MAX_VALUE effectively removes size restrictions for headers, but
     * consider setting a more practical upper bound if large headers are not expected.
     */
    private static final int MAX_HEADER_SIZE = Integer.MAX_VALUE;

    private final NettyHttpChannelConfig httpConfig;

    public Http11Configurator(NettyHttpChannelConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    /**
     * Configures the ChannelPipeline for HTTP/1.1 requests:
     * 1. Validate CRLF sequences in incoming data.
     * 2. Add the HTTP server codec to decode inbound requests and encode outbound responses.
     * 3. Add the dispatcher to handle fully decoded HTTP messages.
     * 4. Insert pre-codec and pre-dispatcher handlers (like keep-alive, request aggregation)
     *    to maintain a consistent pipeline across various protocol scenarios.
     * 5. Enable half-closure for better compatibility with some HTTP/1.1 clients.
     *
     * @param pipeline The ChannelPipeline to configure.
     * @throws NettyException if a pipeline configuration error occurs.
     */
    @Override
    public void configure(ChannelPipeline pipeline) throws NettyException {
        // Validate CRLF sequences before HTTP decoding for security and correctness.
        pipeline.addLast(PipelineHandlerUtility.CRLF_VALIDATION_HANDLER, new CRLFValidationHandler());

        // Add the HTTP server codec with well-defined parameters.
        pipeline.addLast(PipelineHandlerUtility.NETTY_HTTP_SERVER_CODEC, createHttpServerCodec());

        // Add the main dispatcher to route requests and generate responses.
        pipeline.addLast(PipelineHandlerUtility.HTTP_DISPATCHER_HANDLER_NAME, new HttpDispatcherHandler(httpConfig));

        // Pre-codec handlers (e.g., access logging if enabled).
        PipelineHandlerUtility.addPreHttpCodecHandlers(pipeline, httpConfig);

        // Pre-dispatcher handlers (e.g., keep-alive, aggregator, request handler).
        PipelineHandlerUtility.addPreDispatcherHandlers(pipeline, false, httpConfig);

        // Allow half-closure for HTTP/1.1.
        pipeline.channel().config().setOption(ChannelOption.ALLOW_HALF_CLOSURE, true);


        System.out.println("MSP HTTP11CONFIG -> " + pipeline.names());
    
    }

    /**
     * Creates and returns a configured HttpServerCodec instance.
     *
     * @return configured HttpServerCodec instance.
     */
    private HttpServerCodec createHttpServerCodec() {
        return new HttpServerCodec(
            INITIAL_LINE_LENGTH,
            MAX_HEADER_SIZE,
            httpConfig.getIncomingBodyBufferSize()
        );
    }
}