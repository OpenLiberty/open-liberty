/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.netty.jfapchannel;

import java.util.Map;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget;
import com.ibm.ws.sib.jfapchannel.impl.OutboundConnection;
import com.ibm.ws.sib.utils.ras.SibTr;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkConnection. Based
 * on the CFWNetworkConnection class. Basically wraps the Channel code in the Netty
 * framework mimicking the Channel Framework implementation as close as possible.
 *
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection
 *
 */
public class NettyNetworkConnection implements NetworkConnection{

	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyNetworkConnection.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/netty/jfapchannel/NettyNetworkConnection.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
	}

	/** The virtual connection */

	private Channel chan;

	private String chainName;
	
	private boolean isInbound;
	
	private Map<String, Object> sslOptions;
	
	private NettyTlsProvider tlsProvider;

	private NettyFramework nettyBundle;
	
	private BootstrapExtended bootstrap;



	/**
	 * @param bootstrap The Netty bootstrap object to create a channel from
	 * @param chainName The chain name to which the channel belongs to
	 * @throws FrameworkException 
	 */
	public NettyNetworkConnection(BootstrapExtended bootstrap, String chainName, NettyFramework nettyBundle, Map<String, Object> sslOptions, NettyTlsProvider tlsProvider, boolean isInbound) throws FrameworkException
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>" ,new Object[] {bootstrap, chainName});

