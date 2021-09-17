/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Channel handler for {@link AccessLists}: if the remote address for the current context is 
 * not allowed per the include/exclude lists, then it will be terminated here.
 */
@Sharable
public class AccessListHandler extends ChannelInboundHandlerAdapter {

    private static final TraceComponent tc = Tr.register(AccessListHandler.class, TCPMessageConstants.NETTY_TRACE_NAME, 
            TCPMessageConstants.TCP_BUNDLE);

    AccessLists accessLists;

    public AccessListHandler(AccessLists acl) {
        this.accessLists = acl;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // reject the connection based on the config
        InetAddress addr = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
        if (accessLists.accessDenied(addr)) {
            ctx.close();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "connection rejected due to access list configuration: " + ctx.channel());
            }
        } else {
            super.channelActive(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }
}