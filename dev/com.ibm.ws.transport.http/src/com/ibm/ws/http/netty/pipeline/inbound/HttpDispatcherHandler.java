/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound;

import java.net.InetSocketAddress;
import java.util.Map;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.AsyncReadDispatchState;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.http.dispatcher.internal.channel.HttpRequestImpl;
import com.ibm.ws.http.netty.NettyHttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.message.BodyQueue;
import com.ibm.ws.http.netty.pipeline.CRLFValidationHandler;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.http.HttpInputStream;
import com.ibm.wsspi.http.channel.error.HttpError;
import com.ibm.wsspi.http.channel.error.HttpErrorPageProvider;
import com.ibm.wsspi.http.channel.error.HttpErrorPageService;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.channelfw.VirtualConnection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.handler.codec.http.TooLongHttpLineException;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception.StreamException;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.http.netty.timeout.exception.TimeoutException;

import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;
import com.ibm.ws.http.netty.pipeline.inbound.read.FlowState;

import io.openliberty.netty.internal.impl.QuiesceHandler;
import java.io.EOFException;
import io.netty.channel.socket.DuplexChannelConfig;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Dispatcher: wires upgrade and hands off body streaming to BodyQueue (HTTP) or UpgradeHandler (post-101).
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final TraceComponent tc = Tr.register(HttpDispatcherHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public static final String NAME = "httpDispatcherHandler";

    NettyHttpChannelConfig config;
    private ChannelHandlerContext context;

    private final DefaultFullHttpResponse errorResponse;

    private BodyQueue queue;
    private HttpDispatcherLink link;

    private final java.util.ArrayDeque<HttpContent> earlyContents = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<ByteBuf> earlyUpgradeBytes = new java.util.ArrayDeque<>();

    private final AtomicBoolean commitScheduled = new AtomicBoolean(false);
    private final AtomicBoolean upgradeCommitted = new AtomicBoolean(false);
    private final AtomicBoolean postFlipDrainerInstalled = new AtomicBoolean(false);

    private volatile boolean upgradingNow;
    private volatile boolean streamingInitialized;
    private volatile boolean shutdownReceived = false;

    // NEW: per-request marker to avoid double-enqueue when FullHttpRequest is used
    private boolean aggregatedBodyEnqueued;

    private enum CommitTrigger {
        FLUSH_OBSERVER, EARLY_BYTES, RETRY_TASK
    }

    private final AtomicReference<CommitTrigger> commitTrigger = new AtomicReference<>(null);

    private final Map<String, HttpInputStream> streamMap = new ConcurrentHashMap<>();

    public HttpDispatcherHandler(NettyHttpChannelConfig config) {
        super(false);
        this.config = Objects.requireNonNull(config);
        this.errorResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        context = ctx;
        ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).set(0);
        ctx.channel().attr(NettyHttpConstants.STREAMS_REFUSED).set(0);
        ctx.channel().attr(NettyHttpConstants.HTTP_CONFIG).set(config);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof Upgrade101CommittedEvent) {
            Tr.debug(tc,"[UPGRADE-SYSOUT] >>> Upgrade101CommittedEvent RECEIVED <<< autoRead(before)="
                + ctx.channel().config().isAutoRead()
                + " upgradingNow=" + upgradingNow
                + " upgradeCommitted=" + upgradeCommitted.get()
                + " commitTrigger=" + commitTrigger.get()
                + " pipeline(before)=" + ctx.pipeline().names());
            commitTrigger.compareAndSet(null, CommitTrigger.FLUSH_OBSERVER);
            upgradingNow = true;
                onUpgradeCommitted(ctx);
            return;
        }

        Throwable lifecycleFailure = null;
        if (evt instanceof ChannelInputShutdownEvent){
            try {
            FlowState state = ReadFlowHandler.state(ctx);
            if(queue!=null && !queue.isEos() && !state.isRequestConsumed()){
                    try {
                        ctx.channel().attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).set(Boolean.TRUE);
                    } catch (Throwable t) {
                        lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
                    }
                    try {
                        queue.wakeReaders();
                    } catch (Throwable t) {
                        lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
                    }
                    try {
                        firePendingAsyncReadError(ctx);
                    } catch (Throwable t) {
                        lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
                    }
                }
            } catch (Throwable t) {
                lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
            }
        }

        if (evt == QuiesceHandler.QUIESCE_EVENT) {
            try {
                ctx.channel().attr(NettyHttpConstants.QUIESCING).set(Boolean.TRUE);
            } catch (Throwable t) {
                lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
            }
        }
        try {
            super.userEventTriggered(ctx, evt);
        } catch (Throwable t) {
            lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
        }
        rethrowLifecycleFailure(lifecycleFailure);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (upgradingNow && msg instanceof ByteBuf) {
            final ByteBuf buf = (ByteBuf) msg;

            try {
                if (upgradeCommitted.get()) {
                    deliverToUpgradeOrPark(ctx, buf);
                    return;
                }
                final ByteBuf snapshot = buf.retainedSlice(buf.readerIndex(), buf.readableBytes());
                earlyUpgradeBytes.add(snapshot);

                commitTrigger.compareAndSet(null, CommitTrigger.EARLY_BYTES);
                if (commitScheduled.compareAndSet(false, true)) {
                    ctx.executor().execute(() -> {
                        if (upgradingNow && !upgradeCommitted.get())
                            onUpgradeCommitted(ctx);
                    });
                } 
                return;
            } finally {
                ReferenceCountUtil.release(buf);
            }
        }

        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (!(msg.decoderResult().isFinished() && msg.decoderResult().isSuccess())) {
            if(context.channel().isActive()) {
                if (msg.decoderResult().cause() != null) {
                    if (!msg.decoderResult().cause().getMessage().contains("possibly HTTP/0.9")) {
                        FFDCFilter.processException(msg.decoderResult().cause(), HttpDispatcherHandler.class.getName() + ".channelRead0(ChannelHandlerContext, HttpObject)", "1", context);
                    }
                    sendErrorMessage(msg.decoderResult().cause());
                } else {
                    sendErrorMessage(new Exception("HTTP request decoding failure!"));
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed decode request on closed channel: " + context.channel());
                }
            }
            return;
        }
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            upgradingNow = false;
            streamingInitialized = false;
            aggregatedBodyEnqueued = false;

            // Validate trusted H2 metadata before arming half-init queue/link state.
            String trustedStreamId = normalizeProtocolMetadata(ctx, req);

            queue = new BodyQueue(ctx.alloc());
            earlyContents.clear();
            earlyUpgradeBytes.clear();
            link = null;

            boolean isUpgrade = isUpgrade(req);

            if (isUpgrade) {
                setUpgradeReadyPromise(ctx);

                if (req instanceof FullHttpRequest) {
                    final ByteBuf content = ((FullHttpRequest) req).content();
                    if (content.isReadable()) {
                        content.retain();
                        earlyUpgradeBytes.add(content);
                        Tr.debug(tc, "HTTP: parked aggregated upgrade body bytes=" + content.readableBytes());
                    }
                }

            }

            beginStreamingRequest(ctx, req, trustedStreamId);

            if (!upgradingNow && !(req instanceof FullHttpRequest)) {
                drainEarlyHttpContentToBodyQueue(ctx);
            } else {
                HttpContent c;
                while ((c = earlyContents.poll()) != null) {
                    c.release();
                }
            }

            return;
        }

        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;

            int sizeOfCurrentChunk = content.content().readableBytes();
            if (HttpConfigConstants.UNLIMITED != config.getMessageSizeLimit() && (sizeOfCurrentChunk > config.getMessageSizeLimit() ||
                (queue.bytesRead() + sizeOfCurrentChunk) > config.getMessageSizeLimit())) {
                throw new TooLongFrameException("Content length exceeded max of " + config.getMessageSizeLimit() + " bytes.");
            }

            if (upgradingNow) {
                content.retain();
                earlyContents.add(content);
                ReferenceCountUtil.release(content);
                return;
            }

            if (aggregatedBodyEnqueued) {
                ReferenceCountUtil.release(content);
                return;
            }

            if (!streamingInitialized && queue == null) {
                content.retain();
                earlyContents.add(content);
                ReferenceCountUtil.release(msg);
                return;
            }

            boolean notifyAsync = false;
            try {
                ByteBuf data = content.content();
                if (data.isReadable()){
                    queue.enqueueRetained(data);

                    notifyAsync = true;
                }
                if (content instanceof LastHttpContent) {
                    HttpHeaders trailers = ((LastHttpContent) content).trailingHeaders();
                    if (!trailers.isEmpty()) {
                        HttpRequestImpl req = (HttpRequestImpl) link.getRequest();
                        req.setTrailers(trailers);
                    }
                    queue.signalEos();
                    if (this.link != null)
                        this.link.setBodyComplete();
                    notifyAsync = true;
                }
            } finally {
                ReferenceCountUtil.release(content);
            }

            if(notifyAsync){
                firePendingAsyncRead(ctx);
            }
            return;
        }
    }

    //TODO -> Utils candidate
    private static boolean isUpgrade(HttpRequest req) {
        final CharSequence conn = req.headers().get(HttpHeaderNames.CONNECTION);
        final CharSequence upg = req.headers().get(HttpHeaderNames.UPGRADE);
        if (upg == null || conn == null)
            return false;
        return io.netty.util.AsciiString.containsIgnoreCase(conn, "upgrade");
    }

    private static String normalizeProtocolMetadata(ChannelHandlerContext ctx, HttpRequest request) {
        final CharSequence streamIdHeader = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text();
        if (!NettyHttpConstants.isHttp2Pipeline(ctx.channel())) {
            request.headers().remove(streamIdHeader);
            ctx.channel().attr(NettyHttpConstants.PROTOCOL).set(
                            request.protocolVersion().equals(HttpVersion.HTTP_1_0) ? "HTTP10" : NettyHttpConstants.ProtocolName.HTTP1.name());
            return null;
        }

        String rawStreamId = request.headers().get(streamIdHeader);
        final int streamId;
        try {
            streamId = Integer.parseInt(rawStreamId);
        } catch (RuntimeException e) {
            throw new NettyHttpConstants.InvalidHttp2StreamMetadataException("Trusted HTTP/2 request is missing a valid stream id", e);
        }
        if (streamId <= 0) {
            throw new NettyHttpConstants.InvalidHttp2StreamMetadataException("Trusted HTTP/2 request has a nonpositive stream id: " + streamId);
        }

        String validatedStreamId = Integer.toString(streamId);
        request.headers().set(streamIdHeader, validatedStreamId);
        ctx.channel().attr(NettyHttpConstants.PROTOCOL).set(NettyHttpConstants.ProtocolName.HTTP2.name());
        return validatedStreamId;
    }

    private void beginStreamingRequest(ChannelHandlerContext ctx, HttpRequest request, String trustedStreamId) {
         ctx.channel().attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).set(Boolean.FALSE);
        // Per-request boundary: prior incomplete-body close evidence must not leak onto the next request.
        ctx.channel().attr(NettyHttpConstants.RESPONSE_CLOSE_BEFORE_REQUEST_BODY_COMPLETE).set(Boolean.FALSE);

        final CharSequence ae = request.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
        if (ae != null)
            ctx.channel().attr(NettyHttpConstants.ACCEPT_ENCODING).set(ae.toString());

        if (ctx.channel().hasAttr(NettyHttpConstants.CONTENT_LENGTH)) {
            ctx.channel().attr(NettyHttpConstants.CONTENT_LENGTH).set(null);
        }
        int num = ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).get();
        ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).set(num + 1);

        this.link = new HttpDispatcherLink();

        final boolean chunked = HttpUtil.isTransferEncodingChunked(request);
        final long cl = HttpUtil.getContentLength(request, -1);
        final boolean expect100 = HttpUtil.is100ContinueExpected(request);
        final boolean hasBody = chunked || cl > 0;
        final boolean upg = isUpgrade(request);
        final boolean isFullRequest = (request instanceof FullHttpRequest);

        commitScheduled.set(false);
        upgradeCommitted.set(false);
        commitTrigger.set(null);

        // Verify if the request expects 100 continue
        // At this point, the validation of the message size is already done by the aggregator
        if (expect100) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Request contains [Expect: 100-continue]");
            }
            DefaultFullHttpResponse continueResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
            HttpUtil.setContentLength(continueResponse, 0);
            byte[] date = HttpDispatcher.getDateFormatter().getRFC1123TimeAsBytes(config.getDateHeaderRange());
            continueResponse.headers().set(HttpHeaderKeys.HDR_DATE.getName(),
                            new String(date, StandardCharsets.UTF_8));
            context.writeAndFlush(continueResponse);
        }

        link.initStreaming(ctx, request, config, isFullRequest);

        final HttpRequestImpl req = (HttpRequestImpl) link.getRequest();
        final HttpInputStreamImpl body = req.getBody();
        String streamId = trustedStreamId;
        try {
            if (this.link.getVirtualConnection() != null) {
                VirtualConnection v = this.link.getVirtualConnection();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "beginStreamingRequest: vc=" + v
                                 + " stateMap=" + (v != null ? v.getStateMap() : "null")
                                 + " streamId=" + streamId
                                 + " channel=" + ctx.channel().id());
                }
                v.getStateMap().put(NettyHttpConstants.VC_HTTP_INPUT_STREAM, body);

                // If this is HTTP/2, also remember the stream id on the VC

                if (streamId != null) {
                    v.getStateMap().put(NettyHttpConstants.VC_HTTP2_STREAM_ID, streamId);
                }
            }
            else{
                //System.out.println("DEBUG: vc was null, not expected");
            }
        } catch (Throwable t) {
            // be defensive; don't let VC issues kill the request setup
            Tr.debug(tc, "Failed to attach HttpInputStream to VC state", t);
        }
        //if H2

        if (streamId != null) {
            putStream(streamId, body);
        } else {
            ctx.channel().attr(NettyHttpConstants.HTTP_INPUT_STREAM).set(body);
        }
        

        if (upg && trustedStreamId == null) {
            //upgradingNow = true;
            if(commitScheduled.compareAndSet(false, true)){
               HttpDispatcher.getExecutorService().execute(() -> link.ready()); 
            }
            return;
        }

        
        final String contentEncoding = request.headers().get(HttpHeaderNames.CONTENT_ENCODING);
        body.nettyConfigureStreaming(queue, ctx, contentEncoding, cl, chunked);

        if (isFullRequest) {
            final FullHttpRequest full = (FullHttpRequest) request;

            final ByteBuf b = full.content();
            if (b.isReadable()) {
                queue.enqueueRetained(b);
                aggregatedBodyEnqueued = true; 
            }

            if (request instanceof LastHttpContent) {
                HttpHeaders trailers = ((LastHttpContent) request).trailingHeaders();
                if (!trailers.isEmpty()) {
                    req.setTrailers(trailers);
                }
            }
        }

        if (!hasBody && !expect100 || isFullRequest) {
            queue.signalEos();
            if (this.link != null)
                this.link.setBodyComplete();
        }

        streamingInitialized = true;

        HttpDispatcher.getExecutorService().execute(() -> link.ready());
    }

    private void drainEarlyHttpContentToBodyQueue(ChannelHandlerContext ctx) {
        HttpContent early;
        while ((early = earlyContents.poll()) != null) {
            try {
                final ByteBuf data = early.content();
                if (data.isReadable())
                    queue.enqueueRetained(data);
                if (early instanceof LastHttpContent) {
                    queue.signalEos();
                    if (this.link != null)
                        this.link.setBodyComplete();
                }
            } finally {
                early.release();
            }
        }
    }

    private void onUpgradeCommitted(ChannelHandlerContext ctx) {
        Tr.debug(tc,"[UPGRADE-SYSOUT] onUpgradeCommitted ENTER autoRead(before)="
            + ctx.channel().config().isAutoRead()
            + " upgradingNow=" + upgradingNow
            + " upgradeCommitted(before)=" + upgradeCommitted.get()
            + " commitTrigger=" + commitTrigger.get()
            + " pipeline(before)=" + ctx.pipeline().names());
        
        if (!ctx.executor().inEventLoop()) {
            ctx.executor().execute(() -> onUpgradeCommitted(ctx));
            return;
        }
        if (!upgradeCommitted.compareAndSet(false, true)) {
            return;
        }

        final ChannelPipeline p = ctx.pipeline();
        CompletableFuture<Void> promise = null;
        try{
            promise = ctx.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get();

            // Remove HTTP handlers that must not see post-101 bytes
            removeIfPresent(p, HttpServerCodec.class);
            removeIfPresent(p, HttpObjectAggregator.class);
            removeIfPresent(p, CRLFValidationHandler.class);
            removeIfPresent(p, TimeoutHandler.class);
            removeIfPresent(p, WriteTimeoutHandler.class);
            removeIfPresent(p, ReadFlowHandler.class);
            removeIfPresent(p, FlowControlHandler.class);
            removeIfPresent(p, HttpServerKeepAliveHandler.class);

            // Ensure the upgrade handler is present directly before HTTP_DISPATCHER
            NettyServletUpgradeHandler upgrade = p.get(NettyServletUpgradeHandler.class);
            if (upgrade == null) {
                if (p.get("HTTP_DISPATCHER") != null) {
                    p.addBefore("HTTP_DISPATCHER", NettyServletUpgradeHandler.NAME,
                                new NettyServletUpgradeHandler(ctx.channel()));
                } else {
                    p.addLast(NettyServletUpgradeHandler.NAME, new NettyServletUpgradeHandler(ctx.channel()));
                }
                upgrade = p.get(NettyServletUpgradeHandler.class);
            }

            final ChannelHandlerContext upgCtx = p.context(NettyServletUpgradeHandler.class);
            if (upgCtx != null && upgrade != null) {

                HttpContent early;
                while ((early = this.earlyContents.poll()) != null) {
                    try {
                        final ByteBuf d = early.content();
                        if (d.isReadable()) {
                            upgrade.channelRead(upgCtx, d.retain());
                        }
                    } catch (Exception e) {
                        Tr.debug(tc, "deliver early HttpContent failed: " + e);
                    } finally {
                        early.release();
                    }
                }

                ByteBuf raw;
                while ((raw = this.earlyUpgradeBytes.poll()) != null) {
                    try {
                        if (raw.isReadable()) {
                            upgrade.channelRead(upgCtx, raw.retain());
                        }
                    } catch (Exception e) {
                        Tr.debug(tc, "deliver early raw bytes failed: " + e);
                    } finally {
                        raw.release();
                    }
                }

                try {
                    upgrade.channelReadComplete(upgCtx);
                } catch (Exception ignore) {}
            } 

            ctx.channel().attr(NettyHttpConstants.UPGRADED).set(Boolean.TRUE);
            ctx.channel().attr(NettyHttpConstants.HTTP_INPUT_STREAM).set(null);
            try {
                if (this.link != null && this.link.getVirtualConnection() != null) {
                    this.link.getVirtualConnection().getStateMap().put(com.ibm.ws.transport.access.TransportConstants.UPGRADED_CONNECTION, "true");
                }
            } catch (Throwable ignore) {
            }

            firePendingAsyncRead(ctx);

            String protocol = ctx.channel().attr(NettyHttpConstants.PROTOCOL).get();
            //System.out.println(">>> Protocol was : " + protocol);
            if ("WebSocket".equalsIgnoreCase(protocol)){
                if (!ctx.channel().config().isAutoRead()){
                    Tr.debug(tc, "[UPGRADE-SYSOUT]: enable auto read for websoc");
                    ctx.channel().config().setAutoRead(true);
                }

            }
            else {
                if(ctx.channel().config().isAutoRead()){
                 Tr.debug(tc, "[UPGRADE-SYSOUT] onupgradeCommitted, ensuring autoread disabled");
                 ctx.channel().config().setAutoRead(false);
             }
            }

        } finally{
            try {
                promise = ctx.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get();
                if (promise == null){
                    ctx.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).set(CompletableFuture.completedFuture(null));
                } else if(!promise.isDone()){
                    promise.complete(null);
                }
            } catch (Throwable ignore) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "onUpgradeCommitted: callback not called due to promise exception.");
                }
            }
            upgradingNow = false;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ByteBuf raw;
        while ((raw = earlyUpgradeBytes.poll()) != null) {
            raw.release();
        }

        if (cause instanceof StreamException) {
            StreamException c = (StreamException) cause;
            HttpToHttp2ConnectionHandler handler = ctx.pipeline().get(HttpToHttp2ConnectionHandler.class);
            Http2Connection connection = handler.connection();

            if (cause.getMessage() != null && cause.getMessage().startsWith("Maximum active streams violated for this endpoint")) {
                int maxRefused = config.getH2MaxStreamsRefused();
                if (maxRefused == 0)
                    return;
                int streamsRefused = ctx.channel().attr(NettyHttpConstants.STREAMS_REFUSED).get();
                if (++streamsRefused >= maxRefused) {
                    handler.goAway(ctx, connection.remote().lastStreamCreated(), Http2Error.ENHANCE_YOUR_CALM.code(),
                                   Unpooled.wrappedBuffer("too many client-initiated streams have been refused; closing the connection".getBytes()),
                                   ctx.channel().newPromise());
                } else {
                    ctx.channel().attr(NettyHttpConstants.STREAMS_REFUSED).set(streamsRefused);
                    return;
                }
            } else {
                Http2Stream s = connection.stream(c.streamId());
                if (s != null)
                    s.close();
                return;
            }
        } else if (cause instanceof NettyHttpConstants.InvalidHttp2StreamMetadataException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failing closed on invalid trusted HTTP/2 stream metadata: " + cause);
            }
            clearHalfInitializedRequestState();
            HttpToHttp2ConnectionHandler handler = ctx.pipeline().get(HttpToHttp2ConnectionHandler.class);
            if (handler != null) {
                try {
                    handler.goAway(ctx, 0, Http2Error.PROTOCOL_ERROR.code(), Unpooled.EMPTY_BUFFER, ctx.newPromise());
                } catch (Throwable t) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "GOAWAY after invalid H2 metadata failed; closing channel", t);
                    }
                }
            }
            ctx.close();
            return;
        } else if (cause instanceof IllegalArgumentException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring exceptionCaught while decoding request of IllegalArgumentException: " + cause);
            }
            // We assume the IllegalArgumentException comes from the Netty codec. From here we assume that the codecs
            // will still send an http request but with a decoding exception. And we will use that to handle with an 
            // appropriate response. Return for now
            return;
        } else if (cause instanceof ParseException) {
            //Legacy doesnt throw ffdc on processNewInformation
            if (context.channel().attr(NettyHttpConstants.THROW_FFDC).get() != null) {
                context.channel().attr(NettyHttpConstants.THROW_FFDC).set(null);
            } else {
                FFDCFilter.processException(cause, HttpDispatcherHandler.class.getName() + ".exceptionCaught(ChannelHandlerContext, Throwable)", "1", context);
            }
            sendErrorMessage(cause);
            return;
        } else if (cause instanceof TimeoutException) {
            Tr.debug(tc, "Idle timeout; closing channel");
            if (cause instanceof ReadTimeoutException)
                sendErrorMessage(cause);
        } else if(cause instanceof TooLongFrameException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "exceptionCaught encountered an TooLongFrameException : " + cause);
            }
            sendErrorMessage(StatusCodes.ENTITY_TOO_LARGE, cause);
            return;
        }

        ctx.close();
    }

    private void clearHalfInitializedRequestState() {
        streamingInitialized = false;
        link = null;
        if (queue != null) {
            ByteBuf buf;
            while ((buf = queue.poll()) != null) {
                buf.release();
            }
            queue = null;
        }
        HttpContent early;
        while ((early = earlyContents.poll()) != null) {
            early.release();
        }
        ByteBuf raw;
        while ((raw = earlyUpgradeBytes.poll()) != null) {
            raw.release();
        }
    }

    private void sendErrorMessage(StatusCodes code, Throwable cause) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sending a " + code +  " for throwable [" + cause + "]");
        }
        loadErrorPage(code.getHttpError());
        HttpUtil.setKeepAlive(errorResponse, false);
        this.context.writeAndFlush(errorResponse);
    }

    private void sendErrorMessage(Throwable cause) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sending a 400 for throwable [" + cause + "]");
        }
        if (cause instanceof TooLongHttpLineException)
            loadErrorPage(StatusCodes.ENTITY_TOO_LARGE.getHttpError());
        else
            loadErrorPage(StatusCodes.BAD_REQUEST.getHttpError());
        HttpUtil.setKeepAlive(errorResponse, false);
        this.context.writeAndFlush(errorResponse);
    }

    private void loadErrorPage(HttpError error) {
        errorResponse.setStatus(HttpResponseStatus.valueOf(error.getErrorCode()));
        WsByteBuffer[] body = error.getErrorBody();
        if (body != null) {
            errorResponse.replace(Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(body)));
            if (HttpUtil.isTransferEncodingChunked(errorResponse))
                HttpUtil.setTransferEncodingChunked(errorResponse, false);
            long bytes = 0;
            for (WsByteBuffer b : body) {
                bytes += b.remaining();
            }
            HttpUtil.setContentLength(errorResponse, bytes);
            return;
        }
        if (HttpUtil.isTransferEncodingChunked(errorResponse))
            HttpUtil.setTransferEncodingChunked(errorResponse, false);
        HttpUtil.setContentLength(errorResponse, 0);

        HttpErrorPageService eps = (HttpErrorPageService) HttpDispatcher.getFramework().lookupService(HttpErrorPageService.class);
        if (eps == null)
            return;

        InetSocketAddress local = (InetSocketAddress) context.channel().localAddress();
        HttpErrorPageProvider provider = eps.access(local.getPort());
        if (provider != null) {
            String host = local.getAddress().getHostName();
            try {
                body = provider.accessPage(host, local.getPort(), null, null);
            } catch (Throwable t) {
                Tr.debug(tc, "Exception while calling into provider, t=" + t);
            }
            if (body != null) {
                errorResponse.replace(Unpooled.wrappedBuffer(WsByteBufferUtils.asByteArray(body)));
                if (HttpUtil.isTransferEncodingChunked(errorResponse))
                    HttpUtil.setTransferEncodingChunked(errorResponse, false);
                long bytes = 0;
                for (WsByteBuffer b : body) {
                    bytes += b.remaining();
                }
                HttpUtil.setContentLength(errorResponse, body.length);
            }
        }
    }

    public void newRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        normalizeProtocolMetadata(ctx, request);

        HttpDispatcherLink link = new HttpDispatcherLink();
        if (ctx.channel().hasAttr(NettyHttpConstants.CONTENT_LENGTH)) {
            ctx.channel().attr(NettyHttpConstants.CONTENT_LENGTH).set(null);
        }
        int num = ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).get();
        ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).set(num + 1);
        link.init(ctx, request, config);
        link.ready();
    }

    private static void clearPerRequestAttrs(ChannelHandlerContext ctx) {
        boolean asyncStreamRead =
            Boolean.TRUE.equals(ctx.channel().attr(NettyHttpConstants.ASYNC_STREAM_READ).get());
        if (!asyncStreamRead && AsyncReadDispatchState.forChannel(ctx.channel()).clearIfIdle()) {
            ctx.channel().attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).set(Boolean.FALSE);
            ctx.channel().attr(NettyHttpConstants.ASYNC_STREAM_READ).set(Boolean.FALSE);
        }
        ctx.channel().attr(NettyHttpConstants.HTTP_INPUT_STREAM).set(null);
        if (ctx.channel().hasAttr(NettyHttpConstants.CONTENT_LENGTH)) {
            ctx.channel().attr(NettyHttpConstants.CONTENT_LENGTH).set(null);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {

        upgradingNow = false;

        //If there was an upgrade promise, we need to fail it.
        CompletableFuture<Void> promise = context.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).getAndSet(null);
        if (promise != null && !promise.isDone()) {
            promise.completeExceptionally(new IllegalStateException("Upgrade promise failed due to channel being closed."));
        }
        
        postFlipDrainerInstalled.set(false);
        // Rease any buffered upgraded bytes on close
        ByteBuf upgradeBytes;
        while((upgradeBytes = earlyUpgradeBytes.poll()) != null) {
            upgradeBytes.release();
        }
        // Release any buffered content
        HttpContent content;
        while((content = earlyContents.poll()) != null) {
            content.release();
        }

        AsyncReadDispatchState asyncReadState = AsyncReadDispatchState.forChannel(context.channel());
        boolean asyncReadInProgress =
            Boolean.TRUE.equals(context.channel().attr(NettyHttpConstants.ASYNC_STREAM_READ).get()) ||
            asyncReadState.hasOutstandingCallback();
        Throwable lifecycleFailure = null;
        try {
            asyncReadState.fail();
        } catch (Throwable t) {
            lifecycleFailure = t;
        }

        try {
            boolean responseCloseBeforeRequestBodyComplete =
                Boolean.TRUE.equals(context.channel().attr(NettyHttpConstants.RESPONSE_CLOSE_BEFORE_REQUEST_BODY_COMPLETE).get());
            if (queue != null && !queue.isEos()) {
                if (responseCloseBeforeRequestBodyComplete && !asyncReadInProgress) {
                    queue.signalEos();
                    if (link != null)
                        link.setBodyComplete();
                } else {
                    context.channel().attr(NettyHttpConstants.INPUT_SHUTDOWN_PENDING).set(Boolean.TRUE);
                    queue.signalError(new EOFException("Channel closed before request body completed."));
                }
            }
        } catch (Throwable t) {
            lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
        }
        // Preserve RESPONSE_CLOSE_BEFORE_REQUEST_BODY_COMPLETE across physical close so
        // incomplete-body close remains observable on the deactivated channel. It is reset
        // only at the next request boundary in beginStreamingRequest.
        try {
            clearPerRequestAttrs(context);
        } catch (Throwable t) {
            lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
        }
        try {
            super.channelInactive(context);
        } catch (Throwable t) {
            lifecycleFailure = mergeLifecycleFailure(lifecycleFailure, t);
        }
        rethrowLifecycleFailure(lifecycleFailure);
    }

    private static Throwable mergeLifecycleFailure(Throwable primary, Throwable next) {
        if (primary == null)
            return next;
        if (primary != next)
            primary.addSuppressed(next);
        return primary;
    }

    private static void rethrowLifecycleFailure(Throwable failure) throws Exception {
        if (failure == null)
            return;
        if (failure instanceof Exception)
            throw (Exception) failure;
        if (failure instanceof Error)
            throw (Error) failure;
        throw new RuntimeException(failure);
    }

    //TODO -> Pipeline utils candidate
    private static void removeIfPresent(ChannelPipeline pipeline, Class<? extends ChannelHandler> handlerType) {
        try {
            ChannelHandler h = pipeline.get(handlerType);
            if (h != null)
                pipeline.remove(handlerType);
        } catch (Throwable ignore) {
        }
    }

    private static void removeIfPresent(ChannelPipeline pipeline, String name) {
        try {
            if (pipeline.context(name) != null)
                pipeline.remove(name);
        } catch (Throwable ignore) {
        }
    }

    public static final class Upgrade101CommittedEvent {
        @Override
        public String toString() {
            return "UPGRADE_101_COMMITTED";
        }
    }

    public static final Upgrade101CommittedEvent UPGRADE_101_COMMITTED_EVENT = new Upgrade101CommittedEvent();

    private void flushParkedToUpgrade(ChannelHandlerContext upgCtx) throws Exception {
        if (upgCtx == null)
            return;
        final NettyServletUpgradeHandler upgrade = (NettyServletUpgradeHandler) upgCtx.handler();

        int ec = 0, eu = 0;

        HttpContent early;
        while ((early = this.earlyContents.poll()) != null) {
            try {
                ByteBuf d = early.content();
                if (d.isReadable()) {
                    upgrade.channelRead(upgCtx, d.retain());
                    ec++;
                }
            } finally {
                early.release();
            }
        }
        ByteBuf raw;
        while ((raw = this.earlyUpgradeBytes.poll()) != null) {
            try {
                if (raw.isReadable()) {
                    upgrade.channelRead(upgCtx, raw.retain());
                    eu++;
                }
            } finally {
                raw.release();
            }
        }
        try {
            upgrade.channelReadComplete(upgCtx);
        } catch (Exception ignore) {
        }
    }

    private void deliverToUpgradeOrPark(ChannelHandlerContext ctx, ByteBuf buf) {

        ChannelHandlerContext upgCtx = ctx.pipeline().context(NettyServletUpgradeHandler.class);
        if (upgCtx != null) {
            final NettyServletUpgradeHandler upgrade = (NettyServletUpgradeHandler) upgCtx.handler();
            try {
                upgrade.channelRead(upgCtx, buf.retain());
            } catch (Exception ignore) {
            }
            try {
                upgrade.channelReadComplete(upgCtx);
            } catch (Exception ignore) {
            }
            return;
        }

        earlyUpgradeBytes.add(buf.retainedSlice(buf.readerIndex(), buf.readableBytes()));

        if (postFlipDrainerInstalled.compareAndSet(false, true)) {
            CompletableFuture<Void> pr = ctx.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get();
            if (pr == null) {
                pr = new CompletableFuture<>();
                ctx.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).set(pr);
            }
            final CompletableFuture<Void> waiter = pr;
            waiter.whenComplete((v, t) -> {
                ctx.executor().execute(() -> {
                    try {
                        if(!ctx.channel().isActive()){
                            return; //Channel inactive will release the buffers
                        }
                        ChannelHandlerContext u = ctx.pipeline().context(NettyServletUpgradeHandler.class);
                        if (u != null)
                            flushParkedToUpgrade(u);
                    } catch (Exception ignore) {
                        //System.out.println("Exception in flushedParkToUpgrade: " + e);
                    } finally {
                        postFlipDrainerInstalled.set(false);
                    }
                });
            });
        }
    }

    public void putStream(String streamId, HttpInputStream stream){
        streamMap.put(streamId, stream);
    }

    public HttpInputStream getStream(String streamId){
        return streamMap.get(streamId);
    }

    public HttpInputStream removeStream(String streamId){
        return streamMap.remove(streamId);
    }

    private void setUpgradeReadyPromise(ChannelHandlerContext context){
        Tr.debug(tc,"UPGRADE LOG -> setUpgradeReadyPromise");
        CompletableFuture<Void> promise = context.attr(NettyHttpConstants.UPGRADE_READY_PROMISE).get();
        if(promise == null){
            context.attr(NettyHttpConstants.UPGRADE_READY_PROMISE).set(new CompletableFuture<>());
        }
    }


    private void firePendingAsyncRead(ChannelHandlerContext ctx) {
        AsyncReadDispatchState.forChannel(ctx.channel()).signal();
    }

    private void firePendingAsyncReadError(ChannelHandlerContext ctx) {
        AsyncReadDispatchState.forChannel(ctx.channel()).fail();
    } 
}