//		init(bootstrap);
		this.chainName = chainName;
		this.sslOptions = sslOptions;
		this.isInbound = isInbound;
		this.tlsProvider = tlsProvider;
		this.nettyBundle = nettyBundle;
		// TODO Check if we need to clone this
		this.bootstrap = bootstrap;

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>", new Object[] {bootstrap, chainName});
	}

	public NettyNetworkConnection(Channel chan, String chainName, boolean isInbound)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", chan);

		this.chan = chan;
		this.isInbound = isInbound;
		this.chainName = chainName;

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "<init>", new Object[] {chan});
	}

	public void linkOutboundConnection(OutboundConnection conn) throws NettyException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "linkOutboundConnection");
		if(this.chan == null) {
			throw new NettyException("Error linking connection appropriately");
		}
		if(chan.attr(NettyJMSClientHandler.CONNECTION_KEY).get() != null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "linkOutboundConnection","Connection was already set: "+chan.attr(NettyJMSClientHandler.CONNECTION_KEY).get());
		}
		chan.attr(NettyJMSClientHandler.CONNECTION_KEY).set(conn);
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "linkOutboundConnection");
	}


	/**
	 * @return Returns the channel connection.
	 */
	Channel getVirtualConnection()
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getVirtualConnection");
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getVirtualConnection", chan);
		return this.chan;
	}

	/**
	 *
	 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#requestPermissionToClose(long)
	 */
	public boolean requestPermissionToClose(long timeout)
	{
		// TODO Figure out the netty equivalent for this. Only used in connection
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "requestPermissionToClose", Long.valueOf(timeout));
		//	      boolean canProcess = vc.requestPermissionToClose(timeout);
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "requestPermissionToClose", Boolean.valueOf(true));
		return true;
	}

	/**
	 * Sets the timeout to the values passed
	 * @param timeout in seconds
	 * @throws NettyException 
	 */
	public void setHearbeatInterval(int timeout) throws NettyException {
		if(chan == null) {
			throw new NettyException("Haven't registered channel to set timeout");
		}
		ChannelPipeline pipeline = this.chan.pipeline();
		if(getHearbeatInterval() != timeout * 1000)
			pipeline.replace(
					NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, 
					NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, 
					new JMSHeartbeatHandler(timeout));
	}


	private long getHearbeatInterval() throws NettyException {
		if(chan == null) {
			throw new NettyException("Haven't registered channel to get timeout");
		}
		ChannelPipeline pipeline = this.chan.pipeline();
		return ((JMSHeartbeatHandler)pipeline.get(NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY)).getReaderIdleTimeInMillis();

	}

	protected SSLSession getSSLSession() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getSSLSession", new Object[] {chan});
		SSLSession session = null;
		if(chan == null) {
			SibTr.warning(tc, "getSSLSession: Tried to get SSLSession without registering channel.", new Object[] {this.chan});
		}else {
			ChannelHandler handler = this.chan.pipeline().get(NettyNetworkConnectionFactory.SSL_HANDLER_KEY);
			if(handler == null || !(handler instanceof SslHandler)) {
				if (tc.isDebugEnabled())
					SibTr.debug(tc, "getSSLSession: No SSL session found for channel.", new Object[] {this.chan});
			}else {
				session = ((SslHandler)handler).engine().getSession();
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getSSLSession", session);
		return session;
	}


	/**
	 *
	 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#connectAsynch(com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionTarget, com.ibm.ws.sib.jfapchannel.framework.ConnectRequestListener)
	 */
	@SuppressWarnings("unchecked")
	public void connectAsynch(final NetworkConnectionTarget target, final ConnectRequestListener listener)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "connectAsynch", new Object[]{target, listener});

		final NettyNetworkConnection readyConnection = this;



		if(this.isInbound) {
			listener.connectRequestFailedNotification(new NettyException("Can't start outbound connection with an inbound channel"));
			return;
		}

		try {
			
			if(this.sslOptions != null) {
				if (tc.isDebugEnabled())
					SibTr.debug(this, tc, "initChannel","Adding SSL Support");
				String host = target.getRemoteAddress().getAddress().getHostAddress();
				String port = Integer.toString(target.getRemoteAddress().getPort());
				if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Create SSL", new Object[] {this.tlsProvider, host, port, this.sslOptions});
				SslContext context = this.tlsProvider.getOutboundSSLContext(this.sslOptions, host, port);
				if(context == null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initChannel","Error adding TLS Support");
					listener.connectRequestFailedNotification(new NettyException("Problems creating SSL context"));
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initChannel");
					return;
				}
				bootstrap.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						final ChannelPipeline pipeline = ch.pipeline();
						SSLEngine sslEngine = context.newEngine(ch.alloc());
						pipeline.addLast(NettyNetworkConnectionFactory.SSL_HANDLER_KEY, new SslHandler(sslEngine, false));
						pipeline.addLast(NettyNetworkConnectionFactory.DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
						pipeline.addLast(NettyNetworkConnectionFactory.ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
						pipeline.addLast(NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, new JMSHeartbeatHandler(0));
						pipeline.addLast(NettyNetworkConnectionFactory.JMS_CLIENT_HANDLER_KEY, new NettyJMSClientHandler());
					}
				});
				
			}else {
				if (tc.isDebugEnabled())
					SibTr.debug(this, tc, "initChannel","Constructing pipeline");
				bootstrap.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						final ChannelPipeline pipeline = ch.pipeline();
						pipeline.addLast(NettyNetworkConnectionFactory.DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
						pipeline.addLast(NettyNetworkConnectionFactory.ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
						pipeline.addLast(NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, new JMSHeartbeatHandler(0));
						pipeline.addLast(NettyNetworkConnectionFactory.JMS_CLIENT_HANDLER_KEY, new NettyJMSClientHandler());
					}
				});
			}
			
			NettyNetworkConnection parent = this;
			
			
			if(NettyNetworkConnectionFactory.USE_BUNDLE) {
				nettyBundle.startOutbound(this.bootstrap, target.getRemoteAddress().getAddress().getHostAddress(), target.getRemoteAddress().getPort(), f -> {
					if (f.isCancelled() || !f.isSuccess()) {
						SibTr.debug(this, tc, "Channel exception during connect: " + f.cause().getMessage());
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "destroy", (Exception) f.cause());
						listener.connectRequestFailedNotification((Exception) f.cause());
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "destroy");
					}else {
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "ready", f);
						parent.chan = f.channel();
						listener.connectRequestSucceededNotification(readyConnection);
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "ready");
					}

				});
			}else {
				ChannelFuture oFuture = bootstrap.connect(target.getRemoteAddress());
				oFuture.addListener(new ChannelFutureListener() {
					
					@Override
					public void operationComplete(ChannelFuture f) throws Exception {
						if (f.isCancelled() || !f.isSuccess()) {
							SibTr.debug(this, tc, "Channel exception during connect: " + f.cause().getMessage());
							if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "destroy", (Exception) f.cause());
							listener.connectRequestFailedNotification((Exception) f.cause());
							if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "destroy");
						}else {
							if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "ready", f);
							parent.chan = f.channel();
							listener.connectRequestSucceededNotification(readyConnection);
							if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "ready");
						}
					}
				});
			}
			
			


		} catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "destroy", e);
			listener.connectRequestFailedNotification(e);
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "destroy");

		}


		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "connectAsynch");
	}

	/**
	 *
	 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnection#getNetworkConnectionContext()
	 */
	public NetworkConnectionContext getNetworkConnectionContext()
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getNetworkConnectionContext");

		NetworkConnectionContext context = new NettyNetworkConnectionContext(this);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getNetworkConnectionContext", context);
		return context;
	}

	public String getChainName() {
		return chainName;
	}

	public boolean isInbound() {
		return isInbound;
	}

}
