/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.netty;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transport.sip.netty.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import jain.protocol.ip.sip.ListeningPoint;
import java.io.IOException;

public class SipStreamHandler extends SimpleChannelInboundHandler<SipMessageByteBuffer> {

    private static final TraceComponent tc = Tr.register(SipStreamHandler.class);

    final private AttributeKey<SipTcpInboundConnLink> attrKey = AttributeKey.valueOf("SipTcpInboundConnLink");

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

        if (connLink != null) {
            connLink.complete(msg);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelRead0", "could not associate an incoming message with a SIP channel");
            }
            throw new IOException("could not associate an incoming message with a SIP channel");
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelInactive", ctx.channel().remoteAddress() + " has been disconnected");
        }

        Attribute<SipTcpInboundConnLink> attr = ctx.channel().attr(attrKey);
        SipTcpInboundConnLink connLink = attr.get();
        // clean up from connections table
        if (connLink != null) {
            connLink.destroy();
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
        Attribute<SipTcpInboundConnLink> attr = ctx.channel().attr(attrKey);
        if (cause instanceof Exception) {
            SipTcpInboundConnLink connLink = attr.get();
            if (connLink != null) {
                connLink.destroy((Exception) cause);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "exceptionCaught", "could not find a SIP channel");
                }
            }
        }
        ctx.close();
    }
}
