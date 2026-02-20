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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transport.GenericEndpointImpl;
import com.ibm.ws.sip.stack.transport.sip.netty.*;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import jain.protocol.ip.sip.ListeningPoint;

import io.openliberty.netty.internal.exception.NettyException;

public class SipDatagramHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    private static final TraceComponent tc = Tr.register(SipDatagramHandler.class);

    final private AttributeKey<SipUdpConnLink> attrKey = AttributeKey.valueOf("SipUdpConnLink");

    private boolean processingMessage = false;
    private static final int DEFAULT_MAX_QUEUE = 50;
    private final LinkedBlockingQueue<SipMessageEvent> messageQueue;

    public SipDatagramHandler() {
        this.messageQueue = new LinkedBlockingQueue<SipMessageEvent>(DEFAULT_MAX_QUEUE);
    }

    /**
     * Called when a new connection is established
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelActive. " + ctx.channel());
        }

        SipUdpInboundChannel inboundChannel = null;
        ListeningPoint lp = SipHandlerUtils.getListeningPoint(ctx.channel().localAddress(), ListeningPoint.TRANSPORT_UDP);

        if (lp != null) {
            inboundChannel = (SipUdpInboundChannel) SIPConnectionFactoryImplWs.instance().getInboundChannels()
                    .get(lp);
        }

        if (inboundChannel != null) {
            inboundChannel.setChannel(ctx.channel());
            Attribute<SipUdpConnLink> attr = ctx.channel().attr(attrKey);
            attr.setIfAbsent((SipUdpConnLink) inboundChannel.getConnectionLink());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelActive",
                        "could not associate an incoming connection with a SIP channel");
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SipMessageEvent msg) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelRead0. " + ctx.channel() + ". [" + msg.getSipMsg().getMarkedBytesNumber()
                    + "] bytes received");
        }
        Attribute<SipUdpConnLink> attr = ctx.channel().attr(attrKey);
        SipUdpConnLink connLink = attr.get();
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
    private void processNextMessage(final ChannelHandlerContext ctx, final SipUdpConnLink connLink) {
        SipMessageEvent msg = messageQueue.poll();
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
                connLink.complete(msg.getSipMsg(), msg.getRemoteAddress());
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
        Attribute<SipUdpConnLink> attr = ctx.channel().attr(attrKey);
        SipUdpConnLink connLink = attr.get();
        // clean up from connections table
        if (connLink != null) {
            ExecutorService executor = GenericEndpointImpl.getExecutorService();
            if(executor != null) {
                executor.execute(() -> connLink.close());
            }
            else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "channelInactive", "Running logic in Netty event loop because found null executor for channel: " + ctx.channel());
                }
                connLink.close();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelInactive", ctx.name() + "could not find a SIP channel");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "exceptionCaught. " + cause.getClass());
        }
        emptyMessageQueue();
        Attribute<SipUdpConnLink> attr = ctx.channel().attr(attrKey);
        if (cause instanceof Exception) {
            SipUdpConnLink connLink = attr.get();
            if (connLink != null) {
                ExecutorService executor = GenericEndpointImpl.getExecutorService();
                if(executor != null) {
                    executor.execute(() -> connLink.close(cause));
                }
                else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "exceptionCaught", "Running logic in Netty event loop because found null executor for channel: " + ctx.channel());
                    }
                    connLink.close(cause);
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
        SipMessageEvent msg;
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

