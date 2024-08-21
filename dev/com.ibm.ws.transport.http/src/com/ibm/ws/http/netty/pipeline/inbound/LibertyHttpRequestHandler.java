/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Handler that works on queueing requests as they come through the aggregator for proper HTTP pipelining support
 * and also handles the behavior of closing a connection after we finish processing HTTP 1.1 requests if the remote
 * peer already closed the connection. This handler MUST be added before the dispatcher handler to be able to
 * appropriately handle requests
 */
public class LibertyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final TraceComponent tc = Tr.register(LibertyHttpRequestHandler.class);

    private final LinkedBlockingQueue<FullHttpRequest> requestQueue = new LinkedBlockingQueue<FullHttpRequest>();
    private boolean peerClosedConnection = false;
    private ChannelHandlerContext requestHandlerContext;

    public LibertyHttpRequestHandler() {
        super(false);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ctx.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(false);
        requestHandlerContext = ctx;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        // TODO Need to see if we need to check decoder result from request to ensure data is properly parsed as expected
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Reading Full HTTP Request for channel: " + context.channel());
        }
        if (request.decoderResult().isFinished() && request.decoderResult().isSuccess()) {
            synchronized (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST)) {
                if (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).get()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Request already in progress! Adding to request queue...");
                    }
                    requestQueue.add(request);
                    return;
                }
                context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(true);
            }
            context.fireChannelRead(request);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Caught an unsuccesful decode while decoding result! " + request.decoderResult().cause() + " so will ignore message: " + request);
            }
            if (request.decoderResult().cause() != null)
                request.decoderResult().cause().printStackTrace();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object evt) throws Exception {
        if (evt instanceof ChannelInputShutdownEvent) {
            synchronized (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST)) {
                // If handling request we just need to wait until processing finishes to handle the closing
                // else we should close the channel up now
                if (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).get()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Peer closed the connection while we were handling a request, ending the connection after finishing processing");
                    }
                    peerClosedConnection = true;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Peer closed the connection and there was no request being handled, closing the channel");
                    }
                    context.close();
                    return;
                }
            }
        }
        super.userEventTriggered(context, evt);
    }

    // Method to allow for handling the next request if any in the queue of requests
    public void processNextRequest() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Processing next available request in request queue");
        }
        synchronized (requestHandlerContext.channel().attr(NettyHttpConstants.HANDLING_REQUEST)) {
            if (peerClosedConnection) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Closing connection: " + requestHandlerContext.channel() + " because peer ended the connection and we have finished processing.");
                }
                requestHandlerContext.close();
                return;
            }
            if (requestQueue.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "No additional requests found in queue...");
                }
                requestHandlerContext.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(false);
                return;
            }
            requestHandlerContext.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(true);
        }
        requestHandlerContext.fireChannelRead(requestQueue.remove());
    }

}
