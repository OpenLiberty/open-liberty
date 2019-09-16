/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip;

import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;

/**
 * creates outbound udp channels
 * 
 * @author ran
 */
public class SipUdpOutboundChannelFactory extends SipChannelFactory
{
	
	@Override
	public Channel findOrCreateChannel(ChannelData config){
		return new SipUdpOutboundChannel(config);
	
	}

	@Override
	public void updateProperties(Map<Object, Object> properties)
			throws ChannelFactoryPropertyIgnoredException {
		
	}

	@Override
	public Map<Object, Object> getProperties() {
		return null;
	}

	@Override
	public OutboundChannelDefinition getOutboundChannelDefinition(
			Map<Object, Object> props) {
		return null;
	}

}
