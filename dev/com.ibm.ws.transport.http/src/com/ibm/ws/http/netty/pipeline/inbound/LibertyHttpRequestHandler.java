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

import com.ibm.ws.http.netty.NettyHttpConstants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 *
 */
public class LibertyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final LinkedBlockingQueue<FullHttpRequest> requestQueue = new LinkedBlockingQueue<FullHttpRequest>();
    private boolean peerClosedConnection = false;
    private ChannelHandlerContext requestHandlerContext;

    public LibertyHttpRequestHandler() {
        super(false);
        System.out.println("Added Request Handler!!");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ctx.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(false);
        System.out.println("Setting context: " + ctx);
        requestHandlerContext = ctx;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        // TODO Need to see if we need to check decoder result from request to ensure data is properly parsed as expected
        System.out.println("Reading Full HTTP Request for channel: " + context.channel());
        if (request.decoderResult().isFinished() && request.decoderResult().isSuccess()) {
            System.out.println("Success handling request, verifying if it needs to be queued");
//            FullHttpRequest msg = ReferenceCountUtil.retain(request, 1);
            synchronized (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST)) {
                if (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).get()) {
                    System.out.println("Request in progress, queueing next request");
                    requestQueue.add(request);
                    return;
                }
                context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(true);
            }
            System.out.println("Firing channel read!!");
            context.fireChannelRead(request);
        } else {
            System.out.println("Caught an unsuccesful decode while decoding result! " + request.decoderResult().cause() + " so will ignore message: " + request);
            if (request.decoderResult().cause() != null)
                request.decoderResult().cause().printStackTrace();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext context, Object evt) throws Exception {
        System.out.println("User event triggered now! " + evt);
        synchronized (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST)) {
            if (evt instanceof ChannelInputShutdownEvent) {
                // If handling request we just need to wait until it ends to handle the error
                // else we should close the channel up now
                if (context.channel().attr(NettyHttpConstants.HANDLING_REQUEST).get()) {
                    System.out.println("Peer closed the connection while we were handling a request, ending the connection after finishing writing");
                    peerClosedConnection = true;
                } else {
                    System.out.println("Peer closed the connection and there was no request being handled, closing the channel");
                    context.close();
                    return;
                }
            }
        }
        super.userEventTriggered(context, evt);
    }

    // Method to allow for handling the next request if any in the queue of requests
    public void processNextRequest() throws Exception {
        System.out.println("ProcessNextRequest called!!");
        synchronized (requestHandlerContext.channel().attr(NettyHttpConstants.HANDLING_REQUEST)) {
            if (peerClosedConnection) {
                System.out.println("Closing after peer finished the connection and we finished writing");
                requestHandlerContext.close();
                return;
            }
            if (requestQueue.isEmpty()) {
                System.out.println("No further requests to process!!");
                requestHandlerContext.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(false);
                return;
            }
            requestHandlerContext.channel().attr(NettyHttpConstants.HANDLING_REQUEST).set(true);
        }
        System.out.println("Processing next request in queue!!");
        requestHandlerContext.fireChannelRead(requestQueue.remove());
    }

}
