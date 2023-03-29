/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;

import io.netty.channel.Channel;

/**
 * base class for inbound connections of TCP or TLS.
 * on Z, this is also the base class for UDP.
 * 
 * @author ran
 */
public abstract class SipInboundConnLink extends SipConnLink
{
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipInboundConnLink.class);
	
	/** true if initialized, false if not yet */
	private boolean m_initialized;
	
	
	/**
	 * constructor
	 * @param sipInboundChannel channel that created this connection
	 * @param channel a Netty channel associated with this SIP channel
	 */
	public SipInboundConnLink(SipInboundChannel sipInboundChannel, Channel channel) {
		super(sipInboundChannel, channel);
		m_initialized = false;
	}
	

	protected void init() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
			Tr.debug(this, tc,"init"); 
		}
		
		InetAddress remoteAddress = ((InetSocketAddress) m_channel.remoteAddress()).getAddress();
		int remotePort = ((InetSocketAddress) m_channel.remoteAddress()).getPort();
		InetSocketAddress address = new InetSocketAddress(remoteAddress, remotePort);
		setRemoteAddress(address);
		
		SIPListenningConnection listener = getSIPListenningConnection();
		Dispatcher.instance().queueConnectionAcceptedEvent(listener, this);
		connectionEstablished();
	}
	
	// --------------------------
	// SipConnLink implementation
	// --------------------------

	/**
	 * called by the channel framework when a new connection is accepted
	 */
	public void ready() {
		if (!m_initialized) {
			synchronized (this) {
				if (!m_initialized) {
					m_initialized = true;
					init();
				}
			}
		}
	}


	/**
	 * called by the channel framework when new data arrives
	 */
	public void complete(SipMessageByteBuffer buff) {
		if (!m_initialized) {
			synchronized (this) {
				if (!m_initialized) {
					m_initialized = true;
					init();
				}
			}
		}
		super.complete(buff);
	}

	// ----------------------------
	// SIPConnection implementation
	// ----------------------------

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#connect()
	 */
	public void connect() throws IOException {
        throw new IOException("connect should not be called for inbound connection");
	}
}
