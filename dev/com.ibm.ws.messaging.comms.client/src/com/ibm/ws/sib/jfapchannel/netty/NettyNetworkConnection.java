/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.netty;

import java.util.HashMap;
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
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;
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
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/netty/jfapchannel/NettyNetworkConnection.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
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
	public NettyNetworkConnection(BootstrapExtended bootstrap, String chainName, NettyFramework nettyBundle, Map<Object, Object> sslOptions, NettyTlsProvider tlsProvider, boolean isInbound) throws FrameworkException
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>" ,new Object[] {bootstrap, chainName});

		this.chainName = chainName;
		// TODO: Check if this is the best way to do this for SSL options https://github.com/OpenLiberty/open-liberty/issues/24813
		this.sslOptions = sslOptions == null ? null : new HashMap<String, Object>((Map)sslOptions);
		this.isInbound = isInbound;
		this.tlsProvider = tlsProvider;
		this.nettyBundle = nettyBundle;
		// TODO Check if we need to clone this https://github.com/OpenLiberty/open-liberty/issues/24813
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
		if(chan.attr(NettyNetworkConnectionFactory.CONNECTION).get() != null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "linkOutboundConnection","Connection was already set: "+chan.attr(NettyNetworkConnectionFactory.CONNECTION).get());
		}
		chan.attr(NettyNetworkConnectionFactory.CONNECTION).set(conn);
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
		// TODO Figure out the netty equivalent for this. Only used in connection see https://github.com/OpenLiberty/open-liberty/issues/24812
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
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this.chan, tc, "setHearbeatInterval", Integer.valueOf(timeout));
		if(chan == null) {
			throw new NettyException("Haven't registered channel to set timeout");
		}
		ChannelPipeline pipeline = this.chan.pipeline();
		if(getHearbeatInterval() != timeout * 1000) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				SibTr.debug(this, tc, "setHearbeatInterval", "Replacing Heartbeat Interval for Channel: "+this.chan+" from: " + getHearbeatInterval() + " to: "+timeout);
			pipeline.replace(
					NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, 
					NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, 
					new NettyJMSHeartbeatHandler(timeout));
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				SibTr.debug(this, tc, "setHearbeatInterval", "Heartbeat Interval set for Channel: "+this.chan+" pipeline names" + pipeline.names());
			
		}
			
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setHearbeatInterval");
	}


	private long getHearbeatInterval() throws NettyException {
		if(chan == null) {
			throw new NettyException("Haven't registered channel to get timeout");
		}
		ChannelPipeline pipeline = this.chan.pipeline();
		return ((NettyJMSHeartbeatHandler)pipeline.get(NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY)).getReaderIdleTimeInMillis();

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

		if (FrameworkState.isStopping()) {
			// Drive the callback directly here.
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
				SibTr.debug(this, tc, "Framework.isStopping() == true");
			listener.connectRequestFailedNotification(new IllegalStateException("Framework stopped"));

		}
		else {
			try {

				bootstrap.handler(new NettyJMSClientInitializer(bootstrap.getBaseInitializer(), target, listener));

				NettyNetworkConnection parent = this;

				nettyBundle.startOutbound(this.bootstrap, target.getRemoteAddress().getAddress().getHostAddress(), target.getRemoteAddress().getPort(), f -> {
					if (f.isCancelled() || !f.isSuccess()) {
						SibTr.debug(this, tc, "Channel exception during connect: " + f.cause().getMessage());
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "destroy", f.cause());
						listener.connectRequestFailedNotification((Exception) f.cause());
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "destroy");
					}else {
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(parent, tc, "ready", f);
						parent.chan = f.channel();
						listener.connectRequestSucceededNotification(readyConnection);
						if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(parent, tc, "ready");
					}

				});

			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "destroy", e);
				listener.connectRequestFailedNotification(e);
				if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "destroy");

			}
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
	
	/**
	 * ChannelInitializer for JMS Client over TCP, and optionally TLS with Netty
	 */
	private class NettyJMSClientInitializer extends ChannelInitializerWrapper {
		final ChannelInitializerWrapper parent;
		final NetworkConnectionTarget target;
		final ConnectRequestListener listener;

		public NettyJMSClientInitializer(ChannelInitializerWrapper parent, NetworkConnectionTarget target, ConnectRequestListener listener) {
			this.parent = parent;
			this.target = target;
			this.listener = listener;
		}

		@Override
		protected void initChannel(Channel ch) throws Exception {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				SibTr.debug(this, tc, "initChannel","Constructing pipeline");
			parent.init(ch);
			ChannelPipeline pipeline = ch.pipeline();
			if(sslOptions != null) {
				if (tc.isDebugEnabled())
					SibTr.debug(ch, tc, "initChannel","Adding SSL Support");
				String host = target.getRemoteAddress().getAddress().getHostAddress();
				String port = Integer.toString(target.getRemoteAddress().getPort());
				if (tc.isDebugEnabled()) SibTr.debug(this, tc, "Create SSL", new Object[] {tlsProvider, host, port, sslOptions});
				SslHandler handler = tlsProvider.getOutboundSSLContext(sslOptions, host, port, ch);
				if(handler == null) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "initChannel","Error adding TLS Support");
					listener.connectRequestFailedNotification(new NettyException("Problems creating SSL context"));
					if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "initChannel");
					ch.close();
					return;
				}
				pipeline.addFirst(NettyNetworkConnectionFactory.SSL_HANDLER_KEY, handler);
			}
			pipeline.addLast(NettyNetworkConnectionFactory.DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
			pipeline.addLast(NettyNetworkConnectionFactory.ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
			pipeline.replace(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME, NettyNetworkConnectionFactory.HEARTBEAT_HANDLER_KEY, new NettyJMSHeartbeatHandler(0));
			pipeline.addLast(NettyNetworkConnectionFactory.JMS_CLIENT_HANDLER_KEY, new NettyJMSClientHandler());
		}
	}

}
