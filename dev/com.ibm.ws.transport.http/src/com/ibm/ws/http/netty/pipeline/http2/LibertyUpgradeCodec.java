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

import java.util.Collection;
import java.util.Collections;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.inbound.LibertyHttpObjectAggregator;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
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
import io.openliberty.http.netty.quiesce.QuiesceStrategy;
import io.openliberty.netty.internal.impl.QuiesceHandler;

/**
 *
 */
public class LibertyUpgradeCodec implements UpgradeCodecFactory {

    private static final TraceComponent tc = Tr.register(LibertyUpgradeCodec.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    protected static final Http2FrameLogger LOGGER = new Http2FrameLogger(io.netty.handler.logging.LogLevel.TRACE, HttpToHttp2ConnectionHandler.class) {
        @Override
        public void logGoAway(Direction direction, ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) {
            // TODO Auto-generated method stub
            new Exception().printStackTrace();
            super.logGoAway(direction, ctx, lastStreamId, errorCode, debugData);
        }

        @Override
        public void logRstStream(Direction direction, ChannelHandlerContext ctx, int streamId, long errorCode) {
            new Exception().printStackTrace();
            super.logRstStream(direction, ctx, streamId, errorCode);
        };
    };

    private final HttpChannelConfig httpConfig;
    private final Channel channel;

    /**
     * Helper method for creating H2C Upgrade handler
     */
    public static CleartextHttp2ServerUpgradeHandler createCleartextUpgradeHandler(HttpChannelConfig httpConfig, Channel channel) {
        HttpServerCodec sourceCodec = new HttpServerCodec(8192, httpConfig.getIncomingBodyBufferSize(), httpConfig.getLimitOfFieldSize(), httpConfig.getLimitOnNumberOfHeaders());
        LibertyUpgradeCodec codec = new LibertyUpgradeCodec(httpConfig, channel);
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, codec);
        return new CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler, codec.buildHttp2ConnectionHandler(httpConfig, channel));
    }

    public LibertyUpgradeCodec(HttpChannelConfig httpConfig, Channel channel) {
        super();
        this.httpConfig = httpConfig;
        this.channel = channel;
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
            HttpToHttp2ConnectionHandler handler = buildHttp2ConnectionHandler(httpConfig, channel);
            return new Http2ServerUpgradeCodec(handler) {
                @Override
                public void upgradeTo(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request) {
                    ctx.channel().attr(NettyHttpConstants.PROTOCOL).set("HTTP2");
                    
                    
                    // Call upgrade
                    super.upgradeTo(ctx, request);
                    // Set as stream 1 as defined in RFC
                    request.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 1);
                    if (Constants.SPEC_INITIAL_WINDOW_SIZE != httpConfig.getH2ConnectionWindowSize()) {
                        // window update sets the difference between what the client has (default) and the new value.
                        // TODO Should this actually be a difference from the settings value instead of the spec size itself since that's the part where the ohter endpoint has the established info
                        int updateSize = httpConfig.getH2ConnectionWindowSize() - Constants.SPEC_INITIAL_WINDOW_SIZE;
                        try {
                            // System.out.println("Original initial window size for connection: "
                            //                    + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).initialWindowSize(handler.decoder().connection().connectionStream()));
                            // System.out.println("Original window size for connection: "
                            //                    + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowSize(handler.decoder().connection().connectionStream()));
                             ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).incrementWindowSize(handler.decoder().connection().connectionStream(),
                                                                                                                       updateSize);
                            // System.out.println("New window size for connection: "
                            //                    + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowSize(handler.decoder().connection().connectionStream()));
                            // System.out.println("New initial window size for connection: "
                            //                    + ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).initialWindowSize(handler.decoder().connection().connectionStream()));
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

        } else if (AsciiString.contentEqualsIgnoreCase("websocket", protocol)) {
            // WebSocket upgrade detected
            return new UpgradeCodec() {
                @Override
                public void upgradeTo(ChannelHandlerContext ctx, io.netty.handler.codec.http.FullHttpRequest request) {
                             
                    ctx.fireChannelRead(ReferenceCountUtil.retain(request));
                    QuiesceHandler quiesceHandler = ctx.pipeline().get(QuiesceHandler.class);
                    if (quiesceHandler != null) {
                        quiesceHandler.setQuiesceTask(QuiesceStrategy.WEBSOCKET_CLOSE.getTask());
                    }
                }

                @Override
                public boolean prepareUpgradeResponse(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest, HttpHeaders upgradeHeaders) {
                    //Abort upgrade, pass through inbound pipeline like no upgrade was performed.
                    return false;
                }

                @Override
                public Collection<CharSequence> requiredUpgradeHeaders() {
                    //Delegated to HTTPDispatcher
                    return Collections.emptyList();
                };
            }; 
            
        }else {
                
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Returning null since no valid protocol was found: " + protocol);
            }
            return null;
        }
    }

    HttpToHttp2ConnectionHandler buildHttp2ConnectionHandler(HttpChannelConfig httpConfig, Channel channel) {
        DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
        int maxContentlength = (int) httpConfig.getMessageSizeLimit();
        InboundHttp2ToHttpAdapterBuilder builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).validateHttpHeaders(false);
        if (maxContentlength > 0)
            builder.maxContentLength(maxContentlength);
        Http2Settings initialSettings = new Http2Settings().maxConcurrentStreams(httpConfig.getH2MaxConcurrentStreams()).maxFrameSize(httpConfig.getH2MaxFrameSize());
        // TODO Need to appropriately build and parse configs
        if (httpConfig.getH2SettingsInitialWindowSize() != Constants.SPEC_INITIAL_WINDOW_SIZE)
            initialSettings.initialWindowSize(httpConfig.getH2SettingsInitialWindowSize());
        builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).maxContentLength(Integer.MAX_VALUE).validateHttpHeaders(false);
        HttpToHttp2ConnectionHandler handler = new HttpToHttp2ConnectionHandlerBuilder().frameListener(new LibertyInboundHttp2ToHttpAdapter(connection, Integer.MAX_VALUE, false, false, channel)).frameLogger(LOGGER).connection(connection).initialSettings(initialSettings).encoderIgnoreMaxHeaderListSize(true).limitFieldSize(httpConfig.getLimitOfFieldSize()).limitNumHeaders(httpConfig.getLimitOnNumberOfHeaders()).maxHeaderBlockSize(httpConfig.getH2MaxHeaderBlockSize()).build();

        if (!httpConfig.getH2LimitWindowUpdateFrames()) {
            ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowUpdateRatio(0.99999f);
            try {
                ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowUpdateRatio(connection.connectionStream(), 0.9999f);
            } catch (Http2Exception e) {
                e.printStackTrace();
            }
        }

        return handler;

    }

}
