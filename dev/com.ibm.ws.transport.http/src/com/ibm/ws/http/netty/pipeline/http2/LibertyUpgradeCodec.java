/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
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
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.quiesce.QuiesceStrategy;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.netty.internal.impl.QuiesceHandler;

/**
 * Implementation of {@link UpgradeCodecFactory} for Liberty specific functionality
 * when upgrading from HTTP 1.1 to HTTP 2.0 on a clear text connection.
 */
public class LibertyUpgradeCodec implements UpgradeCodecFactory {

    private static final TraceComponent tc = Tr.register(LibertyUpgradeCodec.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

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
                    ctx.pipeline().get(TimeoutHandler.class).markProtocol(ctx.pipeline(), NettyHttpConstants.ProtocolName.HTTP2);
                    
                    // Call upgrade
                    super.upgradeTo(ctx, request);
                    // Set as stream 1 as defined in RFC
                    request.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 1);
                    if (Constants.SPEC_INITIAL_WINDOW_SIZE != httpConfig.getH2ConnectionWindowSize()) {
                        // window update sets the difference between what the client has (default) and the new value.
                        // TODO Should this actually be a difference from the settings value instead of the spec size itself since that's the part where the ohter endpoint has the established info
                        int updateSize = httpConfig.getH2ConnectionWindowSize() - Constants.SPEC_INITIAL_WINDOW_SIZE;
                        try {
                             ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).incrementWindowSize(handler.decoder().connection().connectionStream(),
                                                                                                                       updateSize);
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
                    ctx.channel().attr(NettyHttpConstants.PROTOCOL).set(NettyHttpConstants.ProtocolName.WEBSOCKET.name());
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
        // Netty accepts integer for max length so we would need to adapt for this
        int maxContentlength = httpConfig.getMessageSizeLimit() >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) httpConfig.getMessageSizeLimit();
        InboundHttp2ToHttpAdapterBuilder builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).validateHttpHeaders(false);
        if (maxContentlength > 0)
            builder.maxContentLength(maxContentlength);
        Http2Settings initialSettings = new Http2Settings().maxConcurrentStreams(httpConfig.getH2MaxConcurrentStreams()).maxFrameSize(httpConfig.getH2MaxFrameSize());
        // TODO Need to appropriately build and parse configs
        if (httpConfig.getH2SettingsInitialWindowSize() != Constants.SPEC_INITIAL_WINDOW_SIZE)
            initialSettings.initialWindowSize(httpConfig.getH2SettingsInitialWindowSize());
        int frameWindowSize = httpConfig.getH2ResetFramesWindow() / 1000; // Frame window divided by 1000 because our httpOption is in ms precision but Netty's is in seconds
        builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).maxContentLength(Integer.MAX_VALUE).validateHttpHeaders(false);
        HttpToHttp2ConnectionHandler handler = new HttpToHttp2ConnectionHandlerBuilder().frameListener(new LibertyInboundHttp2ToHttpAdapter(connection, Integer.MAX_VALUE, false, false, channel)).connection(connection).initialSettings(initialSettings).encoderIgnoreMaxHeaderListSize(true).decoderEnforceMaxRstFramesPerWindow(httpConfig.getH2MaxResetFrames(), frameWindowSize).encoderEnforceMaxRstFramesPerWindow(httpConfig.getH2MaxResetFrames(), frameWindowSize).limitFieldSize(httpConfig.getLimitOfFieldSize()).limitNumHeaders(httpConfig.getLimitOnNumberOfHeaders()).maxHeaderBlockSize(httpConfig.getH2MaxHeaderBlockSize()).build();
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
