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

import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

import jain.protocol.ip.sip.ListeningPoint;

/**
 * inbound channel listening on tcp
 * 
 */
public class SipTcpInboundChannel extends SipInboundChannel {
	/**
	 * constructor
	 */
	public SipTcpInboundChannel(ListeningPoint lp, String outboundChainName) {
		super(lp, outboundChainName);
	}

	// --------------------------------------
	// SIPListenningConnection implementation
	// --------------------------------------

	/**
	 * creates a new outbound connection given this inbound channel
	 * 
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection#createConnection(java.net.InetAddress,
	 *      int)
	 */
	public SIPConnection createConnection(InetAddress remoteAddress, int remotePort) throws IOException {
		String remoteHost = SIPStackUtil.getHostAddress(remoteAddress);
		return new SipTcpOutboundConnLink(remoteHost, remotePort, this, null);
	}
}
