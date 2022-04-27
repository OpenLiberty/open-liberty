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
import java.util.HashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;

import io.netty.channel.Channel;
import jain.protocol.ip.sip.ListeningPoint;


/**
 * inbound channel listening on udp
 * 
 */
public class SipUdpInboundChannel extends SipInboundChannel
{
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipUdpInboundChannel.class);
	
	/**
	 * map of inbound udp channels.
	 * there is one inbound udp channel per host:port.
	 */
	private static HashMap s_instances = new HashMap();
	
	/** only one connection link per udp channel */
	private UdpSender m_connLink;
	
	
	private Channel m_channel;
	
	/**
	 * gets channel instance given listening point
	 * @param config channel configuration
	 * @param lp listening point of the channel
	 * @param outboundChainName name of outbound chain, for creating outbound connections
	 * @return a new or existing inbound udp channel
	 */
	public static SipUdpInboundChannel instance(ListeningPoint lp, String outboundChainName)
	{
		SipUdpInboundChannel channel = (SipUdpInboundChannel)s_instances.get(lp);
		if (channel == null) {
			channel = new SipUdpInboundChannel(lp, outboundChainName);
			s_instances.put(lp, channel);
		}
		return channel;
	}
	
	/**
	 * private constructor
	 */
	private SipUdpInboundChannel(ListeningPoint lp,
		String outboundChainName)
	{
		super(lp, outboundChainName);
		m_connLink = null;
	}

	/**
	 * creates the one and only connection link of this channel, if needed
	 * @return the one and only connection link of this channel
	 */
	public UdpSender getConnectionLink() {
		if (m_connLink == null) {
			// don't let 2 threads create 2 conn links
			synchronized (this) {
				if (m_connLink == null) {
					m_connLink = SipUdpConnLink.instance(this);
				}
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"getConnectionLink", m_connLink.toString());
		}
		return m_connLink;
	}

	public synchronized void setChannel(Channel channel) {
		m_channel = channel;
	}
	
	synchronized Channel getChannel() {
		return m_channel;
	}

	/**
	 * called when the connection is closed, to signal that a new connlink
	 * should be created for the next message
	 */
	void connectionClosed() {
		m_connLink = null;
		m_channel = null;
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
		UdpSender connLink = getConnectionLink();
		Channel channel = getChannel();
		return new SipUdpConnection(remoteHost, remotePort, this, connLink, channel);  
	}

}
