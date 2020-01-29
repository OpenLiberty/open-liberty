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

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.udpchannel.UDPContext;

/**
 * an outbound udp channel
 * 
 * @author ran
 */
public class SipUdpOutboundChannel extends SipOutboundChannel
{
	public final static String SipUdpOutboundChannelName = "SipUdpOutboundChannel";
	
	/**
	 * constructor
	 */
	public SipUdpOutboundChannel(ChannelData config) {
		super(config);
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
	 */
	public Class getDeviceInterface() {
		return UDPContext.class;
	}

	
	/*
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.channelfw.OutboundChannel#getDeviceAddress()
	 */
	public Class getDeviceAddress() {
		return UDPContext.class;
	}

	/**
	 * callled by the channel framework, when the udp conn link
	 * is trying to send an outbound message,
	 * before having received any inbound messages.
	 * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw.framework.VirtualConnection)
	 * @see SipUdpConnLink#connect()
	 */
	public ConnectionLink getConnectionLink(VirtualConnection vc) {
		// return the udp conn link that is currently trying to connect()
		SipUdpConnLink connLink = SipUdpConnLink.getPendingConnection();
		return connLink;
	}
	
}
