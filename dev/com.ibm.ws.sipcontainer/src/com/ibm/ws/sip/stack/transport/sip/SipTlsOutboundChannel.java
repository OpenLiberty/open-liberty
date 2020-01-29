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
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * an outbound tls channel
 * 
 * @author ran
 */
public class SipTlsOutboundChannel extends SipOutboundChannel
{
	public final static String SipTlsOutboundChannelName = "SipTlsOutboundChannel";
	/**
	 * constructor
	 */
	public SipTlsOutboundChannel(ChannelData config) {
		super(config);
	}

	/**
	 * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
	 */
	public Class getDeviceInterface() {
		return TCPConnectionContext.class;
	}

	/**
	 * @see com.ibm.wsspi.channelfw.OutboundChannel#getDeviceAddress()
	 * @see SipTlsOutboundConnLink#createConnectRequestContext(String, int)
	 */
	public Class getDeviceAddress() {
		return TCPConnectRequestContext.class;
	}

}
