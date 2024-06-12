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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.Channel;
import io.openliberty.netty.internal.ChannelInitializerWrapper;

/**
 * ChannelInitializer for Wsoc Client over TCP, and optionally TLS with Netty
 */
public class NettyWsocClientInitializer extends ChannelInitializerWrapper {

    private static final TraceComponent tc = Tr.register(NettyWsocClientInitializer.class);

    public static final String SSL_HANDLER_KEY = "sslHandler";
    public static final String DECODER_HANDLER_KEY = "decoder";
    public static final String ENCODER_HANDLER_KEY = "encoder";
    public static final String WSOC_CLIENT_HANDLER_KEY = "wsocClientHandler";

    final ChannelInitializerWrapper parent;
    final Wsoc10Address target;
    //final ConnectRequestListener listener;

    public NettyWsocClientInitializer(ChannelInitializerWrapper parent, Wsoc10Address target) { //, ConnectRequestListener listener) {
        this.parent = parent;
        this.target = target;
        //this.listener = listener;
    }

    @Override
    public void initChannel(Channel ch) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "initChannel", "Constructing pipeline");
        parent.init(ch);
        ChannelPipeline pipeline = ch.pipeline();
        //if (sslOptions != null) {
        //    if (tc.isDebugEnabled())
        //        Tr.debug(ch, tc, "initChannel", "Adding SSL Support");
        //    String host = target.getRemoteAddress().getAddress().getHostAddress();
        //    String port = Integer.toString(target.getRemoteAddress().getPort());
        //    if (tc.isDebugEnabled())
        //        Tr.debug(this, tc, "Create SSL", new Object[] { tlsProvider, host, port, sslOptions });
        //    SslContext context = tlsProvider.getOutboundSSLContext(sslOptions, host, port);
        //    if (context == null) {
        //        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        //            Tr.entry(this, tc, "initChannel", "Error adding TLS Support");
        //        listener.connectRequestFailedNotification(new NettyException("Problems creating SSL context"));
        //        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        //            Tr.exit(this, tc, "initChannel");
        //        ch.close();
        //        return;
        //    }
        //    SSLEngine engine = context.newEngine(ch.alloc());
        //    pipeline.addFirst(NettyNetworkConnectionFactory.SSL_HANDLER_KEY, new SslHandler(engine, false));
        //}
        pipeline.addLast(DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
        pipeline.addLast(ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
        pipeline.addLast(WSOC_CLIENT_HANDLER_KEY, new NettyWsocClientHandler());
    }
}