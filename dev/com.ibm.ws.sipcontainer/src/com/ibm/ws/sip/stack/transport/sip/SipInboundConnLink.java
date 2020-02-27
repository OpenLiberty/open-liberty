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
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.dispatch.Dispatcher;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

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
	
	/** true if initialzed, false if not yet */
	private boolean m_initialized;
	
	/**
	 * constructor
	 * @param channel channel that created this connection
	 */
	public SipInboundConnLink(SipInboundChannel channel) {
		super(channel);
		m_initialized = false;
	}
	
	/**
	 * called once per conn link
	 * @param vc the virtual connection data
	 */
	protected void init(VirtualConnection vc) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc,"init", "vc [" + vc + ']');
        }
		setVirtualConnection(vc);
		
		// set the remote address
		TCPConnectionContext connectionContext = (TCPConnectionContext)getDeviceLink().getChannelAccessor();
		InetAddress remoteAddress = connectionContext.getRemoteAddress();
		int remotePort = connectionContext.getRemotePort();
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
	 * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#ready(com.ibm.wsspi.channelfw.framework.VirtualConnection)
	 */
	public void ready(VirtualConnection vc) {
		if (!m_initialized) {
			synchronized (this) {
				if (!m_initialized) {
					m_initialized = true;
					init(vc);
				}
			}
		}
	}

	/**
	 * called by the channel framework when new data arrives
	 * @see com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback#complete(com.ibm.wsspi.channelfw.framework.VirtualConnection, com.ibm.wsspi.tcpchannel.TCPReadRequestContext)
	 */
	public void complete(VirtualConnection vc, TCPReadRequestContext readCtx) {
		if (!m_initialized) {
			synchronized (this) {
				if (!m_initialized) {
					m_initialized = true;
					init(vc);
				}
			}
		}
		super.complete(vc, readCtx);
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
