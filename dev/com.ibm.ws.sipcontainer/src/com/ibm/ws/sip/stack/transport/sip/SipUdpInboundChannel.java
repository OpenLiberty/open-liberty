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
import java.util.HashMap;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.util.SIPStackUtil;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.udpchannel.UDPContext;

//TODO Liberty import com.ibm.ws.management.AdminHelper;

/**
 * inbound channel listening on udp
 * 
 * @author ran
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
	
	/**
	 * gets channel instance given listening point
	 * @param config channel configuration
	 * @param lp listening point of the channel
	 * @param outboundChainName name of outbound chain, for creating outbound connections
	 * @return a new or existing inbound udp channel
	 */
	public static SipUdpInboundChannel instance(ChannelData config,
		ListeningPoint lp, String outboundChainName)
	{
		SipUdpInboundChannel channel = (SipUdpInboundChannel)s_instances.get(lp);
		if (channel == null) {
			channel = new SipUdpInboundChannel(config, lp, outboundChainName);
			s_instances.put(lp, channel);
		}
		return channel;
	}
	
	/**
	 * private constructor
	 */
	private SipUdpInboundChannel(ChannelData config, ListeningPoint lp,
		String outboundChainName)
	{
		super(config, lp, outboundChainName,UDPContext.class);
		m_connLink = null;
	}

	/**
	 * creates the one and only connection link of this channel, if needed
	 * @return the one and only connection link of this channel
	 */
	private UdpSender getConnectionLink() {
		if (m_connLink == null) {
			// don't let 2 threads create 2 conn links
			synchronized (this) {
				if (m_connLink == null) {
					/*TODO Liberty m_connLink = AdminHelper.getPlatformHelper().isZOS()
						? new ZosInboundConnLink(this, "udp", false, false)
						: SipUdpConLink.instance(this);*/
					m_connLink = SipUdpConnLink.instance(this);
				}
			}
		}
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc,"getConnectionLink", m_connLink.toString());
		}
		return m_connLink;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw.framework.VirtualConnection)
	 */
	public ConnectionLink getConnectionLink(VirtualConnection vc) {
		return getConnectionLink();
	}

	/**
	 * called when the connection is closed, to signal that a new connlink
	 * should be created for the next message
	 */
	void connectionClosed() {
		m_connLink = null;
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
		return new SipUdpConnection(remoteHost, remotePort, this, connLink);
	}

}
