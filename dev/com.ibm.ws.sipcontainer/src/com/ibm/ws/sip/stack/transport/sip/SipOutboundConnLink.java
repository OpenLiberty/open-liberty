/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip;

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.util.AddressUtils;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContextFactory;

/**
 * base class for outbound connections of any transport type
 * 
 * @author ran
 */
public abstract class SipOutboundConnLink extends SipConnLink
	implements OutboundConnectionLink
{
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipOutboundConnLink.class);
	
	/** instance that is currently trying to create a virtual connection */
	private static SipOutboundConnLink s_current = null;
	
	/** timeout in milliseconds for creating outbound connections */
	private static int s_connectTimeout = SIPTransactionStack.instance().getConfiguration().getConnectTimeout();
	
	/** name of outbound chain */
	private final String m_chainName;

	/**
	 * constructor for outbound connections
	 * @param peerHost remote host address in dotted form
	 * @param peerPort remote port number
	 * @param channel channel that created this connection
	 */
	public SipOutboundConnLink(String peerHost, int peerPort, SipInboundChannel channel) {
		super(peerHost, peerPort, channel);
		m_chainName = channel.getOutboundChainName();
	}
	
	/**
	 * returns the transport-specific factory for creating outbound connections
	 */
	private VirtualConnectionFactory getOutboundVCFactory() throws ChannelException, ChainException {
		ChannelFramework cf = ChannelFrameworkFactory.getChannelFramework();
		return cf.getOutboundVCFactory(m_chainName);
	}
	
	/**
	 * creates a connection request
	 * @param host target address
	 * @param port target port number
	 * @param timeout timeout in milliseconds for creating the outbound connection
	 * @return a new connection request
	 */ 
	private Object createConnectRequestContext(String host, int port, int timeout) {
		// if the local listening point is a specific IP address,
		// create the connection from that address.
		// otherwise, create the connection from any local address.
		SIPListenningConnection listeningConnection = getSIPListenningConnection();
		String localBindAddress = listeningConnection.getListeningPoint().getHost();
		if (!AddressUtils.isIpAddress(localBindAddress)) {
			localBindAddress = "0.0.0.0";
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"createConnectRequestContext",
				"Connecting from [" + localBindAddress
				+ "] to [" + host + ':' + port + ']');
		}
		TCPConnectRequestContext context =
			TCPConnectRequestContextFactory.getRef().createTCPConnectRequestContext(
				localBindAddress, 0, // local address and port
				host, port, // remote address and port
				timeout);
		return context;
	}
	
	// --------------------------
	// SipConnLink implementation
	// --------------------------
	
	/**
	 * called by the channel framework when a new connection is established
	 */
	public void ready(VirtualConnection vc) {
		connectionEstablished();
	}
	
	// -------------------------------------
	// OutboundConnectionLink implementation
	// -------------------------------------

	/**
	 */
	public void connect(Object address) throws Exception {
		// copied from OutboundProtocolLink
		((OutboundConnectionLink)getDeviceLink()).connect(address);
		ready(getVirtualConnection());
	}

	/**
	 */
	public void connectAsynch(Object address) {
		// copied from OutboundProtocolLink
		((OutboundConnectionLink)getDeviceLink()).connectAsynch(address);
	}
	
	// ----------------------------
	// SIPConnection implementation
	// ----------------------------
	
	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#connect()
	 */
	public void connect() throws IOException {
		try {
			VirtualConnectionFactory factory = getOutboundVCFactory();
			VirtualConnection vc;
			synchronized (SipOutboundConnLink.class) {
				s_current = this;
				vc = factory.createConnection();
				// now s_current is back to null
			}
			setVirtualConnection(vc);
			
			if (!(vc instanceof OutboundVirtualConnection)) {
				throw new IllegalStateException("Not an OutboundVirtualConnection");
			}
			OutboundVirtualConnection outboundConnection = (OutboundVirtualConnection)vc;
			
			String host = getRemoteHost();
			int port = getRemotePort();
			// set the timeout to -1 if "connectTimeout" configured to 0, because for TCPChannel -1 means no timeout 
			if (s_connectTimeout == 0) {
				s_connectTimeout = -1;
			}
			Object connectRequestContext = createConnectRequestContext(host, port, s_connectTimeout);
			outboundConnection.connectAsynch(connectRequestContext, this);
		}
		catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc,"connect", "Exception", e);
			}
			close();
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * called by the channel while the channel factory instantiates
	 * the new OutboundVirtualConnection object
	 * @return the conn link instance associated with this connection
	 * @see SipOutboundChannel#getConnectionLink(VirtualConnection)
	 */
	static SipOutboundConnLink getPendingConnection() {
		SipOutboundConnLink current = s_current;
		s_current = null;
		return current;
	}
}
