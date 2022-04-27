/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import java.net.InetAddress;

import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

import jain.protocol.ip.sip.ListeningPoint;

/**
 * inbound channel listening on tls
 * 
 */
public class SipTlsInboundChannel extends SipInboundChannel
{
	
	public static final String SipTlsInboundChannelName = "SipTlsInboundChannel";
	
	/**
	 * constructor
	 */
	public SipTlsInboundChannel(ListeningPoint lp, String outboundChainName) {
		super(lp, outboundChainName);
	}

	// --------------------------------------
	// SIPListenningConnection implementation
	// --------------------------------------

	/**
	 * creates a new outbound connection given this inbound channel
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection#createConnection(java.net.InetAddress, int)
	 */
	public SIPConnection createConnection(InetAddress remoteAddress, int remotePort) throws IOException {
		String remoteHost = SIPStackUtil.getHostAddress(remoteAddress);
		return new SipTlsOutboundConnLink(remoteHost, remotePort, this, null);
	}

}
