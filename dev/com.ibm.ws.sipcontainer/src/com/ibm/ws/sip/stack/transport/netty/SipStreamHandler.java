/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transport.GenericEndpointImpl;
import com.ibm.ws.sip.stack.transport.sip.netty.*;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import jain.protocol.ip.sip.ListeningPoint;
import java.io.IOException;

import io.openliberty.netty.internal.exception.NettyException;

public class SipStreamHandler extends SimpleChannelInboundHandler<SipMessageByteBuffer> {

    private static final TraceComponent tc = Tr.register(SipStreamHandler.class);

    final private AttributeKey<SipTcpInboundConnLink> attrKey = AttributeKey.valueOf("SipTcpInboundConnLink");

    private boolean processingMessage = false;
    private static final int DEFAULT_MAX_QUEUE = 50;
    private final LinkedBlockingQueue<SipMessageByteBuffer> messageQueue;

    public SipStreamHandler() {
        this.messageQueue = new LinkedBlockingQueue<SipMessageByteBuffer>(DEFAULT_MAX_QUEUE);
    }

    /**
     * Called when a new connection is established
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelActive", ctx.channel().remoteAddress() + " connected");
        }

        ListeningPoint lp = SipHandlerUtils.getListeningPoint(ctx.channel().localAddress(), ListeningPoint.TRANSPORT_TCP);
        SipInboundChannel inboundChannel = null;

        if (lp != null) {
            inboundChannel = SIPConnectionFactoryImplWs.instance().getInboundChannels().get(lp);
        }

        if (inboundChannel != null) {
            Attribute<SipTcpInboundConnLink> attr = ctx.channel().attr(attrKey);
            attr.setIfAbsent(new SipTcpInboundConnLink(inboundChannel, ctx.channel()));
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelActive",
                        "could not associate an incoming connection with a SIP channel");
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SipMessageByteBuffer msg) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelRead0",
                    ctx.channel() + ". [" + msg.getMarkedBytesNumber() + "] bytes received");
        }
        Attribute<SipTcpInboundConnLink> attr = ctx.channel().attr(attrKey);
        SipTcpInboundConnLink connLink = attr.get();
        if (connLink == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelRead0", "could not associate an incoming message with a SIP channel");
            }
            throw new IOException("could not associate an incoming message with a SIP channel");
        }
        messageQueue.offer(ReferenceCountUtil.retain(msg));
        if (processingMessage) {
            if (messageQueue.remainingCapacity() == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Queue reached capacity. Reads are paused.");
                }
                pauseReading(ctx);
            }
        } else {
            processingMessage = true;
            processNextMessage(ctx, connLink);
        }
    }

    // This is designed so that the logic besides what runs in the Liberty executor pool
    // needs to run on the event loop, otherwise there could be race conditions that show up.
    private void processNextMessage(final ChannelHandlerContext ctx, final SipTcpInboundConnLink connLink){
        SipMessageByteBuffer msg = messageQueue.poll();
        // Continue reading again if isn't autoread and we have space to queue more messages
        if(!ctx.channel().config().isAutoRead() && messageQueue.remainingCapacity() > DEFAULT_MAX_QUEUE/2){
            resumeReading(ctx);
        }
        if (msg == null) {
            processingMessage = false;
            return;
        }
        if (GenericEndpointImpl.getExecutorService() == null) {
            // We should not process messages in the event loop of Netty so throw exception here
            ctx.fireExceptionCaught(new NettyException("Null executor service while processing message!"));
        }
        GenericEndpointImpl.getExecutorService().execute(() -> {
            try {
                connLink.complete(msg);
            } finally {
                ReferenceCountUtil.release(msg);
            }
            ctx.channel().eventLoop().execute(() -> processNextMessage(ctx, connLink));
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelInactive", ctx.channel().remoteAddress() + " has been disconnected");
        }
        emptyMessageQueue();
        Attribute<SipTcpInboundConnLink> attr = ctx.channel().attr(attrKey);
        SipTcpInboundConnLink connLink = attr.get();
        // clean up from connections table
        if (connLink != null) {
            ExecutorService executor = GenericEndpointImpl.getExecutorService();
            if(executor != null) {
                executor.execute(() -> connLink.destroy());
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "channelInactive", "Running logic in Netty event loop because found null executor for channel: " + ctx.channel());
                }
                connLink.destroy();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelInactive", "could not find a SIP channel");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "exceptionCaught. " + cause.getClass());
        }
        emptyMessageQueue();
        Attribute<SipTcpInboundConnLink> attr = ctx.channel().attr(attrKey);
        if (cause instanceof Exception) {
            SipTcpInboundConnLink connLink = attr.get();
            if (connLink != null) {
                ExecutorService executor = GenericEndpointImpl.getExecutorService();
                if(executor != null) {
                    executor.execute(() -> connLink.destroy((Exception) cause));
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "exceptionCaught", "Running logic in Netty event loop because found null executor for channel: " + ctx.channel());
                    }
                    connLink.destroy((Exception) cause);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "exceptionCaught", "could not find a SIP channel");
                }
            }
        }
        ctx.close();
    }

    private void emptyMessageQueue() {
        SipMessageByteBuffer msg;
        while ((msg = messageQueue.poll()) != null) {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    private void pauseReading(ChannelHandlerContext context) {
        ChannelConfig config = context.channel().config();
        if (config.isAutoRead()) {
            config.setAutoRead(false);
        }
    }

    private void resumeReading(ChannelHandlerContext context) {
        ChannelConfig config = context.channel().config();
        if (!config.isAutoRead()) {
            config.setAutoRead(true);
        }
    }

}
