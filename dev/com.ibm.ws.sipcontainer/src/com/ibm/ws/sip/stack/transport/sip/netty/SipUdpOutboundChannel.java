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

import com.ibm.websphere.channelfw.ChannelData;

/**
 * an outbound udp channel
 * 
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

}
