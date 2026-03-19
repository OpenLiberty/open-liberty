/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;

/**
 * Channel handler which keeps track of the overall connection count and terminates new
 * connections once the configured threshold has been reached.
 */
@Sharable
public class MaxOpenConnectionsHandler extends ChannelInboundHandlerAdapter {

    private static final TraceComponent tc = Tr.register(MaxOpenConnectionsHandler.class, TCPMessageConstants.NETTY_TRACE_NAME, TCPMessageConstants.TCP_BUNDLE);

    private final AtomicInteger connections = new AtomicInteger();
    private final int maxConnections;
    private long lastConnExceededTime = 0L;

    public MaxOpenConnectionsHandler(int maxConnectionCount) {
        maxConnections = maxConnectionCount;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int val = connections.incrementAndGet();
        if (val <= maxConnections) {
            super.channelActive(ctx);
        } else {
            ctx.close();
            // notify every 10 minutes if max concurrent conns was hit
            long currentTime = System.currentTimeMillis();
            if (currentTime > (lastConnExceededTime + 600000L)) {
                String channelName = ctx.channel().attr(ConfigConstants.NAME_KEY).get();

                // If the channelName is null check the parent for a name.
                if (channelName == null) {
                    Channel parentChannel = ctx.channel().parent();
                    if (parentChannel != null) {
                        channelName = parentChannel.attr(ConfigConstants.NAME_KEY).get();
                    }
                }

                Tr.warning(tc, TCPMessageConstants.MAX_CONNS_EXCEEDED, channelName, maxConnections);
                lastConnExceededTime = currentTime;
            }
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // If handler is removed while channel is active, we should throw an exception
        if(ctx.channel().isActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unallowed removal of handler: " + NettyConstants.MAX_OPEN_CONNECTIONS_HANDLER_NAME + " from channel: " + ctx.channel());
            }
            ctx.fireExceptionCaught(new NettyException("Removed from channel pipeline handler: " + NettyConstants.MAX_OPEN_CONNECTIONS_HANDLER_NAME));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        connections.decrementAndGet();
    }
}
