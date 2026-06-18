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

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;

/**
 * An extension of {@link InboundHttp2ToHttpAdapter} for Liberty specific functionality.
 * Specifically the {@link processHeadersBegin()} for necessary header verification to
 * launch stream errors handled further in the pipeline to match the same behavior as
 * before.
 */
public class LibertyInboundHttp2ToHttpAdapter extends InboundHttp2ToHttpAdapter implements ResetFrameTracker {
    private static final TraceComponent tc = Tr.register(LibertyInboundHttp2ToHttpAdapter.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final Channel channel;
    private final int maxResetFrames;
    private final int resetFrameWindow;
    
    private volatile long startResetTime = System.nanoTime();
    private volatile int resetFrameCount = 0; //tracks both inbound and outbound resets

    private AtomicBoolean goAwayReceived = new AtomicBoolean(false);

    static final Http2Exception RST_FRAME_RATE_EXCEEDED = new Http2Exception(Http2Error.ENHANCE_YOUR_CALM,
            "too many reset frames processed");


    protected LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, int maxContentLength, boolean validateHttpHeaders, boolean propagateSettings, Channel channel, HttpChannelConfig httpConfig) {
        super(connection, maxContentLength, validateHttpHeaders, propagateSettings);
        this.channel = channel;
        this.maxResetFrames = httpConfig.getH2MaxResetFrames();
        this.resetFrameWindow = httpConfig.getH2ResetFramesWindow();
    }

    @Override
    @FFDCIgnore(NullPointerException.class)
    // Extended to properly get stream errors when working with header parsing with missing pesudo-headers
    protected io.netty.handler.codec.http.FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, io.netty.handler.codec.http2.Http2Stream stream,
                                                                              io.netty.handler.codec.http2.Http2Headers headers, boolean endOfStream, boolean allowAppend,
                                                                              boolean appendToTrailer) throws io.netty.handler.codec.http2.Http2Exception {
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
            return super.processHeadersBegin(ctx, stream, headers, endOfStream, allowAppend, appendToTrailer);
        } catch (NullPointerException e) {
            throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, e.getMessage());
        } catch (Exception e2) {
            throw e2;
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
