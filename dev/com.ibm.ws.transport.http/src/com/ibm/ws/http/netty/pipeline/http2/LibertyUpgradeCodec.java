/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.DecoratingHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2LocalFlowController;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ControlFrameLimitEncoder;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersEncoder;
import io.netty.handler.codec.http2.Http2LifecycleManager;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.codec.http2.LibertyDefaultHttp2HeadersDecoder;
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
        int maxContentlength = (httpConfig.getMessageSizeLimit() >= Integer.MAX_VALUE || httpConfig.getMessageSizeLimit() < 0) ? Integer.MAX_VALUE : (int) httpConfig.getMessageSizeLimit();
        final HttpServerUpgradeHandler upgradeHandler = new HttpServerUpgradeHandler(sourceCodec, codec, maxContentlength);
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
            NettyH2RateState rateState = new NettyH2RateState(httpConfig);
            rateState.setUpgradeStreamId(Http2CodecUtil.HTTP_UPGRADE_STREAM_ID);
            HttpToHttp2ConnectionHandler handler = buildHttp2ConnectionHandler(httpConfig, channel, rateState);
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
         return buildHttp2ConnectionHandler(httpConfig, channel, new NettyH2RateState(httpConfig));
    }

    HttpToHttp2ConnectionHandler buildHttp2ConnectionHandler(HttpChannelConfig httpConfig, Channel channel, NettyH2RateState rateState) {
        DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
        // Netty accepts integer for max length so we would need to adapt for this
        int maxContentlength = httpConfig.getMessageSizeLimit() >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) httpConfig.getMessageSizeLimit();
        Http2Settings initialSettings = new Http2Settings().maxConcurrentStreams(httpConfig.getH2MaxConcurrentStreams()).maxFrameSize(httpConfig.getH2MaxFrameSize());
        if (httpConfig.getH2SettingsInitialWindowSize() != Constants.SPEC_INITIAL_WINDOW_SIZE)
            initialSettings.initialWindowSize(httpConfig.getH2SettingsInitialWindowSize());
        InboundHttp2ToHttpAdapterBuilder builder = new InboundHttp2ToHttpAdapterBuilder(connection).propagateSettings(false).maxContentLength(Integer.MAX_VALUE).validateHttpHeaders(false);
        if (maxContentlength >= 0)
            builder.maxContentLength(maxContentlength);
        else
            maxContentlength = Integer.MAX_VALUE;
        LibertyInboundHttp2ToHttpAdapter listener = new LibertyInboundHttp2ToHttpAdapter(connection, maxContentlength, false, false, channel, httpConfig, rateState);

        connection.addListener(listener);


        // Create encoder with same configuration as builder would
        DefaultHttp2FrameWriter frameWriter = new DefaultHttp2FrameWriter(Http2HeadersEncoder.NEVER_SENSITIVE, true);
        DefaultHttp2ConnectionEncoder defaultEncoder = new DefaultHttp2ConnectionEncoder(
            connection, 
            frameWriter
        );
        Http2ConnectionEncoder wrappedEncoder = new Http2ControlFrameLimitEncoder(defaultEncoder, Http2CodecUtil.DEFAULT_MAX_QUEUED_CONTROL_FRAMES);

        // Wrap with millisecond tracking for Liberty logic reusing the listener as the tracker
        wrappedEncoder = new LibertyResetEncoderWrapper(
            wrappedEncoder,
            listener,
            rateState,
            httpConfig.getWriteTimeout()
        );

        // Create default decoder with reader using Liberty HTTP options
        DefaultHttp2ConnectionDecoder defaultDecoder = new DefaultHttp2ConnectionDecoder(
            connection,
            wrappedEncoder,
            new DefaultHttp2FrameReader(
                new LibertyDefaultHttp2HeadersDecoder(
                    true,
                    Long.MAX_VALUE,
                    httpConfig.getH2MaxHeaderBlockSize(),
                    httpConfig.getLimitOfFieldSize(),
                    httpConfig.getLimitOnNumberOfHeaders()
                    )
                )
            );

        // Build with custom encoder
        HttpToHttp2ConnectionHandler handler = new HttpToHttp2ConnectionHandlerBuilder()
            .frameListener(listener)
            .initialSettings(initialSettings)
            .decoderEnforceMaxRstFramesPerWindow(0, 0) // Force disable since logic is already done in custom listener
            .codec(defaultDecoder, wrappedEncoder)
            .build();

        if (!httpConfig.getH2LimitWindowUpdateFrames()) {
            // Execute this in event loop due to assertions
            channel.eventLoop().execute(() -> {
                ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowUpdateRatio(0.99999f);
                try {
                    ((DefaultHttp2LocalFlowController) handler.decoder().flowController()).windowUpdateRatio(connection.connectionStream(), 0.9999f);
                } catch (Http2Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isErrorEnabled()) {
                        Tr.error(tc, "Encountered error while attempting to update window ratio: " + e.getMessage() + ". Continuing with default window update ratio.", e);
                    }
                }
            });
        }
        return handler;
    }

    private class LibertyResetEncoderWrapper extends DecoratingHttp2ConnectionEncoder {
        private final ResetFrameTracker tracker;
        private final NettyH2RateState rateState;
        private final int writeTimeout;
        private Http2LifecycleManager lifecycleManager;

        /**
         * Per-stream state for the deferred-write timeout 
         */
        private final Map<Integer, StreamWriteState> streamWrites = new HashMap<>();

        private final class StreamWriteState {
            ScheduledFuture<?> timeoutFuture;
            boolean timedOut;
            int pendingFrames;
        }
        
        public LibertyResetEncoderWrapper(Http2ConnectionEncoder delegate, ResetFrameTracker libertyAdapter, NettyH2RateState rateState, int writeTimeout) {
            super(delegate);
            this.tracker = libertyAdapter;
            this.rateState = rateState;
            this.writeTimeout = writeTimeout;
        }

        @Override
        public void lifecycleManager(Http2LifecycleManager lifecycleManager) {
            this.lifecycleManager = lifecycleManager;
            super.lifecycleManager(lifecycleManager);
        }
        
        @Override
        public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, 
                                            long errorCode, ChannelPromise promise) {
            ChannelFuture future = super.writeRstStream(ctx, streamId, errorCode, promise);
            if (tracker.isResetTrackingEnabled()) {
                tracker.incrementResetCount();
                if (tracker.isResetsInTimeExceeded()) {
                    lifecycleManager.onError(ctx, true, LibertyInboundHttp2ToHttpAdapter.RST_FRAME_RATE_EXCEEDED);
                    ctx.close();
                }
            }
            return future;
        }

        /**
         * queued-bytes guard - Intercept every outbound DATA write and account for its bytes.
         * This method enforces our cap before the frame enters Netty's DefaultHttp2RemoteFlowController queue
         */
        @Override
        public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId,
                                       ByteBuf data, int padding, boolean endStream,
                                       ChannelPromise promise) {
            // Capture size before delegating — the delegate takes ownership of the buffer.
            final long frameSize = data.readableBytes() + padding;
            ChannelPromise accounted = promise;

            // skipping a stream we have already timed out and reset.
            StreamWriteState timedOutState = streamWrites.get(streamId);
            if (timedOutState != null && timedOutState.timedOut) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "writeData: stream " + streamId + " already timed out and reset; "
                                       + frameSize + " bytes not accounted against the queue cap");
                }
                return super.writeData(ctx, streamId, data, padding, endStream, promise);
            }

            switch (rateState.tryIncrementQueuedBytes(frameSize)) {
                case SUCCESS:
                    accounted = promise.unvoid();
                    final int completedStreamId = streamId;
                    StreamWriteState state = scheduleWriteTimeoutIfNeeded(ctx, completedStreamId);
                    state.pendingFrames++;
                    accounted.addListener(f -> onDataWriteComplete(completedStreamId, (int) frameSize));
                    break;

                case FIRST_TO_EXCEED:
                    Http2Exception qbEx = Http2Exception.connectionError(
                            Http2Error.ENHANCE_YOUR_CALM,
                            "Total queued bytes across all streams exceeded limit!");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "writeData: queued bytes limit exceeded on channel "
                                     + ctx.channel() + "; closing the connection");
                    }
                    lifecycleManager.onError(ctx, true, qbEx);
                    ctx.close();
                    break;

                case ALREADY_EXCEEDED:
                default:
                    // Connection is already being torn down.
                    break;
            }

            return super.writeData(ctx, streamId, data, padding, endStream, accounted);
        }

        /**
         * Deferred-write timeout for a stream, if it does not already have one.
         * One timer per stream, not per frame.
         */
        private StreamWriteState scheduleWriteTimeoutIfNeeded(ChannelHandlerContext ctx, int streamId) {
            StreamWriteState state = streamWrites.get(streamId);
            if (state == null) {
                state = new StreamWriteState();
                streamWrites.put(streamId, state);
            }
            if (state.timeoutFuture == null && writeTimeout > 0 && !state.timedOut) {
                state.timeoutFuture = ctx.executor().schedule(() -> handleWriteTimeout(ctx, streamId),
                                                              writeTimeout, TimeUnit.MILLISECONDS);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "scheduleWriteTimeoutIfNeeded: stream " + streamId
                                 + " write timeout armed for " + writeTimeout + "ms");
                }
            }
            return state;
        }

        /**
         * A stream did not drain its queued DATA within writeTimeout.
         * Resets the stream but does NOT return its bytes to the budget.
         */
        private void handleWriteTimeout(ChannelHandlerContext ctx, int streamId) {
            StreamWriteState state = streamWrites.get(streamId);
            if (state == null) {
                return; 
            }
            state.timeoutFuture = null;
            state.timedOut = true;

            if (!ctx.channel().isActive()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "handleWriteTimeout: stream " + streamId + " channel already closed, no RST_STREAM sent");
                }
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleWriteTimeout: stream " + streamId + " did not drain within "
                             + writeTimeout + "ms; resetting stream, queued bytes stay charged");
            }

            writeRstStream(ctx, streamId, Http2Error.FLOW_CONTROL_ERROR.code(), ctx.newPromise());
            ctx.flush();
        }

        /**
         * A queued DATA frame has left the flow controller.
         * Returns bytes to the budget unless this stream was timed out.
         */
        private void onDataWriteComplete(int streamId, int size) {
            StreamWriteState state = streamWrites.get(streamId);
            boolean timedOut = state != null && state.timedOut;

            if (!timedOut) {
                rateState.decrementQueuedBytes(size);
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "onDataWriteComplete: stream " + streamId + " timed out earlier; "
                             + size + " bytes stay charged to the connection");
            }

            if (state != null && --state.pendingFrames <= 0) {
                if (state.timeoutFuture != null) {
                    state.timeoutFuture.cancel(false);
                }
                streamWrites.remove(streamId);
            }
        }

        /**
         * server-push low-window guard 
         */
        @Override
        public ChannelFuture writePushPromise(ChannelHandlerContext ctx, int streamId,
                                              int promisedStreamId, Http2Headers headers,
                                              int padding, ChannelPromise promise) {
            final int     initialWindowSize = flowController().initialWindowSize();
            final boolean lowWindow = rateState.getMaxLowWindowStreams() > 0
                                      && initialWindowSize <= rateState.getLowWindowLimit();

            // Reject before sending anything to the client.
            if (lowWindow && rateState.wouldExceedLowWindowStreams(promisedStreamId)) {
                ChannelPromise unvoided = promise.unvoid();
                Http2Exception exception = Http2Exception.connectionError(
                        Http2Error.ENHANCE_YOUR_CALM,
                        "Too many streams with low initial window size have been opened; closing the connection");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writePushPromise: too many low-window streams (including push promises);"
                                 + " suppressing PUSH_PROMISE for stream " + promisedStreamId
                                 + " and closing connection");
                }
                lifecycleManager.onError(ctx, true, exception);
                ctx.close();
                return unvoided.setFailure(exception);
            }

            ChannelFuture future = super.writePushPromise(ctx, streamId, promisedStreamId,
                                                          headers, padding, promise);

            // Only add if the stream was actually reserved.
            if (lowWindow && connection().stream(promisedStreamId) != null) {
                rateState.addLowWindowStream(promisedStreamId);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writePushPromise: push stream " + promisedStreamId
                                 + " has low initial window: " + initialWindowSize
                                 + " <= " + rateState.getLowWindowLimit());
                }
            }

            return future;
        }
    }

}
