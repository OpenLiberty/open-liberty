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
import com.ibm.ws.sip.stack.transport.sip.netty.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import jain.protocol.ip.sip.ListeningPoint;

public class SipDatagramHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    private static final TraceComponent tc = Tr.register(SipDatagramHandler.class);

    final private AttributeKey<SipUdpConnLink> attrKey = AttributeKey.valueOf("SipUdpConnLink");

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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "channelInactive", ctx.channel().remoteAddress() + " has been disconnected");
        }

        Attribute<SipUdpConnLink> attr = ctx.channel().attr(attrKey);
        SipUdpConnLink connLink = attr.get();
        // clean up from connections table
        if (connLink != null) {
            connLink.close();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelInactive", ctx.name() + "could not find a SIP channel");
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
        if (connLink != null) {
            connLink.complete(msg.getSipMsg(), msg.getRemoteAddress());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "channelRead0", "could not find a SIP channel");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "exceptionCaught. " + cause.getClass());
        }
        Attribute<SipUdpConnLink> attr = ctx.channel().attr(attrKey);
        if (cause instanceof Exception) {
            SipUdpConnLink connLink = attr.get();
            if (connLink != null) {
                connLink.close(cause);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "exceptionCaught", "could not find a SIP channel");
                }
            }
        }
        ctx.close();
    }
}

