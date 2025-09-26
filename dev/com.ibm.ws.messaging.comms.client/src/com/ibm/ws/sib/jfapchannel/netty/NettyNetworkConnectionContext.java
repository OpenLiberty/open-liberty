/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLSession;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.jfapchannel.ConversationMetaData;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnection;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.netty.channel.Channel;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext. It
 * basically wraps the NettyNetworkConnection code making use of the underlying Channel object.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext
 * 
 */
public class NettyNetworkConnectionContext implements NetworkConnectionContext{

	/** Trace */
	private static final TraceComponent tc = SibTr.register(NettyNetworkConnectionContext.class,
			JFapChannelConstants.MSG_GROUP,
			JFapChannelConstants.MSG_BUNDLE);

	/** Log class info on load */
	static
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			SibTr.debug(tc,
					"@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/netty/jfapchannel/NettyNetworkConnectionContext.java, SIB.comms, WASX.SIB, uu1215.01 1.2");
	}

	/** The underlying connection link */
	private ConversationMetaData metaData;

	/** The connection reference */
	private NettyNetworkConnection conn = null;


	/**
	 * @param connLink
	 */
	public NettyNetworkConnectionContext(NettyNetworkConnection conn)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(this, tc, "<init>", new Object[] { conn });
		this.conn = conn;
		this.metaData = new ConversationMetaData() {

			@Override
			public boolean isInbound() {
				return conn.isInbound();
			}

			@Override
			public SSLSession getSSLSession() {
				return conn.getSSLSession();
			}

			@Override
			public int getRemotePort() {
				return ((InetSocketAddress)conn.getVirtualConnection().remoteAddress()).getPort();
			}

			@Override
			public InetAddress getRemoteAddress() {
				return ((InetSocketAddress)conn.getVirtualConnection().remoteAddress()).getAddress();
			}

			@Override
			public String getChainName() {
				return conn.getChainName();
			}
		};
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "<init>", new Object[] { this.conn , this.metaData});
	}

	/**
	 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#close(com.ibm.ws.sib.jfapchannel.framework.NetworkConnection, java.lang.Throwable)
	 */
	@Override
	public void close(NetworkConnection networkConnection, Throwable throwable)
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(this, tc, "close", new Object[] { networkConnection, throwable });

		// TODO: This needs to be verified and implemented is not. https://github.com/OpenLiberty/open-liberty/issues/24814
		// If the server is stopping, all connections will be closed/flushed by netty bundle? Verify
		if (FrameworkState.isStopping()) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
				SibTr.exit(this, tc, "close");
			return;
		}
		Exception exception = null;
		if (throwable instanceof Exception)
		{
			exception = (Exception) throwable;
		}
		else
		{
			exception = new Exception(throwable);
		}
		Channel chan = conn.getVirtualConnection();
		if(chan != null) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				SibTr.debug(this, tc, "close: Found Netty Channel to close: ", new Object[] {chan, chan.isActive(), chan.isOpen()});
			chan.close();
		}else {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
				SibTr.debug(this, tc, "close", "Found NULL Netty Channel to close");
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(this, tc, "close", new Object[] { networkConnection, throwable });
	}

	/**
	 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#getIOContextForDevice()
	 */
	@Override
	public IOConnectionContext getIOContextForDevice()
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(this, tc, "getIOContextForDevice");

		final IOConnectionContext ioConnCtx = new NettyIOConnectionContext(conn);


		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(this, tc, "getIOContextForDevice", ioConnCtx);
		return ioConnCtx;
	}

	/**
	 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionContext#getMetaData()
	 */
	@Override
	public ConversationMetaData getMetaData()
	{
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(this, tc, "getMetaData");

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(this, tc, "getMetaData", metaData);
		return metaData;
	}


}
