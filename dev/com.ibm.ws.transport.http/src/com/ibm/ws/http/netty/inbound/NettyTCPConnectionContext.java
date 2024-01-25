/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.inbound;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;

/**
 *
 */
public class NettyTCPConnectionContext implements TCPConnectionContext {

    private final NettyTCPReadRequestContext readContext;
    private final NettyTCPWriteRequestContext writeContext;
    private final VirtualConnection vc;
    private final ChannelHandlerContext nettyContext;
    private SSLConnectionContext sslContext;

    public NettyTCPConnectionContext(ChannelHandlerContext nettyContext, VirtualConnection vc) {

        this.vc = vc;
        this.nettyContext = nettyContext;

        this.readContext = new NettyTCPReadRequestContext(this, nettyContext);
        this.readContext.setVC(this.vc);
        this.writeContext = new NettyTCPWriteRequestContext(this, nettyContext);
        initializeSSLContext();

    }

    private void initializeSSLContext() {
        SslHandler sslHandler = nettyContext.pipeline().get(SslHandler.class);

        if (sslHandler != null) {
            this.sslContext = new NettySSLConnectionContext(nettyContext.channel(), nettyContext.channel().attr(NettyHttpConstants.IS_OUTBOUND_KEY).get());
        }
    }

    @Override
    public TCPReadRequestContext getReadInterface() {
        return readContext;
    }

    @Override
    public TCPWriteRequestContext getWriteInterface() {
        return writeContext;
    }

    @Override
    public InetAddress getRemoteAddress() {
        InetSocketAddress remoteAddress = (InetSocketAddress) nettyContext.channel().remoteAddress();

        return remoteAddress.getAddress();
    }

    @Override
    public int getRemotePort() {

        InetSocketAddress remoteAddress = (InetSocketAddress) nettyContext.channel().remoteAddress();
        return remoteAddress.getPort();
    }

    @Override
    public InetAddress getLocalAddress() {
        InetSocketAddress localAddress = (InetSocketAddress) nettyContext.channel().localAddress();

        return localAddress.getAddress();
    }

    @Override
    public int getLocalPort() {
        InetSocketAddress localAddress = (InetSocketAddress) nettyContext.channel().localAddress();
        return localAddress.getPort();
    }

    @Override
    public SSLConnectionContext getSSLContext() {
        // TODO Auto-generated method stub
        return null;
    }

}
