/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.http.dispatcher.internal.channel.HttpRequestImpl;
import com.ibm.ws.http.dispatcher.internal.channel.HttpResponseImpl;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.message.BodyQueue;
import com.ibm.ws.http.netty.message.NettyRequestMessage;
import com.ibm.ws.http.netty.pipeline.CRLFValidationHandler;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.http.channel.error.HttpError;
import com.ibm.wsspi.http.channel.error.HttpErrorPageProvider;
import com.ibm.wsspi.http.channel.error.HttpErrorPageService;
import com.ibm.wsspi.http.channel.values.StatusCodes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
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
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception.StreamException;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.ReferenceCountUtil;
import io.openliberty.http.netty.timeout.TimeoutHandler;
import io.openliberty.http.netty.timeout.exception.TimeoutException;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;



import com.ibm.ws.http.channel.outstream.HttpOutputStreamObserver;

/**
 * Dispatcher: wires upgrade and hands off body streaming to BodyQueue (HTTP) or UpgradeHandler (post-101).
 */
public class HttpDispatcherHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final TraceComponent tc = Tr.register(HttpDispatcherHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final HttpChannelConfig config;
    private ChannelHandlerContext context;

    private final DefaultFullHttpResponse errorResponse;

    private BodyQueue queue;
    private HttpDispatcherLink link;

    private final java.util.ArrayDeque<HttpContent> earlyContents = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<io.netty.buffer.ByteBuf> earlyUpgradeBytes = new java.util.ArrayDeque<>();

    private final java.util.concurrent.atomic.AtomicBoolean commitScheduled = new java.util.concurrent.atomic.AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicBoolean upgradeCommitted = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile boolean upgradingNow;
    private volatile boolean streamingInitialized;


    private static final String UPGRADE_TRACE_ID = "UGTRACE";
    private static final io.netty.util.AttributeKey<Long> ATTR_SEQ = io.netty.util.AttributeKey.valueOf("ug.seq");
    private static final io.netty.util.AttributeKey<Long> ATTR_TID = io.netty.util.AttributeKey.valueOf("ug.tid");

    private enum CommitTrigger {
        FLUSH_OBSERVER, EARLY_BYTES, RETRY_TASK
    }

    private final java.util.concurrent.atomic.AtomicReference<CommitTrigger> commitTrigger = new java.util.concurrent.atomic.AtomicReference<>(null);

    public HttpDispatcherHandler(HttpChannelConfig config) {
        super(false);
        this.config = Objects.requireNonNull(config);
        

        this.errorResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
    }

    private long nextSeq(ChannelHandlerContext ctx) {
        Long s = ctx.channel().attr(ATTR_SEQ).get();
        long n = (s == null ? 0L : s) + 1L;
        ctx.channel().attr(ATTR_SEQ).set(n);
        return n;
    }

    private long ensureTraceId(ChannelHandlerContext ctx) {
        Long tid = ctx.channel().attr(ATTR_TID).get();
        if (tid != null)
            return tid;
        long newTid = System.nanoTime(); // unique enough
        ctx.channel().attr(ATTR_TID).set(newTid);
        return newTid;
    }

    private String chid(ChannelHandlerContext ctx) {
        return ctx.channel().id().asShortText();
    }

    

    // Compact pipeline snapshot: handler simple names in order
    private String pipelineNames(ChannelPipeline p) {
        StringBuilder sb = new StringBuilder("[");
        for (String n : p.names()) {
            sb.append(n).append(',');
        }
        if (sb.length() > 1)
            sb.setLength(sb.length() - 1);
        return sb.append(']').toString();
    }

    private void t(ChannelHandlerContext ctx, String at, String fmt, Object... args) {
        long tid = ensureTraceId(ctx);
        long seq = nextSeq(ctx);
        String msg = String.format(fmt, args);
        Tr.debug(tc, String.format("UG|tid=%d|seq=%d|at=%s|ch=%s|thr=%s|%s",
                                   tid, seq, at, chid(ctx), thr(), msg));
    }

    private static String bufInfo(ByteBuf b) {
        return String.format("buf@%x rc=%d ridx=%d widx=%d cap=%d",
                             System.identityHashCode(b), b.refCnt(), b.readerIndex(), b.writerIndex(), b.capacity());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().config().isAutoRead()) {
            Tr.debug(tc, "HTTP: channelActive -> initial read()");
            ctx.channel().read();
        }
        snapCtx("channelActive", ctx);
        super.channelActive(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        context = ctx;
        ctx.channel().attr(NettyHttpConstants.NUMBER_OF_HTTP_REQUESTS).set(0);
        ctx.channel().attr(NettyHttpConstants.STREAMS_REFUSED).set(0);
        //Should we have this in channel start instead of passing it as handler param?
        ctx.channel().attr(NettyHttpConstants.HTTP_CONFIG).set(config);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handlerAdded cid=" + cid(ctx.channel()) + " autoRead=" + ctx.channel().config().isAutoRead());
            dumpPipeline(ctx.pipeline(), "onAdd");
        }
        
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof Upgrade101CommittedEvent) {
            // If the flip already happened, satisfy any late waiter immediately.
            // if (upgradeCommitted.get()) {
            //     io.netty.channel.ChannelPromise waiter = ctx.channel().attr(NettyHttpConstants.UPGRADE_FLIP_PROMISE).getAndSet(null);
            //     if (waiter != null && !waiter.isDone()) {
            //         t(ctx, "userEvt", "flip already committed; completing waiter");
            //         waiter.setSuccess();
            //     }
            //     return;
            // }
            t(ctx, "userEvt", "UPGRADE_101_COMMITTED (triggerWas=%s scheduled=%s committed=%s)",
              commitTrigger.get(), commitScheduled.get(), upgradeCommitted.get());
            commitTrigger.compareAndSet(null, CommitTrigger.FLUSH_OBSERVER);
            if (commitScheduled.compareAndSet(false, true)) {
                onUpgradeCommitted(ctx);
            } else {
                t(ctx, "userEvt", "commit already scheduled");
            }
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    // Direct invocation path for tests
    public void processMessageDirectly(FullHttpRequest request) throws Exception {
        channelRead0(context, request);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (upgradingNow && msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                dumpBuf("EARLY-101 incoming", buf);
            }
            ByteBuf snapshot = buf.retainedSlice(buf.readerIndex(), buf.readableBytes());
            t(ctx, "earlyBytes",
              "captured %s (upgradingNow=%s upgradeCommitted=%s commitScheduled=%s)",
              bufInfo(snapshot), upgradingNow, upgradeCommitted.get(), commitScheduled.get());
            earlyUpgradeBytes.add(snapshot);
            ReferenceCountUtil.release(buf);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "EARLY-101 stored count=" + earlyUpgradeBytes.size() + " " + flags());
            }

            if (commitTrigger.compareAndSet(null, CommitTrigger.EARLY_BYTES)) {
                t(ctx, "earlyBytes", "setting trigger=EARLY_BYTES");
            }

            if (commitScheduled.compareAndSet(false, true)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Scheduling onUpgradeCommitted (from channelRead) inEL=" + ctx.executor().inEventLoop());
                }
                ctx.executor().execute(() -> {
                    t(ctx, "earlyBytes.schedule", "inEventLoop=%s", ctx.executor().inEventLoop());
                    if (upgradingNow && !upgradeCommitted.get()) {
                        onUpgradeCommitted(ctx);
                    }
                });
            } else {
                t(ctx, "earlyBytes", "commit already scheduled");
            }
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            currentReqId = REQ_SEQ.incrementAndGet();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                HttpRequest req = (HttpRequest) msg;
                Tr.debug(tc, "REQ BEGIN reqId=" + currentReqId +
                             " uri=" + req.uri() +
                             " ver=" + req.protocolVersion() +
                             " isUpgrade=" + isUpgrade(req));
                snapCtx("req-begin", ctx);
            }
            HttpRequest req = (HttpRequest) msg;
            upgradingNow = false;
            streamingInitialized = false;
            if (queue != null)
                Tr.debug(tc, "HTTP: new request; resetting BodyQueue old=" + System.identityHashCode(queue));

            queue = new BodyQueue(ctx.alloc());
            earlyContents.clear();
            earlyUpgradeBytes.clear();
            link = null;

            if (isUpgrade(req) && (req instanceof FullHttpRequest)) {
                ByteBuf content = ((FullHttpRequest) req).content();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    dumpBuf("EARLY-CONTENT(upgNow) park", content);
                }
                if (content.isReadable()) {
                    content.retain(); 
                    earlyUpgradeBytes.add(content); 
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "HTTP: parked aggregated upgrade body bytes=" + content.readableBytes());
                    }
                }
            }

            beginStreamingRequest(ctx, req);

            if (!upgradingNow) {
                // Drain any early content (arrived in same read cycle)
                drainEarlyHttpContentToBodyQueue(ctx);
            }

            if (!ctx.channel().config().isAutoRead() && queue.wantsInput()) {
                ctx.executor().execute(() -> ctx.channel().read());
            }
            return;
        }

        if (msg instanceof HttpContent) {
            HttpContent content = (HttpContent) msg;

            if (upgradingNow) {
                content.retain();
                earlyContents.add(content);
                t(ctx, "park.content", "last=%s bytes=%d %s",
                  (content instanceof LastHttpContent), content.content().readableBytes(), bufInfo(content.content()));

                
                ReferenceCountUtil.release(content);
                return;
            }

            if (!streamingInitialized && queue == null) {
                content.retain();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    dumpBuf("EARLY-CONTENT(pre-stream) park", content.content());
                }
                earlyContents.add(content);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "HTTP: parked early HttpContent bytes=" + content.content().readableBytes()
                                 + " last=" + (content instanceof LastHttpContent));
                }
                ReferenceCountUtil.release(msg);
                return;
            }

            try {
                ByteBuf data = content.content();
                if (data.isReadable())
                    queue.enqueueRetained(data);
                if (content instanceof LastHttpContent) {
                    HttpHeaders trailers = ((LastHttpContent) content).trailingHeaders();
                    if(!trailers.isEmpty()){
                        System.out.println("trailers >>> found >>> " + trailers);
                        HttpRequestImpl req = (HttpRequestImpl) link.getRequest();
                        req.setTrailers(trailers);
                    }
                    queue.signalEos();
                    if (this.link != null)
                        this.link.setBodyComplete();
                }
                if (!ctx.channel().config().isAutoRead() && queue.wantsInput()) {
                    ctx.executor().execute(() -> ctx.channel().read());
                }
            } finally {
                ReferenceCountUtil.release(content);
            }
            return;
        }    
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().config().isAutoRead() && !upgradingNow && queue != null && queue.wantsInput()) {
            ctx.executor().execute(() -> ctx.channel().read());
        }
        super.channelReadComplete(ctx);
    }

    private static boolean isUpgrade(HttpRequest req) {
        final CharSequence conn = req.headers().get(HttpHeaderNames.CONNECTION);
        final CharSequence upg = req.headers().get(HttpHeaderNames.UPGRADE);
        if (upg == null || conn == null)
            return false;
        return io.netty.util.AsciiString.containsIgnoreCase(conn, "upgrade");
    }

    private void beginStreamingRequest(ChannelHandlerContext ctx, HttpRequest request) {

        final CharSequence ae = request.headers().get(HttpHeaderNames.ACCEPT_ENCODING);
        if (ae != null) {
            ctx.channel().attr(NettyHttpConstants.ACCEPT_ENCODING).set(ae.toString());
        }

        // protocol tag on channel
        if (request.headers().contains(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text())) {
            ctx.channel().attr(NettyHttpConstants.PROTOCOL).set("HTTP2");
        } else {
            ctx.channel().attr(NettyHttpConstants.PROTOCOL).set(request.protocolVersion().equals(HttpVersion.HTTP_1_0) ? "HTTP10" : "http");
        }
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

        commitScheduled.set(false);
        upgradeCommitted.set(false);

        commitTrigger.set(null);
        t(ctx, "begin",
          "autoRead=%s upg=%s cl=%d chunked=%s expect100=%s pipeline=%s",
          ctx.channel().config().isAutoRead(), upg, cl, chunked, expect100, pipelineNames(ctx.pipeline()));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, String.format(
                                       "beginStreamingRequest reqId=%d CL=%d chunked=%s expect100=%s upgrade=%s",
                                       currentReqId, cl, chunked, expect100, upg));
            snapCtx("beginStreamingRequest", ctx);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "beginStreamingRequest: CL=" + cl + " chunked=" + chunked + " expect100=" + expect100 + " upg=" + upg);
        }

        // Initialize link & request/response
        link.initStreaming(ctx, request, config);
        final HttpRequestImpl req = (HttpRequestImpl) link.getRequest();
        final HttpInputStreamImpl body = req.getBody();

        if (upg) {
            // Defer pipeline switch until we commit 101+Upgrade
            
            upgradingNow = true;
            HttpDispatcher.getExecutorService().execute(() -> link.ready());
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "non-upgrade: wiring body queue=" + System.identityHashCode(queue));
        }

        // Non-upgrade: expose input stream and wire body queue
        ctx.channel().attr(NettyHttpConstants.HTTP_INPUT_STREAM).set(body);
        final String contentEncoding = request.headers().get(HttpHeaderNames.CONTENT_ENCODING);
        body.nettyConfigureStreaming(queue, ctx, contentEncoding, cl, chunked);

        if (!hasBody && !expect100) {
            queue.signalEos();
            if (this.link != null)
                this.link.setBodyComplete();
        }

        streamingInitialized = true;

        if (!ctx.channel().config().isAutoRead() && queue.wantsInput())
            ctx.channel().read();

        
        HttpDispatcher.getExecutorService().execute(() -> link.ready());
    }

    private void drainEarlyHttpContentToBodyQueue(ChannelHandlerContext ctx) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "drainEarlyHttpContentToBodyQueue count=" + earlyContents.size());
        }
        t(ctx, "drainToBodyQueue.begin", "count=%d", earlyContents.size());
        HttpContent early;
        while ((early = earlyContents.poll()) != null) {
            try {

                final ByteBuf data = early.content();

                t(ctx, "drainToBodyQueue.item", "last=%s bytes=%d %s",
                  (early instanceof LastHttpContent), data.readableBytes(), bufInfo(data));

                if (data.isReadable())
                    queue.enqueueRetained(data);
                if (early instanceof LastHttpContent) {
                    queue.signalEos();
                    if (this.link != null)
                        this.link.setBodyComplete();
                }
            } finally {
                early.release();
                t(ctx, "drainToBodyQueue.end", "done");
            }
        }
    }

    private void onUpgradeCommitted(ChannelHandlerContext ctx) {
        if (!ctx.executor().inEventLoop()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "onUpgradeCommitted off-EL, rescheduling...");
            }
            t(ctx, "commit.defer", "not in EL, deferring");
            ctx.executor().execute(() -> onUpgradeCommitted(ctx));
            return;
        }
        if (!upgradeCommitted.compareAndSet(false, true)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "onUpgradeCommitted: already committed; skipping");
            }
            t(ctx, "commit.skip", "already committed (trigger=%s)", commitTrigger.get());
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            snapCtx("onUpgradeCommitted:enter", ctx);
            dumpPipeline(ctx.pipeline(), "preFlip");
        }

        final ChannelPipeline p = ctx.pipeline();
        t(ctx, "commit.begin",
          "trigger=%s autoRead=%s pipeline(before)=%s earlyContents=%d earlyUpgBytes=%d",
          commitTrigger.get(), ctx.channel().config().isAutoRead(),
          pipelineNames(p), earlyContents.size(), earlyUpgradeBytes.size());



        removeIfPresent(p, HttpServerCodec.class);
        removeIfPresent(p, HttpObjectAggregator.class);
        removeIfPresent(p, CRLFValidationHandler.class);
        removeIfPresent(p, TimeoutHandler.class);
        removeIfPresent(p, LibertyHttpRequestHandler.class);
        removeIfPresent(p, io.netty.handler.codec.http.HttpServerKeepAliveHandler.class);


        NettyServletUpgradeHandler upgrade = p.get(NettyServletUpgradeHandler.class);
        if (upgrade == null) {
            if (p.get("HTTP_DISPATCHER") != null) {
                p.addBefore("HTTP_DISPATCHER", NettyServletUpgradeHandler.NAME, new NettyServletUpgradeHandler(ctx.channel()));
                t(ctx, "commit.add", "ServletUpgradeHandler before HTTP_DISPATCHER");
            } else {
                p.addLast(NettyServletUpgradeHandler.NAME, new NettyServletUpgradeHandler(ctx.channel()));
                t(ctx, "commit.add", "ServletUpgradeHandler addLast");
            }
            upgrade = p.get(NettyServletUpgradeHandler.class);
        } else {
            t(ctx, "commit.add", "ServletUpgradeHandler already present");
        }

        // Deliver parked content to the *upgrade handler context*
        final ChannelHandlerContext upgCtx = p.context(NettyServletUpgradeHandler.class);
        if (upgCtx == null) {
            t(ctx, "commit.err", "ServletUpgradeHandler context is null");
        }

        HttpContent early;
        int ec = 0;
        while ((early = this.earlyContents.poll()) != null) {
            try {
                ByteBuf d = early.content();
                if (d.isReadable() && upgCtx != null) {
                    t(ctx, "commit.flushEarlyContent", "bytes=%d %s", d.readableBytes(), bufInfo(d));
                    upgCtx.fireChannelRead(d.retain());
                    ec++;
                }
            } finally {
                early.release();
            }
        }
        ByteBuf raw;
        int eu = 0;
        while ((raw = earlyUpgradeBytes.poll()) != null) {
            try {
                if (raw.isReadable() && upgCtx != null) {
                    t(ctx, "commit.flushEarlyBytes", "bytes=%d %s", raw.readableBytes(), bufInfo(raw));
                    upgCtx.fireChannelRead(raw.retain());
                    eu++;
                }
            } finally {
                raw.release();
            }
        }
        if (upgCtx != null) {
            t(ctx, "commit.readComplete", "flushed earlyContents=%d earlyUpgBytes=%d", ec, eu);
            upgCtx.fireChannelReadComplete();
        }

        // Mark and cleanup
        ReadFlowHandler.setClosedOrUpgraded(ctx);
        ctx.channel().attr(NettyHttpConstants.UPGRADED).set(Boolean.TRUE);
        ctx.channel().attr(NettyHttpConstants.HTTP_INPUT_STREAM).set(null);

        try {
            if (this.link != null && this.link.getVirtualConnection() != null) {
                this.link.getVirtualConnection().getStateMap().put(com.ibm.ws.transport.access.TransportConstants.UPGRADED_CONNECTION, "true");
            }
        } catch (Throwable ignore) {
            // keep pipeline alive regardless
        }

        Runnable pending = ctx.channel().attr(NettyHttpConstants.ASYNC_READ_CALLBACK).getAndSet(null);
        if (pending != null) {
            t(ctx, "commit.pending", "running ASYNC_READ_CALLBACK once");
            HttpDispatcher.getExecutorService().execute(pending);
        }

        if (!ctx.channel().config().isAutoRead()) {
            t(ctx, "commit.nudgeRead", "autoRead=false, issuing read()");
            ctx.channel().read();
        }

        try {
            CompletableFuture<Void> promise = ctx.channel().attr(NettyHttpConstants.UPGRADE_READY_PROMISE).getAndSet(null);
            System.out.println("UG-PROMISE fetched ch=" + ctx.channel().id().asShortText() +
                               " p@" + (promise == null ? 0 : System.identityHashCode(promise)));
            if (promise != null && !promise.isDone()) {
                promise.complete(null);
                t(ctx, "commit.done", "signaled UPGRADE_READY_PROMISE");
            } else {
                t(ctx, "commit.done", "no waiter for UPGRADE_READY_PROMISE");
            }
        } catch (Throwable ignore) {
            // keep the pipeline alive regardless
        }

        upgradingNow = false;
        t(ctx, "commit.done", "pipeline(after)=%s", pipelineNames(p));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "exceptionCaught cid=" + cid(ctx.channel()) + " inEL=" + ctx.executor().inEventLoop() +
                         " type=" + cause.getClass().getName() + " msg=" + String.valueOf(cause.getMessage()));
            snapCtx("exceptionCaught", ctx);
        }
        int n = 0;
        ByteBuf raw;
        while ((raw = earlyUpgradeBytes.poll()) != null) {
            n++;
            raw.release();
        }
        t(ctx, "exception", "released earlyUpgradeBytes=%d cause=%s", n, cause.toString());
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
                Http2Stream stream = connection.stream(c.streamId());
                if (stream != null)
                    stream.close();
                return;
            }
        } else if (cause instanceof IllegalArgumentException) {
            if (ctx.channel().attr(NettyHttpConstants.THROW_FFDC).get() != null) {
                ctx.channel().attr(NettyHttpConstants.THROW_FFDC).set(null);
            } else if (cause.getMessage() == null || !cause.getMessage().contains("possibly HTTP/0.9")) {
                FFDCFilter.processException(cause, HttpDispatcherHandler.class.getName() + ".exceptionCaught(ChannelHandlerContext, Throwable)", "1", ctx);
            }
            Tr.debug(tc, "IllegalArgumentException: " + cause);
            sendErrorMessage(cause);
            return;
        } else if (cause instanceof TooLongHttpHeaderException) {
            Tr.debug(tc, "TooLongHttpHeaderException: " + cause);
            sendErrorMessage(cause);
            return;
        } else if (cause instanceof TimeoutException) {
            Tr.debug(tc, "Idle timeout; closing channel");
            if (cause instanceof ReadTimeoutException)
                sendErrorMessage(cause);
        }

        clearPerRequestAttrs(ctx);
        ctx.close();
    }

    private void sendErrorMessage(Throwable cause) {
        Tr.debug(tc, "Sending a 400 for throwable [" + cause + "]");
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
            //HttpUtil.setContentLength(errorResponse, body.length);
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
        if (request.headers().contains(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text())) {
            ctx.channel().attr(NettyHttpConstants.PROTOCOL).set("HTTP2");
        } else {
            ctx.channel().attr(NettyHttpConstants.PROTOCOL).set(request.protocolVersion().equals(HttpVersion.HTTP_1_0) ? "HTTP10" : "http");
        }

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
        ctx.channel().attr(NettyHttpConstants.HTTP_INPUT_STREAM).set(null);
        ctx.channel().attr(NettyHttpConstants.ASYNC_READ_CALLBACK).set(null);
        if (ctx.channel().hasAttr(NettyHttpConstants.CONTENT_LENGTH)) {
            ctx.channel().attr(NettyHttpConstants.CONTENT_LENGTH).set(null);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelInactive cid=" + cid(ctx.channel()));
            snapCtx("channelInactive", ctx);
        }
        int n = 0;
        ByteBuf raw;
        while ((raw = earlyUpgradeBytes.poll()) != null) {
            n++;
            raw.release();
        }
        t(ctx, "inactive", "released earlyUpgradeBytes=%d", n);
        clearPerRequestAttrs(ctx);
        super.channelInactive(ctx);
    }

    /////DEBUG
    /// // Correlate events across a channel's lifetime and requests
    private static final AtomicLong REQ_SEQ = new AtomicLong(0);

    private long currentReqId = -1L;

    private static String cid(Channel ch) {
        return ch.id().asShortText();
    }

    private static String thr() {
        return Thread.currentThread().getName();
    }

    private String flags() {
        return "upgradingNow=" + upgradingNow +
               " upgradeCommitted=" + upgradeCommitted.get() +
               " commitScheduled=" + commitScheduled.get() +
               " earlyContents=" + earlyContents.size() +
               " earlyUpgradeBytes=" + earlyUpgradeBytes.size() +
               " queue=" + (queue == null ? "null" : System.identityHashCode(queue)) +
               " link=" + (link == null ? "null" : System.identityHashCode(link));
    }

    private void snapCtx(String where, ChannelHandlerContext ctx) {
        if (!TraceComponent.isAnyTracingEnabled() || !tc.isDebugEnabled())
            return;
        Channel ch = ctx.channel();
        Tr.debug(tc, String.format(
                                   "SNAP[%s] cid=%s reqId=%d inEL=%s autoRead=%s active=%s open=%s writable=%s flags={%s}",
                                   where, cid(ch), currentReqId, ctx.executor().inEventLoop(), ch.config().isAutoRead(),
                                   ch.isActive(), ch.isOpen(), ch.isWritable(), flags()));
    }

    private void dumpPipeline(ChannelPipeline p, String tag) {
        if (!TraceComponent.isAnyTracingEnabled() || !tc.isDebugEnabled())
            return;
        StringBuilder sb = new StringBuilder("PIPE[").append(tag).append("]: ");
        p.toMap().forEach((name, handler) -> sb.append(name).append(","));
        Tr.debug(tc, sb.toString());
    }

    private void dumpBuf(String tag, ByteBuf b) {
        if (!TraceComponent.isAnyTracingEnabled() || !tc.isDebugEnabled())
            return;
        int readable = b.readableBytes();
        String hex = ByteBufUtil.hexDump(b, b.readerIndex(), Math.min(readable, 64));
        Tr.debug(tc, String.format("%s ridx=%d widx=%d readable=%d refCnt=%d preview=%s",
                                   tag, b.readerIndex(), b.writerIndex(), readable, b.refCnt(), hex));
    }

    private static void removeIfPresent(ChannelPipeline pipeline, Class<? extends ChannelHandler> handler) {
        if (pipeline.get(handler) != null) {
            pipeline.remove(handler);
        }
    }

    public static final class Upgrade101CommittedEvent {
        @Override
        public String toString() {
            return "UPGRADE_101_COMMITTED";
        }
    }

    public static final Upgrade101CommittedEvent UPGRADE_101_COMMITTED_EVENT = new Upgrade101CommittedEvent();
}