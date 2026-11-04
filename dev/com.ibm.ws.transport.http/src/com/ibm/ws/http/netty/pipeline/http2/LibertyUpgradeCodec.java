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
import com.ibm.ws.http.netty.NettyHttpConstants.ProtocolName;
import com.ibm.ws.http.netty.ProtocolState;
import com.ibm.ws.http.netty.ProtocolState.ProtocolSource;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler;
import io.netty.handler.codec.http2.DecoratingHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionDecoder;
import io.netty.handler.codec.http2.DefaultHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.DefaultHttp2FrameReader;
import io.netty.handler.codec.http2.DefaultHttp2FrameWriter;
import io.netty.handler.codec.http2.DefaultHttp2LocalFlowController;
import io.netty.handler.codec.http2.DefaultHttp2RemoteFlowController;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ControlFrameLimitEncoder;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersEncoder;
import io.netty.handler.codec.http2.Http2LifecycleManager;
import io.netty.handler.codec.http2.Http2RemoteFlowController;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.LibertyDefaultHttp2HeadersDecoder;
import io.netty.handler.codec.http2.StreamByteDistributor;
import io.netty.handler.codec.http2.WeightedFairQueueByteDistributor;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.quiesce.QuiesceStrategy;
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
        HttpServerCodec sourceCodec = HttpPipelineInitializer.newHttpServerCodec(httpConfig);
        LibertyUpgradeCodec codec = new LibertyUpgradeCodec(httpConfig, channel);
        int maxContentlength = (httpConfig.getMessageSizeLimit() >= Integer.MAX_VALUE || httpConfig.getMessageSizeLimit() < 0) ? Integer.MAX_VALUE : (int) httpConfig.getMessageSizeLimit();
        final HttpServerUpgradeHandler upgradeHandler = new Http11OnlyUpgradeHandler(sourceCodec, codec, maxContentlength);
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
                public void upgradeTo(ChannelHandlerContext ctx, FullHttpRequest request) {
                    // Call upgrade
                    super.upgradeTo(ctx, request);
                    // Successful topology installation is the trusted h2c transition boundary.
                    ProtocolState.establish(ctx.channel(), ProtocolName.HTTP2,
                                            ProtocolSource.H2C_UPGRADE);
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
                public void upgradeTo(ChannelHandlerContext ctx, FullHttpRequest request) {
                    ctx.fireChannelRead(ReferenceCountUtil.retain(request));
                    QuiesceHandler quiesceHandler = ctx.pipeline().get(QuiesceHandler.class);
                    if (quiesceHandler != null) {
                        quiesceHandler.setQuiesceTask(QuiesceStrategy.WEBSOCKET_CLOSE.getTask());
                    }
                }

                @Override
                public boolean prepareUpgradeResponse(ChannelHandlerContext ctx, FullHttpRequest upgradeRequest, HttpHeaders upgradeHeaders) {
                    ctx.channel().attr(NettyHttpConstants.WEBSOCKET_UPGRADE_REQUEST).set(Boolean.TRUE);
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
        int maxContentlength = (httpConfig.getMessageSizeLimit() >= Integer.MAX_VALUE || httpConfig.getMessageSizeLimit() < 0) ? Integer.MAX_VALUE : (int) httpConfig.getMessageSizeLimit();
        Http2Settings initialSettings = new Http2Settings().maxConcurrentStreams(httpConfig.getH2MaxConcurrentStreams()).maxFrameSize(httpConfig.getH2MaxFrameSize());
        if (httpConfig.getH2SettingsInitialWindowSize() != Constants.SPEC_INITIAL_WINDOW_SIZE)
            initialSettings.initialWindowSize(httpConfig.getH2SettingsInitialWindowSize());
        LibertyInboundHttp2ToHttpAdapter listener = new LibertyInboundHttp2ToHttpAdapter(connection, maxContentlength, false, false, channel, httpConfig, rateState);

        connection.addListener(listener);

        // PH71839: have the remote flow controller report the bytes it holds per stream into rateState,
        // instead of predicting them. Must be installed before DefaultHttp2ConnectionEncoder is created,
        // otherwise the encoder creates a default flow controller of its own.
        LibertyRemoteFlowController remoteFlowController = new LibertyRemoteFlowController(connection,
            new ParkedBytesReporter(new WeightedFairQueueByteDistributor(connection), rateState));
        connection.remote().flowController(remoteFlowController);

        // Create encoder with same configuration as builder would
        DefaultHttp2FrameWriter frameWriter = new DefaultHttp2FrameWriter(Http2HeadersEncoder.NEVER_SENSITIVE, true);
        DefaultHttp2ConnectionEncoder defaultEncoder = new DefaultHttp2ConnectionEncoder(
            connection,
            frameWriter
        );
        Http2ConnectionEncoder wrappedEncoder = new Http2ControlFrameLimitEncoder(defaultEncoder, Http2CodecUtil.DEFAULT_MAX_QUEUED_CONTROL_FRAMES);

        // Wrap with millisecond tracking for Liberty logic reusing the listener as the tracker
        final LibertyResetEncoderWrapper libertyEncoder = new LibertyResetEncoderWrapper(
            wrappedEncoder,
            listener,
            rateState,
            httpConfig.getWriteTimeout()
        );
        wrappedEncoder = libertyEncoder;
        // Check maxQueuedBytes once each flush has sent what it can (see LibertyRemoteFlowController)
        remoteFlowController.afterWritePendingBytes(libertyEncoder::afterPendingBytesWritten);

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
                    Tr.warning(tc, "netty.h2.window.ratio.error", e.getMessage());
                }
            });
        }
        return handler;
    }

    /**
     * The default remote flow controller, plus one hook: a callback once {@link #writePendingBytes()} has
     * sent what it can. Http2ConnectionHandler.flush() calls writePendingBytes() on every flush (after
     * the writes of a Liberty write task, and after reading WINDOW_UPDATEs), so at that point whatever the
     * flow controller still holds really is parked.
     * <p>
     * A nested call (a flush triggered while bytes are being distributed) does not run the callback;
     * only the outermost call does, when the counts are final.
     */
    private static final class LibertyRemoteFlowController extends DefaultHttp2RemoteFlowController {
        private Runnable afterWritePendingBytes;
        private int depth;

        LibertyRemoteFlowController(Http2Connection connection, StreamByteDistributor distributor) {
            super(connection, distributor);
        }

        void afterWritePendingBytes(Runnable callback) {
            this.afterWritePendingBytes = callback;
        }

        @Override
        public void writePendingBytes() throws Http2Exception {
            depth++;
            try {
                super.writePendingBytes();
            } finally {
                depth--;
            }
            if (depth == 0 && afterWritePendingBytes != null) {
                afterWritePendingBytes.run();
            }
        }
    }

    /**
     * Reports the remote flow controller's per-stream pending bytes into {@link NettyH2RateState}.
     * <p>
     * DefaultHttp2RemoteFlowController calls {@code updateStreamableBytes} every time a stream's pending
     * bytes or window change: a DATA frame queued (or merged into the previous one), bytes written out,
     * queued frames dropped because the stream was reset or closed. {@code state.pendingBytes()} is the
     * flow controller's own count, so the numbers are exact. They drop when bytes are handed to the
     * channel, the same point at which legacy stops counting (retryDeferredWrite polls before writing).
     * <p>
     * Every call is passed through unchanged, so stream priorities and byte distribution stay exactly
     * Netty's. Event loop only.
     */
    private static final class ParkedBytesReporter implements StreamByteDistributor {
        private final StreamByteDistributor delegate;
        private final NettyH2RateState rateState;

        ParkedBytesReporter(StreamByteDistributor delegate, NettyH2RateState rateState) {
            this.delegate = delegate;
            this.rateState = rateState;
        }

        @Override
        public void updateStreamableBytes(StreamState state) {
            delegate.updateStreamableBytes(state);
            int streamId = state.stream().id();
            if (streamId != 0) { // the connection stream reports window changes only; it never holds frames
                rateState.parkedBytesChanged(streamId, state.pendingBytes());
            }
        }

        @Override
        public void updateDependencyTree(int childStreamId, int parentStreamId, short weight, boolean exclusive) {
            delegate.updateDependencyTree(childStreamId, parentStreamId, weight, exclusive);
        }

        @Override
        public boolean distribute(int maxBytes, Writer writer) throws Http2Exception {
            return delegate.distribute(maxBytes, writer);
        }
    }

    private class LibertyResetEncoderWrapper extends DecoratingHttp2ConnectionEncoder implements NettyH2RateState.ParkedBytesListener {
        private final ResetFrameTracker tracker;
        private final NettyH2RateState rateState;
        private final int writeTimeout;
        private Http2LifecycleManager lifecycleManager;

        /** The Http2ConnectionHandler's context; captured on the first DATA write. */
        private ChannelHandlerContext ctx;

        /** Parked bytes may have grown, or a stream may need a timer: check after the next writePendingBytes. */
        private boolean parkedBytesCheckNeeded;

        /** True while handleWriteTimeout sends its RST; keeps that reset out of the maxResetFrames count. */
        private boolean sendingTimeoutReset;

        /** True while the fallback check task is queued (see markParkedBytesCheckNeeded). */
        private boolean fallbackCheckScheduled;

        /** Deferred-write timeout per stream; at most one per stream, as in legacy. */
        private final Map<Integer, ScheduledFuture<?>> writeTimeouts = new HashMap<>();

        public LibertyResetEncoderWrapper(Http2ConnectionEncoder delegate, ResetFrameTracker libertyAdapter, NettyH2RateState rateState,
                                          int writeTimeout) {
            super(delegate);
            this.tracker = libertyAdapter;
            this.rateState = rateState;
            this.writeTimeout = writeTimeout;
            rateState.parkedBytesListener(this);
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
            // The write-timeout reset is the server's own doing, not something the peer induced, so it is not
            // counted (legacy does not count it either). Netty re-enters this method for the same stream while
            // the timeout reset is being sent (the failed writes call onError -> encoder().writeRstStream),
            // so the flag has to cover the whole of handleWriteTimeout.
            if (!sendingTimeoutReset && tracker.isResetTrackingEnabled()) {
                tracker.incrementResetCount();
                if (tracker.isResetsInTimeExceeded()) {
                    lifecycleManager.onError(ctx, true, LibertyInboundHttp2ToHttpAdapter.RST_FRAME_RATE_EXCEEDED);
                    ctx.close();
                }
            }
            return future;
        }

        /**
         * queued-bytes guard.
         * <p>
         * Netty queues every DATA frame in its remote flow controller and sends only on flush, so whether
         * a frame will stay parked is only known after the flush. Two checks, both on Netty's own counts:
         * <ol>
         * <li>here, before queuing: a lower bound on what will still be parked after the next flush. If
         * even that crosses maxQueuedBytes, this frame is rejected (as legacy deferDataWrite rejects the
         * frame that would cross). This catches a peer that starves its windows with no overshoot.</li>
         * <li>{@link #afterPendingBytesWritten}: the exact amount still parked after the flush. This catches the rest (bytes held back
         * by a slow socket, not by the windows), at most one flushed batch late.</li>
         * </ol>
         */
        @Override
        public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId,
                                       ByteBuf data, int padding, boolean endStream,
                                       ChannelPromise promise) {
            this.ctx = ctx;
            final long frameSize = data.readableBytes() + padding;

            if (rateState.isQueuedBytesLimitExceeded()) {
                // The GOAWAY has been sent and the connection is closing. Matches legacy deferDataWrite's
                // ALREADY_EXCEEDED path: fail this write only.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "writeData: queued bytes limit already exceeded; dropping " + frameSize
                                 + " bytes on stream " + streamId);
                }
                ChannelPromise unvoided = promise.unvoid();
                ReferenceCountUtil.release(data);
                return unvoided.setFailure(Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                                                                      "Connection closing due to queued bytes limit exceeded"));
            }

            Http2Stream stream = connection().stream(streamId);
            if (stream != null) {
                long surelyParked = surelyParkedAfterFlush(stream, frameSize);
                if (rateState.checkQueuedBytes(surelyParked) == NettyH2RateState.QueuedBytesResult.FIRST_TO_EXCEED) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "writeData: stream " + streamId + " frame of " + frameSize + " bytes would leave at least "
                                     + surelyParked + " bytes parked; rejecting it and closing the connection");
                    }
                    ChannelPromise unvoided = promise.unvoid();
                    ReferenceCountUtil.release(data);
                    Http2Exception qbEx = queuedBytesExceeded();
                    unvoided.setFailure(qbEx);
                    closeForQueuedBytes(ctx, qbEx);
                    return unvoided;
                }
            }

            ChannelFuture future = super.writeData(ctx, streamId, data, padding, endStream, promise);
            markParkedBytesCheckNeeded();
            return future;
        }

        /**
         * A lower bound on the bytes the flow controller will still hold after the next flush, if this
         * frame is queued now.
         * <p>
         * Nothing is sent before the next flush, and that flush can send at most the connection window
         * in total, of which at most min(stream window, this stream's bytes) can come from this stream.
         * So at least (everything held + this frame - that most) stays parked. Never more than will
         * really be parked: a lower bound can't cause a false trip.
         */
        private long surelyParkedAfterFlush(Http2Stream stream, long frameSize) {
            Http2RemoteFlowController controller = flowController();
            long total = rateState.getParkedBytes();
            long thisStream = rateState.getParkedBytes(stream.id());
            long otherStreams = total - thisStream;
            long connectionWindow = Math.max(0, controller.windowSize(connection().connectionStream()));
            long thisStreamCanSend = Math.max(0, Math.min(controller.windowSize(stream), thisStream + frameSize));
            long canLeave = Math.min(connectionWindow, thisStreamCanSend + otherStreams);
            return total + frameSize - canLeave;
        }

        /**
         * Ask for a check after the next writePendingBytes. Every Liberty write path flushes in the same
         * event-loop task as its writes; in case some future path doesn't, also queue one check task behind
         * the current one, which does nothing if the flush already ran the check.
         */
        private void markParkedBytesCheckNeeded() {
            parkedBytesCheckNeeded = true;
            if (!fallbackCheckScheduled && ctx != null) {
                fallbackCheckScheduled = true;
                ctx.executor().execute(() -> {
                    fallbackCheckScheduled = false;
                    if (parkedBytesCheckNeeded) {
                        parkedBytesCheckNeeded = false;
                        checkParkedBytes();
                    }
                });
            }
        }

        /**
         * Called by LibertyRemoteFlowController after every flush's writePendingBytes(), when the flow
         * controller has sent all the windows and the socket allow. Cheap when nothing changed.
         */
        void afterPendingBytesWritten() {
            if (parkedBytesCheckNeeded && ctx != null) {
                parkedBytesCheckNeeded = false;
                checkParkedBytes();
            }
        }

        /**
         * Enforce maxQueuedBytes against what the flow controller still holds, then start write timeouts
         * for streams that are stuck on flow control.
         */
        private void checkParkedBytes() {
            if (!ctx.channel().isActive()) {
                return;
            }

            switch (rateState.checkQueuedBytes()) {
                case FIRST_TO_EXCEED:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "checkParkedBytes: queued bytes limit exceeded on channel " + ctx.channel()
                                     + " (parked " + rateState.getParkedBytes() + ", timed out " + rateState.getTimedOutBytes()
                                     + "); closing the connection");
                    }
                    closeForQueuedBytes(ctx, queuedBytesExceeded());
                    return;
                case ALREADY_EXCEEDED:
                    return; // connection is already closing
                case SUCCESS:
                default:
                    break;
            }

            if (writeTimeout <= 0) {
                return; // 0 = no write timeout (legacy fires immediately; see the design doc)
            }
            for (Integer streamId : rateState.getStreamsWithParkedBytes()) {
                if (!writeTimeouts.containsKey(streamId) && isBlockedByFlowControl(streamId)) {
                    final int id = streamId;
                    writeTimeouts.put(id, ctx.executor().schedule(() -> handleWriteTimeout(id),
                                                                  writeTimeout, TimeUnit.MILLISECONDS));
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "checkParkedBytes: stream " + id + " has " + rateState.getParkedBytes(id)
                                     + " bytes blocked by flow control; write timeout armed for " + writeTimeout + "ms");
                    }
                }
            }
        }

        private Http2Exception queuedBytesExceeded() {
            return Http2Exception.connectionError(Http2Error.ENHANCE_YOUR_CALM,
                                                  "Total queued bytes across all streams exceeded limit!");
        }

        /** GOAWAY(ENHANCE_YOUR_CALM) and close, as for the other PH71839 connection limits. */
        private void closeForQueuedBytes(ChannelHandlerContext ctx, Http2Exception qbEx) {
            lifecycleManager.onError(ctx, true, qbEx);
            ctx.close();
        }

        /**
         * True if the HTTP/2 windows can't take what this stream has parked. False means the bytes are
         * waiting only because the socket is backed up, which is left to WriteTimeoutHandler, as on
         * legacy where writeSync blocks on the socket instead of deferring.
         */
        private boolean isBlockedByFlowControl(int streamId) {
            Http2Stream stream = connection().stream(streamId);
            if (stream == null || stream.isResetSent()) {
                return false; // gone, or already reset by us (its frames are about to be dropped)
            }
            Http2RemoteFlowController controller = flowController();
            int window = Math.min(controller.windowSize(stream),
                                  controller.windowSize(connection().connectionStream()));
            return window < rateState.getParkedBytes(streamId);
        }

        /**
         * A stream's parked DATA did not drain within writeTimeout. Its parked bytes stay charged to the
         * connection, then the stream is reset (which makes the flow controller drop its frames).
         */
        private void handleWriteTimeout(int streamId) {
            writeTimeouts.remove(streamId);
            if (!ctx.channel().isActive()) {
                return;
            }
            long stuck = rateState.getParkedBytes(streamId);
            if (stuck == 0) {
                return; // drained just as the timer fired
            }
            rateState.addTimedOutBytes(stuck);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "handleWriteTimeout: stream " + streamId + " did not drain within " + writeTimeout
                             + "ms; resetting stream, " + stuck + " bytes stay charged");
            }
            sendingTimeoutReset = true;
            try {
                super.writeRstStream(ctx, streamId, Http2Error.FLOW_CONTROL_ERROR.code(), ctx.newPromise());
                ctx.flush();
            } finally {
                sendingTimeoutReset = false;
            }
        }

        /**
         * From {@link NettyH2RateState}, on the event loop, whenever a stream's parked bytes change.
         */
        @Override
        public void parkedBytesChanged(int streamId, long oldParked, long newParked) {
            if (newParked == 0) {
                // Fully written, or dropped because the stream was reset or closed.
                ScheduledFuture<?> timeout = writeTimeouts.remove(streamId);
                if (timeout != null) {
                    timeout.cancel(false);
                }
            } else if (!writeTimeouts.containsKey(streamId)) {
                // A stream that was waiting only on the socket may now be waiting on its window
                // (or a SETTINGS frame shrank it): look again at the end of the flush.
                parkedBytesCheckNeeded = true;
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

    /**
     * Restricts HTTP Upgrade processing to HTTP/1.1 requests. Requests with another
     * decoded HTTP version fall through to HTTP/1 handling, where the dispatcher
     * applies the supported-version policy.
     */
    private static final class Http11OnlyUpgradeHandler extends HttpServerUpgradeHandler {

        Http11OnlyUpgradeHandler(SourceCodec sourceCodec, UpgradeCodecFactory factory, int maxContentLength) {
            super(sourceCodec, factory, maxContentLength);
        }

        @Override
        protected boolean shouldHandleUpgradeRequest(HttpRequest request) {
            return HttpVersion.HTTP_1_1.equals(request.protocolVersion());
        }
    }

}
