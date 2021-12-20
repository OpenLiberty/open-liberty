/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLEngine;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transport.GenericEndpointImpl;
import com.ibm.ws.sip.stack.transport.netty.SipMessageBufferStreamDecoder;
import com.ibm.ws.sip.stack.util.AddressUtils;
import com.ibm.wsspi.channelfw.VirtualConnection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.*;
import jain.protocol.ip.sip.ListeningPoint;

/**
 * base class for outbound connections of any transport type
 * 
 * @author ran
 */
public abstract class SipOutboundConnLink extends SipConnLink {
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipOutboundConnLink.class);

	/** instance that is currently trying to create a virtual connection */
	private static SipOutboundConnLink s_current = null;

	/** timeout in milliseconds for creating outbound connections */
	private static int s_connectTimeout = SIPTransactionStack.instance().getConfiguration().getConnectTimeout();

	/** name of outbound chain */
	private final String m_chainName;

	private Bootstrap bootstrap = new Bootstrap();
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

	/**
	 * constructor for outbound connections
	 * 
	 * @param peerHost remote host address in dotted form
	 * @param peerPort remote port number
	 * @param channel  channel that created this connection
	 */
	public SipOutboundConnLink(String peerHost, int peerPort, SipInboundChannel sipInboundChannel, Channel channel, boolean isSecure) {
        super(peerHost, peerPort, sipInboundChannel, channel);
        m_chainName = sipInboundChannel.getOutboundChainName();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class);
    
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, s_connectTimeout);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                if (isSecure) {
                    SslContext context = GenericEndpointImpl.getTlsProvider().getOutboundSSLContext(GenericEndpointImpl.getSslOptions(), peerHost, Integer.toString(peerPort));
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "SipOutboundConnLink", "context: " + context);
                    }
                    SSLEngine engine = context.newEngine(ch.alloc());
                    pipeline.addFirst("ssl", new SslHandler(engine, false));
                }
                pipeline.addLast("decoder", new SipMessageBufferStreamDecoder());
                pipeline.addLast("handler", new SipStreamHandler());
            }
        });
	}

	// ----------------------------
	// SIPConnection implementation
	// ----------------------------

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#connect()
	 */
	public void connect() throws IOException {

		InetSocketAddress localBindAddress = getLocalAddress();
		InetSocketAddress remoteAddress = getRemoteAddress();
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "connect", "Connecting from [" + localBindAddress + "] to [" + remoteAddress + ']');
		}
		ChannelFuture channelFuture = null;
		try {
			channelFuture = bootstrap.connect(remoteAddress, localBindAddress).sync();
		} catch (Exception e) {
			e.printStackTrace();
			close();
			throw new IOException(e);
		}

		if (channelFuture.isCancelled() || !channelFuture.isSuccess()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "connect", "connection can't be established: " + channelFuture.cause());
			}
			close();
			throw new IOException(channelFuture.cause());
		}
		synchronized (SipOutboundConnLink.class) {
			s_current = this;
		}
		// Connection established successfully
		m_channel = channelFuture.channel();
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "connect", "connection established successfully: " + remoteAddress);
		}
		connectionEstablished();
	}

	/**
	 * called by the channel while the channel factory instantiates the new
	 * OutboundVirtualConnection object
	 * 
	 * @return the conn link instance associated with this connection
	 * @see SipOutboundChannel#getConnectionLink(VirtualConnection)
	 */
	static SipOutboundConnLink getPendingConnection() {
		SipOutboundConnLink current = s_current;
		s_current = null;
		return current;
	}

	private InetSocketAddress getLocalAddress() {
		InetSocketAddress returnValue = null;

		// if the local listening point is a specific IP address,
		// create the connection from that address.
		// otherwise, create the connection from any local address.

		ListeningPoint lp = getSIPListenningConnection().getListeningPoint();
		String localBindAddress = lp.getHost();
		if (!AddressUtils.isIpAddress(localBindAddress)) {
			localBindAddress = "0.0.0.0";
		}
		
		// use an ephemeral port
		returnValue = new InetSocketAddress(localBindAddress, 0);

		return returnValue;
	}

	private InetSocketAddress getRemoteAddress() {
		InetSocketAddress returnValue = null;

		String host = getRemoteHost();
		int port = getRemotePort();
		returnValue = new InetSocketAddress(host, port);

		return returnValue;
	}

	private class SipStreamHandler extends SimpleChannelInboundHandler<SipMessageByteBuffer> {

		/** Called when a new connection is established */
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "channelActive", ctx.channel().remoteAddress() + " connected");
			}

		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, SipMessageByteBuffer msg) throws Exception {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "channelRead0",
						ctx.channel() + ". [" + msg.getMarkedBytesNumber() + "] bytes received");
			}
			complete(msg);
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "channelInactive", ctx.channel().remoteAddress() + " has been disonnected");
			}
			destroy(null);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "exceptionCaught", cause);
			}

			// TODO ANNA - should we close the connection ?
			connectionError(new Exception(cause));
			ctx.close();
		}
	}
}
