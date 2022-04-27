/*******************************************************************************
 * Copyright (c) 2008, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip.netty;

import io.netty.channel.Channel;
import jain.protocol.ip.sip.ListeningPoint;

/**
 * represents an outbound tcp connection
 * 
 * @author ran
 */
public class SipTcpOutboundConnLink extends SipOutboundConnLink
{
	/**
	 * constructor for outbound connections
	 * @param peerHost remote host address in dotted form
	 * @param peerPort remote port number
	 * @param sipInboundChannel channel that created this connection
	 * @param channel Netty channel associated with that SIP channel
	 */
	public SipTcpOutboundConnLink(String peerHost, int peerPort, SipInboundChannel sipInboundChannel, Channel channel) {
		super(peerHost, peerPort, sipInboundChannel, channel, false);
	}
	
	// ----------------------------
	// SIPConnection implementation
	// ----------------------------

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getTransport()
	 */
	public String getTransport() {
		return ListeningPoint.TRANSPORT_TCP;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isReliable()
	 */
	public boolean isReliable() {
		return true;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#isSecure()
	 */
	public boolean isSecure() {
		return false;
	}

	/**
	 * @see com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection#getPathMTU()
	 */
	public int getPathMTU() {
		return -1;
	}

}
