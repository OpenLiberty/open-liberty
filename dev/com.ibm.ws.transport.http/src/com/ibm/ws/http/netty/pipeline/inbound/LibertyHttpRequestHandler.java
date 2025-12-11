/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound;

import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

/**
 * Handler that works on queueing requests as they come through the aggregator for proper HTTP pipelining support
 * and also handles the behavior of closing a connection after we finish processing HTTP 1.1 requests if the remote
 * peer already closed the connection. This handler MUST be added before the dispatcher handler to be able to
 * appropriately handle requests
 */
public class LibertyHttpRequestHandler extends ChannelDuplexHandler {

    private static final TraceComponent tc = Tr.register(LibertyHttpRequestHandler.class);
    private static final int DEFAULT_MAX_QUEUE = 50;

    private final Object lock = new Object();

    private final LinkedBlockingQueue<FullHttpRequest> requestQueue;
    private boolean peerClosedConnection = false;
    private ChannelHandlerContext requestHandlerContext;

    private final int maxRequests;
    private final int maxQueuedRequests;
    private final boolean hasMaxRequests;

    private int acceptedRequests = 0;
    private int completedRequests = 0;

    private boolean closeAfterDrain = false;
    private boolean handlingRequest = false;

    public LibertyHttpRequestHandler(HttpChannelConfig config) {
        int configMaxRequests = config.getMaximumPersistentRequests();

        this.hasMaxRequests = configMaxRequests > 0;
        this.maxRequests = hasMaxRequests ? configMaxRequests : -1;

        int queueMax = hasMaxRequests ? Math.min(configMaxRequests, DEFAULT_MAX_QUEUE) : DEFAULT_MAX_QUEUE;
        this.maxQueuedRequests = queueMax;

        this.requestQueue = new LinkedBlockingQueue<FullHttpRequest>(maxQueuedRequests);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext context) {
        context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(false);
        requestHandlerContext = context;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) {
        FullHttpRequest request;
        while ((request = requestQueue.poll()) != null) {
            ReferenceCountUtil.safeRelease(request);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception{
        FullHttpRequest request;
        while ((request = requestQueue.poll()) != null) {
            ReferenceCountUtil.safeRelease(request);
        }
        super.channelInactive(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            if (!(request.decoderResult().isFinished() && request.decoderResult().isSuccess())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Bad decode result, close connection. Cause: " + request.decoderResult().cause());
                }
                ReferenceCountUtil.safeRelease(request);
                context.close();
                return;
            }
            if (closeAfterDrain || (hasMaxRequests && acceptedRequests >= maxRequests)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Pausing read for channel " + context.channel());
                }
                ReferenceCountUtil.safeRelease(request);
                closeAfterDrain = true;
                pauseReading(context);
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Reading Full HTTP Request for channel: " + context.channel());
            }

            if (handlingRequest) {
                if (!requestQueue.offer(request)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Queue full. Dropping new requests and draining to close.");
                    }
                    ReferenceCountUtil.safeRelease(request);
                    closeAfterDrain = true;
                    pauseReading(context);
                    return;
                }
                acceptedRequests++;
                if (requestQueue.remainingCapacity() == 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Queue reached capacity. Reads are paused.");
                    }
                    pauseReading(context);
                }

                if (hasMaxRequests && acceptedRequests >= maxRequests) {
                    closeAfterDrain = true;
                    pauseReading(context);
                }
                return;
            }
            handlingRequest = true;
            acceptedRequests++;
            if (hasMaxRequests && acceptedRequests >= maxRequests) {
                closeAfterDrain = true;
                pauseReading(context);
            }
            context.fireChannelRead(request);
        } else {
            context.fireChannelRead(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
        if (!peerClosedConnection && (event instanceof ChannelInputShutdownEvent || event instanceof ChannelInputShutdownReadComplete)) {
            // If handling request we just need to wait until processing finishes to handle the closing
            // else we should close the channel up now
            if (handlingRequest) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Peer closed the connection while we were handling a request, ending the connection after finishing processing");
                }
                peerClosedConnection = true;
                pauseReading(context);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Peer closed the connection and there was no request being handled, closing the channel");
                }
                context.close();
            }
            return;
        }
        super.userEventTriggered(context, event);
    }

    @Override
    public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) throws Exception{
        ChannelFuture writeFuture = context.write(message, promise);
        if (message instanceof LastHttpContent) {
            // Found last HTTP content for the current request, can queue up other reads now
            completedRequests++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Processing next available request in request queue. Completed requests: " + completedRequests + " of max " +
                                maxRequests + ". Queued requests: " + requestQueue.size());
            }
            boolean draining = peerClosedConnection || closeAfterDrain || (hasMaxRequests && completedRequests >= maxRequests);
            if (draining && requestQueue.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Closing connection: " + requestHandlerContext.channel() + " because peer ended the connection and we have finished processing.");
                }
                writeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        requestHandlerContext.close();
                    }
                });
                return;
            }

            FullHttpRequest nextRequest = requestQueue.poll();
            if (nextRequest == null) {
                handlingRequest = false;
                if (draining) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "No additional requests remaining. Closing channel.");
                    }
                    writeFuture.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            requestHandlerContext.close();
                        }
                    });
                } else if(!context.channel().config().isAutoRead()){
                    resumeReading(context);                                                      
                }
                return;
            }    
            else if(!draining && !context.channel().config().isAutoRead() && requestQueue.remainingCapacity()>0){
                resumeReading(context);
            }
            handlingRequest = true;
            writeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    requestHandlerContext.fireChannelRead(nextRequest);
                }
            });
        }
    }

    private static void pauseReading(ChannelHandlerContext context) {
        ChannelConfig config = context.channel().config();
        if (config.isAutoRead()) {
            config.setAutoRead(false);
        }
    }

    private static void resumeReading(ChannelHandlerContext context) {
        ChannelConfig config = context.channel().config();
        if (!config.isAutoRead()) {
            config.setAutoRead(true);
        }
    }

}
