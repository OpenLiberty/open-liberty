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

import com.ibm.websphere.channelfw.ChannelData;

/**
 * an outbound tcp channel
 * 
 */
public class SipTcpOutboundChannel extends SipOutboundChannel
{
	
	public final static String SipTcpOutboundChannelName = "SipTcpOutboundChannel";
	/**
	 * constructor
	 */
	public SipTcpOutboundChannel(ChannelData config) {
		super(config);
	}

}
