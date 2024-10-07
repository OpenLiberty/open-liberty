/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.http2;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2LocalFlowController;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

/**
 *
 */
public class LibertyUpgradeCodec implements UpgradeCodecFactory {

    private static final TraceComponent tc = Tr.register(LibertyUpgradeCodec.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    protected static final Http2FrameLogger LOGGER = new Http2FrameLogger(io.netty.handler.logging.LogLevel.TRACE, HttpToHttp2ConnectionHandler.class);

    private final HttpChannelConfig httpConfig;

    /**
     * Helper method for creating H2C Upgrade handler
     */
    public static CleartextHttp2ServerUpgradeHandler createCleartextUpgradeHandler(HttpChannelConfig httpConfig) {
        HttpServerCodec sourceCodec = new HttpServerCodec();
        LibertyUpgradeCodec codec = new LibertyUpgradeCodec(httpConfig);
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, codec);
        return new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, codec.buildHttp2ConnectionHandler(httpConfig));
    }

    public LibertyUpgradeCodec(HttpChannelConfig httpConfig) {
        super();
        this.httpConfig = httpConfig;
    }

    @Override
    public UpgradeCodec newUpgradeCodec(CharSequence protocol) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "New upgrade codec called for protocol " + protocol);
        }
        if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Valid h2c protocol, setting up http2 clear text " + protocol);
            }
            HttpToHttp2ConnectionHandler handler = buildHttp2ConnectionHandler(httpConfig);
            return new Http2ServerUpgradeCodec(handler) {
                @Override
                public void upgradeTo(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request) {
                    // Remove http1 handler adder
                    ctx.pipeline().remove(HttpPipelineInitializer.NO_UPGRADE_OCURRED_HANDLER_NAME);
                    // Call upgrade
                    super.upgradeTo(ctx, request);
                    // Set as stream 1 as defined in RFC
                    request.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 1);
                    if (Constants.SPEC_INITIAL_WINDOW_SIZE != httpConfig.getH2ConnectionWindowSize()) {
                        // window update sets the difference between what the client has (default) and the new value.
                        // TODO Should this actually be a difference from the settings value instead of the spec size itself since that's the part where the ohter endpoint has the established info
                        int updateSize = httpConfig.getH2ConnectionWindowSize() - Constants.SPEC_INITIAL_WINDOW_SIZE;
                        System.out.println("Update size to work with: " + updateSize);
                        try {
                            System.out.println("Original initial window size for connection: "
                                               + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).initialWindowSize(handler.decoder().connection().connectionStream()));
                            System.out.println("Original window size for connection: "
                                               + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowSize(handler.decoder().connection().connectionStream()));
                            ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).incrementWindowSize(handler.decoder().connection().connectionStream(),
                                                                                                                       updateSize);
                            System.out.println("New window size for connection: "
                                               + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowSize(handler.decoder().connection().connectionStream()));
                            System.out.println("New initial window size for connection: "
                                               + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).initialWindowSize(handler.decoder().connection().connectionStream()));
                        } catch (Http2Exception e) {
                            ctx.fireExceptionCaught(e);
                        }
                    }
                    // Send settings frame in advance
                    ctx.flush();
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

    private HttpToHttp2ConnectionHandler buildHttp2ConnectionHandler(HttpChannelConfig httpConfig) {
        DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
        int maxContentlength = (int) httpConfig.getMessageSizeLimit();
        InboundHttp2ToHttpAdapterBuilder builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).validateHttpHeaders(false);
        if (maxContentlength > 0)
            builder.maxContentLength(maxContentlength);
        System.out.println("Found length to be: " + maxContentlength);
        Http2Settings initialSettings = new Http2Settings().maxConcurrentStreams(httpConfig.getH2MaxConcurrentStreams()).maxFrameSize(httpConfig.getH2MaxFrameSize());
        // TODO Need to appropriately build and parse configs
        System.out.println("Initial window size got: " + httpConfig.getH2SettingsInitialWindowSize());
        if (httpConfig.getH2SettingsInitialWindowSize() != Constants.SPEC_INITIAL_WINDOW_SIZE)
            initialSettings.initialWindowSize(httpConfig.getH2SettingsInitialWindowSize());
        builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).maxContentLength(64 * 1024).validateHttpHeaders(false);
//        return new HttpToHttp2ConnectionHandlerBuilder().frameListener(builder.build()).frameLogger(LOGGER).connection(connection).initialSettings(initialSettings).build();
        HttpToHttp2ConnectionHandler handler = new HttpToHttp2ConnectionHandlerBuilder().frameListener(new LibertyInboundHttp2ToHttpAdapter(connection, 64
                                                                                                                                                        * 1024, false, false)).frameLogger(LOGGER).connection(connection).initialSettings(initialSettings).build();
        System.out.println("Limit window update frames? " + httpConfig.getH2LimitWindowUpdateFrames());
        System.out.println("Connection window size: " + httpConfig.getH2ConnectionWindowSize());
        if (!httpConfig.getH2LimitWindowUpdateFrames()) {
            ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowUpdateRatio(0.99999f);
            try {
                ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowUpdateRatio(connection.connectionStream(), 0.9999f);
            } catch (Http2Exception e) {
                // TODO Auto-generated catch block
                System.out.println("Damn an exception happened");
                e.printStackTrace();
            }
        }

        return handler;

    }

}
