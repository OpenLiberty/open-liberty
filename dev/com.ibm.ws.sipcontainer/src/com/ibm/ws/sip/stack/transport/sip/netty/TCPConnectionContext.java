/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.channel.Channel;

/**
 * A context object encapsulating data related to a TCPChannel.
 */
public class TCPConnectionContext {

	private InetAddress m_remoteAddress;
	private int m_remotePort;
	private InetAddress m_localAddress;
	private int m_localPort;

	public TCPConnectionContext(Channel channel) {
		m_localAddress = ((InetSocketAddress) channel.localAddress()).getAddress();
		m_localPort = ((InetSocketAddress) channel.localAddress()).getPort();
		m_remoteAddress = ((InetSocketAddress) channel.remoteAddress()).getAddress();
		m_remotePort = ((InetSocketAddress) channel.remoteAddress()).getPort();
	}

	/**
	 * Returns the remote address (on the other end of the socket) Under some
	 * circumstances (for example a failed outbound connection attempt) the value
	 * returned is the address <em>attempted</em> to have been connected to.
	 * 
	 * @return InetAddress
	 */
	public InetAddress getRemoteAddress() {
		return m_remoteAddress;
	}

	/**
	 * Returns the remote port (on the other end of the socket)
	 * 
	 * @return int
	 */
	public int getRemotePort() {
		return m_remotePort;
	}

	/**
	 * Returns the local host address of the socket
	 * 
	 * @return InetAddress
	 */
	public InetAddress getLocalAddress() {
		return m_localAddress;
	}

	/**
	 * Returns the local port number of the socket
	 * 
	 * @return int
	 */
	public int getLocalPort() {
		return m_localPort;
	}

}
