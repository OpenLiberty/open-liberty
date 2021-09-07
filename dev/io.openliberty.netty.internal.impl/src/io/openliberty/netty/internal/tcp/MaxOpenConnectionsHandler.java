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

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.openliberty.netty.internal.ConfigConstants;

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
                String channelName = ctx.channel().attr(ConfigConstants.NameKey).get();
                Tr.warning(tc, TCPMessageConstants.MAX_CONNS_EXCEEDED, channelName, maxConnections);
                lastConnExceededTime = currentTime;
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        connections.decrementAndGet();
    }
}