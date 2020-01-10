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

import jain.protocol.ip.sip.ListeningPoint;

import java.io.IOException;
import java.net.InetAddress;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
//TODO Liberty import com.ibm.ws.management.AdminHelper;

/**
 * inbound channel listening on tls
 * 
 * @author ran
 */
public class SipTlsInboundChannel extends SipInboundChannel
{
	
	public static final String SipTlsInboundChannelName = "SipTlsInboundChannel";
	
	/**
	 * constructor
	 */
	public SipTlsInboundChannel(ChannelData config, ListeningPoint lp,
		String outboundChainName)
	{
		super(config, lp, outboundChainName,TCPConnectionContext.class);
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw.framework.VirtualConnection)
	 */
	public ConnectionLink getConnectionLink(VirtualConnection vc) {
		return new SipTlsInboundConnLink(this); // random - match to Liberty
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
		return new SipTlsOutboundConnLink(remoteHost, remotePort, this);
	}

}
