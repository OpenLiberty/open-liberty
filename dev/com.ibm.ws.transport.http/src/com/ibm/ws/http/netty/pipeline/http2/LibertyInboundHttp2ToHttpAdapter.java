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

import com.ibm.ws.http.channel.internal.h2spool.H2BodySpool;
import com.ibm.ws.http.channel.internal.h2spool.H2CompletedSpoolRequest;
import com.ibm.ws.http.channel.internal.h2spool.H2SealedBody;
import com.ibm.ws.http.channel.internal.h2spool.H2SpoolResources;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.message.BodyQueue;
import com.ibm.ws.http2.GrpcServletServices;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2LocalFlowController;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;

/**
 * An extension of {@link InboundHttp2ToHttpAdapter} for Liberty specific functionality.
 * Specifically the {@link processHeadersBegin()} for necessary header verification to
 * launch stream errors handled further in the pipeline to match the same behavior as
 * before.
 */
public class LibertyInboundHttp2ToHttpAdapter extends InboundHttp2ToHttpAdapter implements ResetFrameTracker {
    private static final TraceComponent tc = Tr.register(LibertyInboundHttp2ToHttpAdapter.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);
    // Only a small ordinary body may use Netty's FullHttpMessage when the spool owner is unavailable.
    private static final int DEGRADED_ORDINARY_MAX_BYTES = 64 * 1024;

    private final Channel channel;
    private final HttpChannelConfig httpConfig;
    private final int maxResetFrames;
    private final int resetFrameWindow;
    private final long maxRequestContentLength;
    private final Http2Connection.PropertyKey streamingKey;
    private final Http2Connection.PropertyKey ordinaryKey;
    private final Http2Connection.PropertyKey closedSpoolBypassKey;
    private final H2SpoolResources.ConnectionScope ordinaryResources;
    private final int maxDataFrameBytes;
    private final int ordinaryPendingBytes;
    
    private volatile long startResetTime = System.nanoTime();
    private volatile int resetFrameCount = 0; //tracks both inbound and outbound resets

    private AtomicBoolean goAwayReceived = new AtomicBoolean(false);

    static final Http2Exception RST_FRAME_RATE_EXCEEDED = new Http2Exception(Http2Error.ENHANCE_YOUR_CALM,
            "too many reset frames processed");


    LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, int maxContentLength, boolean validateHttpHeaders, boolean propagateSettings, Channel channel, HttpChannelConfig httpConfig) {
        this(connection, (long) maxContentLength, validateHttpHeaders, propagateSettings, channel, httpConfig, null);
    }

    LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, int maxContentLength, boolean validateHttpHeaders,
                                               boolean propagateSettings, Channel channel, HttpChannelConfig httpConfig,
                                               H2SpoolResources.ConnectionScope ordinaryResources) {
        this(connection, (long) maxContentLength, validateHttpHeaders, propagateSettings, channel, httpConfig, ordinaryResources);
    }

    protected LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, long maxContentLength, boolean validateHttpHeaders,
                                               boolean propagateSettings, Channel channel, HttpChannelConfig httpConfig) {
        this(connection, maxContentLength, validateHttpHeaders, propagateSettings, channel, httpConfig, null);
    }

    LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, long maxContentLength, boolean validateHttpHeaders,
                                               boolean propagateSettings, Channel channel, HttpChannelConfig httpConfig,
                                               H2SpoolResources.ConnectionScope ordinaryResources) {
        super(connection, aggregateLimit(maxContentLength), validateHttpHeaders, propagateSettings);
        this.channel = channel;
        this.httpConfig = httpConfig;
        this.maxResetFrames = httpConfig.getH2MaxResetFrames();
        this.resetFrameWindow = httpConfig.getH2ResetFramesWindow();
        // The inherited FullHttpMessage path needs an int; stream accounting keeps the supplied long.
        this.maxRequestContentLength = maxContentLength;
        this.streamingKey = connection.newKey();
        this.ordinaryKey = connection.newKey();
        this.closedSpoolBypassKey = connection.newKey();
        this.ordinaryResources = ordinaryResources;
        this.maxDataFrameBytes = httpConfig.getH2MaxFrameSize();
        this.ordinaryPendingBytes = Math.max(256 * 1024, maxDataFrameBytes);
    }

    private static int aggregateLimit(long limit) {
        return limit < 0 || limit > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) limit;
    }

    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
        Http2Stream stream = connection.stream(streamId);
        OrdinaryState ordinary = stream == null ? null : stream.getProperty(ordinaryKey);
        if (ordinary != null) {
            return onOrdinaryData(ctx, stream, ordinary, data, padding, endOfStream);
        }
        StreamingState state = stream == null ? null : stream.getProperty(streamingKey);
        FullHttpMessage message = null;
        if (state == null) {
            message = stream == null ? null : getMessage(stream);
            if (message == null) {
                return super.onDataRead(ctx, streamId, data, padding, endOfStream);
            }
            if (!isRegisteredGrpc((HttpRequest) message)) {
                boolean closedSpoolBypass = stream.getProperty(closedSpoolBypassKey) != null;
                if ((ordinaryResources == null || closedSpoolBypass)
                                && (maxRequestContentLength < 0 || maxRequestContentLength > Integer.MAX_VALUE)) {
                    checkDegradedBodyLimit(message, data, streamId);
                }
                if (closedSpoolBypass) {
                    return super.onDataRead(ctx, streamId, data, padding, endOfStream);
                }
                if (ordinaryResources == null || (maxRequestContentLength >= 0 && maxRequestContentLength <= Integer.MAX_VALUE)) {
                    return super.onDataRead(ctx, streamId, data, padding, endOfStream);
                }
                ordinary = beginOrdinaryStream(ctx, stream, message);
                if (ordinary == null) {
                    // The closed owner did not take the inherited message or this DATA frame.
                    checkDegradedBodyLimit(message, data, streamId);
                    return super.onDataRead(ctx, streamId, data, padding, endOfStream);
                }
                return onOrdinaryData(ctx, stream, ordinary, data, padding, endOfStream);
            }
        }

        int length = data.readableBytes();
        long bytes = state == null ? 0 : state.bytes;
        if (bytes > Long.MAX_VALUE - length
                        || (maxRequestContentLength >= 0 && bytes > maxRequestContentLength - length)) {
            throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                            "Content length exceeded max of %d for stream id %d",
                            maxRequestContentLength >= 0 ? maxRequestContentLength : Long.MAX_VALUE, streamId);
        }

        if (state == null) {
            state = new StreamingState(ctx, stream);
            stream.setProperty(streamingKey, state);
            removeMessage(stream, false);
            try {
                ctx.fireChannelRead(new H2InboundRequest(streamId, (HttpRequest) message));
            } finally {
                message.release();
            }
        }

        state.bytes += length;
        StreamingDataCredit credit = length == 0 ? null : new StreamingDataCredit(state, length);
        try {
            ctx.fireChannelRead(new H2InboundContent(streamId, data.retainedDuplicate(), endOfStream, null,
                            credit));
        } catch (RuntimeException | Error failure) {
            // The decoder consumes outstanding frame credit when onDataRead throws.
            if (credit != null) {
                credit.cancel();
            }
            throw failure;
        }
        return padding;
    }

    private static void checkDegradedBodyLimit(FullHttpMessage message, ByteBuf data, int streamId) throws Http2Exception {
        if (data.readableBytes() > DEGRADED_ORDINARY_MAX_BYTES
                        || message.content().readableBytes() > DEGRADED_ORDINARY_MAX_BYTES - data.readableBytes()) {
            throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                            "HTTP/2 long-body spool unavailable for stream id %d", streamId);
        }
    }

    private OrdinaryState beginOrdinaryStream(ChannelHandlerContext ctx, Http2Stream stream,
                                              FullHttpMessage message) throws Http2Exception {
        int streamId = stream.id();
        boolean storedMessage = getMessage(stream) == message;
        boolean preserveStoredMessage = false;
        H2BodySpool spool = null;
        try {
            long declaredLength = declaredContentLength(message.headers(), streamId);
            if (maxRequestContentLength >= 0 && declaredLength > maxRequestContentLength) {
                throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                                "Content length exceeded max of %d for stream id %d",
                                maxRequestContentLength, streamId);
            }
            HttpRequest headers = new DefaultHttpRequest(message.protocolVersion(), ((HttpRequest) message).method(),
                            ((HttpRequest) message).uri(), message.headers().copy());
            OrdinaryState ordinary = new OrdinaryState(headers, declaredLength);
            spool = ordinaryResources.tryOpenStream(maxRequestContentLength >= 0 ? maxRequestContentLength : Long.MAX_VALUE,
                            ordinaryPendingBytes, maxDataFrameBytes,
                            (settledBytes, failure) -> onOrdinaryChunkSettled(ctx, stream, ordinary, settledBytes, failure));
            if (spool == null) {
                if (declaredLength > DEGRADED_ORDINARY_MAX_BYTES) {
                    throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                                    "HTTP/2 long-body spool unavailable for stream id %d", streamId);
                }
                stream.setProperty(closedSpoolBypassKey, Boolean.TRUE);
                preserveStoredMessage = storedMessage;
                return null;
            }
            ordinary.spool = spool;
            if (ordinary.aborted) {
                throw Http2Exception.streamError(streamId, Http2Error.INTERNAL_ERROR,
                                "HTTP/2 body spool closed before stream admission for stream id %d", streamId);
            }
            stream.setProperty(ordinaryKey, ordinary);
            return ordinary;
        } catch (H2SpoolResources.CapacityException failure) {
            throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                            "HTTP/2 body spool capacity exceeded for stream id %d", streamId);
        } catch (Http2Exception | RuntimeException | Error failure) {
            if (spool != null) {
                spool.abort(failure);
            }
            throw failure;
        } finally {
            if (storedMessage && !preserveStoredMessage) {
                removeMessage(stream, false);
            }
            if (!preserveStoredMessage) {
                message.release();
            }
        }
    }

    private int onOrdinaryData(ChannelHandlerContext ctx, Http2Stream stream, OrdinaryState ordinary,
                               ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
        int streamId = stream.id();
        if (ordinary.endReceived) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR,
                            "DATA received after HTTP/2 request END_STREAM");
        }
        int length = data.readableBytes();
        long bytes = ordinary.spool.acceptedBytes();
        if (bytes > Long.MAX_VALUE - length
                        || (maxRequestContentLength >= 0 && bytes > maxRequestContentLength - length)) {
            abortOrdinary(stream, ordinary, new IllegalStateException("HTTP/2 body limit exceeded"));
            throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                            "Content length exceeded max of %d for stream id %d",
                            maxRequestContentLength >= 0 ? maxRequestContentLength : Long.MAX_VALUE, streamId);
        }

        try {
            ordinary.spool.offer(data);
        } catch (H2BodySpool.CapacityException failure) {
            abortOrdinary(stream, ordinary, failure);
            throw Http2Exception.streamError(streamId, Http2Error.ENHANCE_YOUR_CALM,
                            "HTTP/2 body spool capacity exceeded for stream id %d", streamId);
        } catch (RuntimeException failure) {
            abortOrdinary(stream, ordinary, failure);
            throw Http2Exception.streamError(streamId, Http2Error.INTERNAL_ERROR, failure,
                            "HTTP/2 body spool failed for stream id %d", streamId);
        } catch (Error failure) {
            abortOrdinary(stream, ordinary, failure);
            throw failure;
        }
        if (endOfStream) {
            try {
                finishOrdinary(ctx, stream, ordinary);
            } catch (Http2Exception failure) {
                abortOrdinary(stream, ordinary, failure);
                throw failure;
            }
        }
        return padding;
    }

    private void onOrdinaryChunkSettled(ChannelHandlerContext ctx, Http2Stream stream,
                                        OrdinaryState ordinary, int bytes, Throwable failure) {
        boolean schedule;
        synchronized (ordinary) {
            if (ordinary.aborted) {
                return;
            }
            if (failure != null) {
                ordinary.chunkFailure = failure;
            } else if (ordinary.chunkFailure == null && bytes > 0) {
                if (ordinary.pendingCredit > Long.MAX_VALUE - bytes) {
                    ordinary.chunkFailure = new IllegalStateException("HTTP/2 body credit overflow");
                } else {
                    ordinary.pendingCredit += bytes;
                }
            }
            schedule = !ordinary.creditDrainScheduled
                            && (ordinary.chunkFailure != null || ordinary.pendingCredit > 0);
            if (schedule) {
                ordinary.creditDrainScheduled = true;
            }
        }
        if (!schedule) {
            return;
        }
        try {
            ctx.executor().execute(() -> drainOrdinaryCredit(ctx, stream, ordinary));
        } catch (RuntimeException | Error rejected) {
            // The event loop has stopped; the connection can no longer send WINDOW_UPDATE.
            ordinary.aborted = true;
            try {
                H2BodySpool spool = ordinary.spool;
                if (spool != null) {
                    spool.abort(rejected);
                }
            } catch (RuntimeException | Error ignored) {
                // The connection scope retains the lease for off-loop cleanup retry.
            }
        }
    }

    private void drainOrdinaryCredit(ChannelHandlerContext ctx, Http2Stream stream, OrdinaryState ordinary) {
        // A callback can add credit while this task consumes the current batch.
        boolean hadWork;
        do {
            long bytes;
            Throwable failure;
            synchronized (ordinary) {
                failure = ordinary.chunkFailure;
                bytes = ordinary.pendingCredit;
                ordinary.pendingCredit = 0;
                hadWork = failure != null || bytes != 0;
                if (ordinary.aborted || !hadWork) {
                    ordinary.creditDrainScheduled = false;
                }
            }
            if (ordinary.aborted) {
                return;
            }
            if (failure != null) {
                reportOrdinaryFailureOnLoop(ctx, stream, ordinary, failure);
                return;
            }
            if (bytes > 0) {
                try {
                    Http2LocalFlowController flow = connection.local().flowController();
                    boolean flush = false;
                    while (bytes > 0 && !ordinary.aborted) {
                        int portion = (int) Math.min(bytes, Integer.MAX_VALUE);
                        flush |= flow.consumeBytes(stream, portion);
                        bytes -= portion;
                    }
                    if (flush) {
                        ctx.flush();
                    }
                } catch (Http2Exception creditFailure) {
                    reportOrdinaryFailureOnLoop(ctx, stream, ordinary, creditFailure);
                    ordinary.aborted = true;
                    ctx.fireExceptionCaught(creditFailure);
                    return;
                }
            }
        } while (hadWork);
    }

    private boolean captureOrdinaryTrailers(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                                            boolean endOfStream) throws Http2Exception {
        Http2Stream stream = connection.stream(streamId);
        OrdinaryState ordinary = stream == null ? null : stream.getProperty(ordinaryKey);
        if (ordinary == null) {
            return false;
        }
        for (Map.Entry<CharSequence, CharSequence> header : headers) {
            if (header.getKey().length() > 0 && header.getKey().charAt(0) == ':') {
                abortOrdinary(stream, ordinary, new IllegalArgumentException("Invalid trailing pseudo-header"));
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Invalid trailing pseudo-header");
            }
        }
        if (ordinary.trailers == null) {
            ordinary.trailers = new DefaultHttpHeaders(false);
        }
        try {
            HttpConversionUtil.addHttp2ToHttpHeaders(streamId, headers, ordinary.trailers,
                            HttpVersion.HTTP_1_1, true, true);
        } catch (Http2Exception | RuntimeException | Error failure) {
            abortOrdinary(stream, ordinary, failure);
            throw failure;
        }
        if (endOfStream) {
            try {
                finishOrdinary(ctx, stream, ordinary);
            } catch (Http2Exception failure) {
                abortOrdinary(stream, ordinary, failure);
                throw failure;
            }
        }
        return true;
    }

    private void finishOrdinary(ChannelHandlerContext ctx, Http2Stream stream, OrdinaryState ordinary) throws Http2Exception {
        if (ordinary.endReceived) {
            throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR,
                            "Duplicate HTTP/2 request END_STREAM");
        }
        long actualLength = ordinary.spool.acceptedBytes();
        if (ordinary.declaredLength >= 0 && ordinary.declaredLength != actualLength) {
            throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR,
                            "HTTP/2 Content-Length %d does not match DATA length %d",
                            ordinary.declaredLength, actualLength);
        }
        ordinary.endReceived = true;
        ordinary.spool.seal().whenComplete((body, failure) -> {
            try {
                ctx.executor().execute(() -> {
                    if (failure != null) {
                        reportOrdinaryFailureOnLoop(ctx, stream, ordinary, failure);
                    } else {
                        publishOrdinary(ctx, stream, ordinary, body);
                    }
                });
            } catch (RejectedExecutionException rejected) {
                if (body != null) {
                    body.closeAsync();
                } else {
                    ordinary.aborted = true;
                    ordinary.spool.abort(rejected);
                }
            }
        });
    }

    private void publishOrdinary(ChannelHandlerContext ctx, Http2Stream stream, OrdinaryState ordinary,
                                 H2SealedBody body) {
        if (ordinary.aborted || !ctx.channel().isActive() || stream.getProperty(ordinaryKey) != ordinary) {
            body.closeAsync();
            return;
        }
        ChannelFuture continueWrite = ordinary.continueWrite;
        if (continueWrite != null) {
            if (!continueWrite.isDone()) {
                continueWrite.addListener(future -> {
                    try {
                        ctx.executor().execute(() -> publishOrdinary(ctx, stream, ordinary, body));
                    } catch (RejectedExecutionException rejected) {
                        ordinary.aborted = true;
                        body.closeAsync();
                    }
                });
                return;
            }
            if (!continueWrite.isSuccess()) {
                body.closeAsync();
                return;
            }
        }
        H2CompletedSpoolRequest request;
        try {
            if (body.length() != ordinary.spool.acceptedBytes()) {
                throw new IllegalStateException("HTTP/2 sealed body length does not match DATA length");
            }
            request = new H2CompletedSpoolRequest(stream.id(), ordinary.headers, body, ordinary.trailers);
        } catch (RuntimeException | Error failure) {
            body.closeAsync();
            reportOrdinaryFailureOnLoop(ctx, stream, ordinary, failure);
            return;
        }
        stream.removeProperty(ordinaryKey);
        ordinary.published = true;
        try {
            ctx.fireChannelRead(request);
        } catch (RuntimeException | Error failure) {
            request.closeAsync();
            ctx.fireExceptionCaught(failure);
        }
    }

    private void reportOrdinaryFailureOnLoop(ChannelHandlerContext ctx, Http2Stream stream,
                                             OrdinaryState ordinary, Throwable failure) {
        if (ordinary.aborted || ordinary.published) {
            return;
        }
        abortOrdinary(stream, ordinary, failure);
        Http2ConnectionHandler handler = ctx.pipeline().get(Http2ConnectionHandler.class);
        if (handler != null && ctx.channel().isActive()) {
            handler.resetStream(ctx, stream.id(), Http2Error.INTERNAL_ERROR.code(), ctx.newPromise())
                            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            ctx.flush();
        }
    }

    private void abortOrdinary(Http2Stream stream, OrdinaryState ordinary, Throwable cause) {
        if (!ordinary.aborted && !ordinary.published) {
            ordinary.aborted = true;
            if (stream.getProperty(ordinaryKey) == ordinary) {
                stream.removeProperty(ordinaryKey);
            }
            ordinary.spool.abort(cause);
        }
    }

    private static long declaredContentLength(HttpHeaders headers, int streamId) throws Http2Exception {
        return declaredContentLength(headers.getAll(HttpHeaderNames.CONTENT_LENGTH), streamId);
    }

    private static long declaredContentLength(List<? extends CharSequence> values, int streamId) throws Http2Exception {
        if (values.isEmpty()) {
            return -1;
        }
        if (values.size() != 1) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR,
                            "Multiple HTTP/2 Content-Length values");
        }
        CharSequence text = values.get(0);
        if (text.length() == 0) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR,
                            "Invalid HTTP/2 Content-Length");
        }
        long value = 0;
        for (int i = 0; i < text.length(); i++) {
            char digit = text.charAt(i);
            if (digit < '0' || digit > '9' || value > (Long.MAX_VALUE - (digit - '0')) / 10) {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR,
                                "Invalid HTTP/2 Content-Length");
            }
            value = value * 10 + digit - '0';
        }
        return value;
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
                              boolean endOfStream) throws Http2Exception {
        if (!captureOrdinaryTrailers(ctx, streamId, headers, endOfStream)
                        && !publishStreamingTrailers(ctx, streamId, headers, endOfStream)) {
            checkEmptyOrdinaryEnd(streamId, headers, endOfStream);
            super.onHeadersRead(ctx, streamId, headers, padding, endOfStream);
        }
    }

    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                              short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
        if (!captureOrdinaryTrailers(ctx, streamId, headers, endOfStream)
                        && !publishStreamingTrailers(ctx, streamId, headers, endOfStream)) {
            checkEmptyOrdinaryEnd(streamId, headers, endOfStream);
            super.onHeadersRead(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream);
            Http2Stream stream = connection.stream(streamId);
            OrdinaryState ordinary = stream == null ? null : stream.getProperty(ordinaryKey);
            if (ordinary != null) {
                if (streamDependency > 0) {
                    ordinary.headers.headers().setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_DEPENDENCY_ID.text(),
                                    streamDependency);
                }
                ordinary.headers.headers().setShort(HttpConversionUtil.ExtensionHeaderNames.STREAM_WEIGHT.text(), weight);
            }
        }
    }

    private void checkEmptyOrdinaryEnd(int streamId, Http2Headers headers, boolean endOfStream) throws Http2Exception {
        if (!endOfStream || (maxRequestContentLength >= 0 && maxRequestContentLength <= Integer.MAX_VALUE)) {
            return;
        }
        Http2Stream stream = connection.stream(streamId);
        if (stream == null || stream.getProperty(streamingKey) != null) {
            return;
        }
        FullHttpMessage message = getMessage(stream);
        long declaredLength = message == null
                        ? declaredContentLength(headers.getAll(HttpHeaderNames.CONTENT_LENGTH), streamId)
                        : declaredContentLength(message.headers(), streamId);
        if (declaredLength > 0) {
            throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR,
                            "HTTP/2 Content-Length %d does not match DATA length 0", declaredLength);
        }
    }

    private boolean publishStreamingTrailers(ChannelHandlerContext ctx, int streamId, Http2Headers headers,
                                             boolean endOfStream) throws Http2Exception {
        Http2Stream stream = connection.stream(streamId);
        if (stream == null || stream.getProperty(streamingKey) == null) {
            return false;
        }
        for (Map.Entry<CharSequence, CharSequence> header : headers) {
            if (header.getKey().length() > 0 && header.getKey().charAt(0) == ':') {
                throw Http2Exception.streamError(streamId, Http2Error.PROTOCOL_ERROR, "Invalid trailing pseudo-header");
            }
        }
        HttpHeaders trailers = new DefaultHttpHeaders(false);
        HttpConversionUtil.addHttp2ToHttpHeaders(streamId, headers, trailers,
                        HttpVersion.HTTP_1_1, true, true);
        ctx.fireChannelRead(new H2InboundContent(streamId, Unpooled.EMPTY_BUFFER, endOfStream, trailers));
        return true;
    }

    private static boolean isRegisteredGrpc(HttpRequest request) {
        CharSequence mimeType = HttpUtil.getMimeType(request);
        if (mimeType == null || !"application/grpc".equalsIgnoreCase(mimeType.toString())) {
            return false;
        }
        String path = request.uri();
        int lastSlash = path.lastIndexOf('/');
        if (!path.startsWith("/") || lastSlash <= 1) {
            return false;
        }
        synchronized (GrpcServletServices.class) {
            Map<String, GrpcServletServices.ServiceInformation> services = GrpcServletServices.getServletGrpcServices();
            return services != null && services.containsKey(path.substring(1, lastSlash));
        }
    }

    private final class StreamingState {
        long bytes;

        private final ChannelHandlerContext ctx;
        private final Http2Stream stream;
        private final AtomicLong pendingCredit = new AtomicLong();
        private final AtomicBoolean drainScheduled = new AtomicBoolean();

        StreamingState(ChannelHandlerContext ctx, Http2Stream stream) {
            this.ctx = ctx;
            this.stream = stream;
        }

        void scheduleCreditDrain() {
            if (pendingCredit.get() <= 0 || !drainScheduled.compareAndSet(false, true)) {
                return;
            }
            // One event-loop task drains credit from any number of released chunks.
            try {
                ctx.executor().execute(() -> {
                    try {
                        long ready = pendingCredit.getAndSet(0);
                        boolean flush = false;
                        Http2LocalFlowController flow = connection.local().flowController();
                        while (ready > 0) {
                            int part = (int) Math.min(ready, Integer.MAX_VALUE);
                            flush |= flow.consumeBytes(stream, part);
                            ready -= part;
                        }
                        if (flush) {
                            ctx.flush();
                        }
                    } catch (Http2Exception failure) {
                        ctx.fireExceptionCaught(failure);
                    } finally {
                        drainScheduled.set(false);
                        if (pendingCredit.get() > 0) {
                            scheduleCreditDrain();
                        }
                    }
                });
            } catch (RejectedExecutionException ignored) {
                drainScheduled.set(false);
                // The connection's event loop has stopped; no WINDOW_UPDATE can be sent.
            }
        }
    }

    private static final class OrdinaryState {
        final HttpRequest headers;
        final long declaredLength;
        volatile H2BodySpool spool;
        HttpHeaders trailers;
        ChannelFuture continueWrite;
        boolean endReceived;
        boolean published;
        volatile boolean aborted;
        long pendingCredit;
        Throwable chunkFailure;
        boolean creditDrainScheduled;

        OrdinaryState(HttpRequest headers, long declaredLength) {
            this.headers = headers;
            this.declaredLength = declaredLength;
        }
    }

    private final class StreamingDataCredit implements BodyQueue.AggregatingCompletion {
        private final StreamingState state;
        private final int length;
        private StreamingDataCredit root = this;
        private int aggregateLength;
        private boolean released;
        private boolean cancelled;

        StreamingDataCredit(StreamingState state, int length) {
            this.state = state;
            this.length = length;
            this.aggregateLength = length;
        }

        @Override
        public void merge(BodyQueue.AggregatingCompletion next) {
            if (!(next instanceof StreamingDataCredit)) {
                throw new IllegalArgumentException("HTTP/2 DATA credit has a different owner");
            }
            StreamingDataCredit credit = (StreamingDataCredit) next;
            synchronized (this) {
                if (root != this || released || credit.root != credit || credit.state != state) {
                    throw new IllegalArgumentException("HTTP/2 DATA credit crossed stream or chunk ownership");
                }
                if (aggregateLength > Integer.MAX_VALUE - credit.length) {
                    throw new IllegalArgumentException("HTTP/2 chunk credit overflow");
                }
                aggregateLength += credit.length;
                credit.root = this;
            }
        }

        @Override
        public void run() {
            StreamingDataCredit owner = root;
            int ready;
            synchronized (owner) {
                if (owner.released) {
                    return;
                }
                owner.released = true;
                ready = owner.aggregateLength;
                if (ready > 0) {
                    owner.state.pendingCredit.addAndGet(ready);
                }
            }
            if (ready > 0) {
                owner.state.scheduleCreditDrain();
            }
        }

        void cancel() {
            StreamingDataCredit owner = root;
            synchronized (owner) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                owner.aggregateLength -= length;
                if (owner.released) {
                    // Decoder failure is synchronous; its event loop has not run
                    // the deferred drain task for this frame yet.
                    owner.state.pendingCredit.addAndGet(-length);
                }
            }
        }
    }

    private final class DataCredit {
        private final ChannelHandlerContext ctx;
        private final Http2Stream stream;
        private final int length;
        private final OrdinaryState ordinary;
        private final AtomicBoolean completed = new AtomicBoolean();
        private volatile boolean cancelled;

        DataCredit(ChannelHandlerContext ctx, Http2Stream stream, int length) {
            this(ctx, stream, length, null);
        }

        DataCredit(ChannelHandlerContext ctx, Http2Stream stream, int length, OrdinaryState ordinary) {
            this.ctx = ctx;
            this.stream = stream;
            this.length = length;
            this.ordinary = ordinary;
        }

        void complete() {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            // Always defer: the decoder must finish the current frame first.
            try {
                ctx.executor().execute(() -> {
                    if (cancelled || (ordinary != null && ordinary.aborted)) {
                        return;
                    }
                    try {
                        Http2LocalFlowController flow = connection.local().flowController();
                        if (flow.consumeBytes(stream, length)) {
                            ctx.flush();
                        }
                    } catch (Http2Exception failure) {
                        ctx.fireExceptionCaught(failure);
                    }
                });
            } catch (RejectedExecutionException ignored) {
                // The connection's event loop has stopped; no WINDOW_UPDATE can be sent.
            }
        }

        void cancel() {
            cancelled = true;
        }
    }

    @Override
    @FFDCIgnore(NullPointerException.class)
    // Extended to properly get stream errors when working with header parsing with missing pesudo-headers
    protected io.netty.handler.codec.http.FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, io.netty.handler.codec.http2.Http2Stream stream,
                                                                              io.netty.handler.codec.http2.Http2Headers headers, boolean endOfStream, boolean allowAppend,
                                                                              boolean appendToTrailer) throws io.netty.handler.codec.http2.Http2Exception {
        if (getMessage(stream) != null) {
            for (Map.Entry<CharSequence, CharSequence> header : headers) {
                if (header.getKey().length() > 0 && header.getKey().charAt(0) == ':') {
                    throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, "Invalid trailing pseudo-header");
                }
            }
            return super.processHeadersBegin(ctx, stream, headers, endOfStream, allowAppend, appendToTrailer);
        }
        try {
            boolean containsPath = Objects.nonNull(headers.path()) && !headers.path().toString().isEmpty();
            boolean containsScheme = Objects.nonNull(headers.scheme()) && !headers.scheme().toString().isEmpty();
            if (headers.method().toString().equalsIgnoreCase(HttpMethod.CONNECT.asciiName().toString())) {
                if (containsPath || containsScheme || Objects.isNull(headers.authority()))
                    throw new NullPointerException("Connect method request must omit path and scheme values!");
            } else {
                if (!containsPath)
                    throw new NullPointerException("Request path must have a value!");
                if (!containsScheme)
                    throw new NullPointerException("Request scheme must have a value!");
            }
            if (!endOfStream && allowAppend && connection.isServer()
                            && (maxRequestContentLength < 0 || maxRequestContentLength > Integer.MAX_VALUE)) {
                FullHttpMessage candidate = newMessage(stream, headers, validateHttpHeaders, ctx.alloc());
                boolean registered;
                try {
                    registered = isRegisteredGrpc((HttpRequest) candidate);
                } catch (RuntimeException | Error failure) {
                    candidate.release();
                    throw failure;
                }
                if (registered) {
                    candidate.release();
                } else if (ordinaryResources == null) {
                    try {
                        if (declaredContentLength(candidate.headers(), stream.id()) > DEGRADED_ORDINARY_MAX_BYTES) {
                            throw Http2Exception.streamError(stream.id(), Http2Error.ENHANCE_YOUR_CALM,
                                            "HTTP/2 long-body spool unavailable for stream id %d", stream.id());
                        }
                    } finally {
                        candidate.release();
                    }
                } else {
                    boolean continueExpected = candidate.headers().contains(HttpHeaderNames.EXPECT)
                                    && hasSingle100Continue((HttpRequest) candidate);
                    OrdinaryState ordinary = beginOrdinaryStream(ctx, stream, candidate);
                    if (ordinary != null) {
                        if (continueExpected) {
                            sendOrdinaryContinue(ctx, stream, ordinary);
                        }
                        return null;
                    }
                }
            }
            return super.processHeadersBegin(ctx, stream, headers, endOfStream, allowAppend, appendToTrailer);
        } catch (NullPointerException e) {
            throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, e.getMessage());
        } catch (Exception e2) {
            throw e2;
        }
    }

    private static boolean hasSingle100Continue(HttpRequest request) {
        return request.headers().getAll(HttpHeaderNames.EXPECT).size() == 1
                        && HttpUtil.is100ContinueExpected(request);
    }

    private void sendOrdinaryContinue(ChannelHandlerContext ctx, Http2Stream stream,
                                      OrdinaryState ordinary) throws Http2Exception {
        try {
            // A FullHttpResponse would end the HTTP/2 response stream at 100.
            DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
            HttpUtil.setContentLength(response, 0);
            byte[] date = HttpDispatcher.getDateFormatter().getRFC1123TimeAsBytes(httpConfig.getDateHeaderRange());
            response.headers().set(HttpHeaderKeys.HDR_DATE.getName(), new String(date, StandardCharsets.UTF_8));
            response.headers().setInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), stream.id());
            ChannelFuture write = ctx.channel().writeAndFlush(response);
            ordinary.continueWrite = write;
            write.addListener(future -> {
                if (!future.isSuccess()) {
                    Throwable failure = future.cause();
                    onOrdinaryChunkSettled(ctx, stream, ordinary, 0,
                                    failure != null ? failure : new IllegalStateException("HTTP/2 100 Continue write failed"));
                }
            });
        } catch (RuntimeException failure) {
            abortOrdinary(stream, ordinary, failure);
            throw Http2Exception.streamError(stream.id(), Http2Error.INTERNAL_ERROR, failure,
                            "HTTP/2 100 Continue write failed for stream id %d", stream.id());
        } catch (Error failure) {
            abortOrdinary(stream, ordinary, failure);
            throw failure;
        }
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {
        goAwayReceived.getAndSet(true);
        super.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
        sendGoAwayIfClosing();
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
        if (isResetTrackingEnabled())
            incrementResetCount();
        Http2Stream stream = connection.stream(streamId);
        if (stream != null) {
            stream.removeProperty(streamingKey);
        }
        FullHttpMessage msg = getMessage(stream);
        if (msg != null) {
            onRstStreamRead(stream, msg);
        }
        Http2Error code = Http2Error.valueOf(errorCode);
        if (Objects.isNull(code)) {
            code = Http2Error.INTERNAL_ERROR;
        }
        if (isResetsInTimeExceeded()) {
            throw RST_FRAME_RATE_EXCEEDED;
        }
        ctx.fireExceptionCaught(Http2Exception.streamError(streamId, code,
                                                           "HTTP/2 to HTTP layer caught stream reset"));
    }

    public void incrementResetCount() {
        resetFrameCount++;
    }

    public boolean isResetTrackingEnabled() {
        return maxResetFrames > 0;
    }

    public boolean isResetsInTimeExceeded() {
        // Are we checking the reset frames/time ?
        if (isResetTrackingEnabled()) {
            // Is the window limited?
            if (resetFrameWindow > 0) {
                long curResetTime = System.nanoTime();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "setting curResetTime: " + curResetTime);
                }

                if (curResetTime - startResetTime < TimeUnit.MILLISECONDS.toNanos(resetFrameWindow)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "number of resets in time is " + resetFrameCount);
                    }
                    if (resetFrameCount >= maxResetFrames) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "too many resets within time");
                        }
                        return true;
                    }

                } else {
                    // Start over with a new window
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "restarting reset frame time window " + curResetTime);
                    }
                    startResetTime = curResetTime;
                    resetFrameCount = 0;
                }
            } else {
                // Unlimited time window
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "number of resets in unlimited time is " + resetFrameCount);
                }
                if (resetFrameCount >= maxResetFrames) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "reset frames in unlimited window exceeded: " + resetFrameCount);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onStreamClosed(Http2Stream stream) {
        super.onStreamClosed(stream);
        // After stream was closed, check if we need to send a go away frame
        sendGoAwayIfClosing();
    }

    @Override
    public void onStreamRemoved(Http2Stream stream) {
        stream.removeProperty(streamingKey);
        super.onStreamRemoved(stream);
    }

    private void sendGoAwayIfClosing() {
        // We received a go away and need to check if we have finished the streams to close
        // the connection and send a go away back if we haven't sent one already.
        // I was considering using the API goAwayReceived() from the H2Connection but that
        // unfortunately is called/set after this listener is called so keeping goAwayReceived to track it.
        if(goAwayReceived.get() && connection.numActiveStreams() == 0 && !connection.goAwaySent()) {
            channel.close();
        }
    }

}
