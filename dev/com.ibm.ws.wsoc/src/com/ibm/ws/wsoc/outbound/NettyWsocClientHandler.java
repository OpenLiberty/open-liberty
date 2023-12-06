/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.outbound;

import java.sql.Connection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

/**
 * Main handler class for managing connections for each WSOC client.
 * There is a 1-1 mapping between each object of this class and single OutboundConnection.
 * On each read the readCompleted method of the read listener inside the OutboundConnection
 * is called.
 */
public class NettyWsocClientHandler extends SimpleChannelInboundHandler<WsByteBuffer> {

    /** Trace */
    private static final TraceComponent tc = Tr.register(NettyWsocClientHandler.class);

    protected final static AttributeKey<String> CHAIN_ATTR_KEY = AttributeKey.valueOf("CHAIN_NAME");
    public final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("CONNECTION");

    /** Called when a new connection is established */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "channelActive: connected for chain " + ctx.channel().attr(CHAIN_ATTR_KEY).get(), ctx.channel());

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "channelActive", ctx.channel());

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WsByteBuffer msg) throws Exception {

        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "channelRead0", ctx.channel());

        HttpRequestor connection = null;
        // LLA TODO
        //Attribute<HttpRequestor> attr = ctx.channel().attr(HttpRequestorWsoc10FactoryImpl.CONNECTION);
        //HttpRequestor connection = attr.get();

        if (connection != null) {
            if (tc.isEntryEnabled())
                Tr.entry(this, tc, "connection is not null");
            //NettyWsocReadCallback callback = connection.getReadCompletedCallback();
            //NettyWsocReadRequestContext readCtx = connection.getReadRequestContext();
            //IOReadCompletedCallback callback = connection.getReadCompletedCallback();
            //IOReadRequestContext readCtx = connection.getReadRequestContext();
            //NetworkConnection networkConnection = connection.getNetworkConnection();
            //if (callback instanceof NettyWsocReadCallback &&
            //    readCtx instanceof NettyIOReadRequestContext &&
            //    networkConnection instanceof NettyNetworkConnection) {
            //    callback.readCompleted(msg, readCtx, (NettyNetworkConnection) networkConnection);
            //} else {
            //    if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            //        Tr.warning(tc, "channelRead0: Something's wrong. Callback, network connection, or read context is not netty specific. This shouldn't happen.",
            //                   new Object[] { connection,
            //                                  callback, readCtx, networkConnection });
            //    }
            //    exceptionCaught(ctx, new NettyException("Illegal callback type for channel."));
            //    return;
            //}

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.warning(tc, "channelRead0: could not associate an incoming message with a Connection. Message will be ignored and channel will be closed.",
                           new Object[] { ctx.channel() });
                ctx.close();
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "channelRead0", ctx.channel());

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "channelInactive", ctx.channel());

        // TODO: Check how to manage inactive channels appropriately
        //Connection connection = ctx.channel().attr(NettyNetworkConnectionFactory.CONNECTION).get();

        //if (tc.isEntryEnabled() && tc.isDebugEnabled()) {
        //    Tr.debug(this, tc, "Fired for connection which is closed?: " + connection.isClosed() + " closedDeffered?: " + connection.isCloseDeferred(),
        //             new Object[] { ctx.channel(),
        //                            connection });
        // }
        //ctx.channel().attr(NettyNetworkConnectionFactory.CONNECTION).set(null);

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "channelInactive", ctx.channel());

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(this, tc, "exceptionCaught", new Object[] { ctx.channel(), cause });

        //Attribute<Connection> attr = ctx.channel().attr(NettyNetworkConnectionFactory.CONNECTION);
        //Connection connection = attr.get();

        //if (connection == null) {
        //    if (tc.isEntryEnabled() && tc.isDebugEnabled())
        //        Tr.debug(this, tc, "Found null connection for JMS Channel. Closing channel and moving cause through pipeline.", ctx.channel());
        //    ctx.close();
        //} else {
        //    connection.invalidate(false, cause, "Connection closed due to exception");
        //    ctx.channel().attr(NettyNetworkConnectionFactory.CONNECTION).set(null);
        //    ctx.close();
        //}

        super.exceptionCaught(ctx, cause);

        if (tc.isEntryEnabled())
            Tr.exit(this, tc, "exceptionCaught", ctx.channel());

    }

}
