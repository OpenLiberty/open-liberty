/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;

/**
 * The factory class for LocalCommChannel objects.
 * 
 * This factory class is registered with CFW via LocalCommChannelFactoryProvider.
 */
public class LocalCommChannelFactory implements ChannelFactory {

	/** Properties */
	private Map<Object, Object> properties = null;
	
	 /** Map of the existing channels for this factory. */
    private static Map<String, Channel> existingChannels =  new HashMap<String, Channel>();

    /**
     * Note: this method must be synchronized because it reads and updates the existingChannels 
     * HashMap.  HashMaps must be externally synchronized.
     */
	@Override
	public synchronized Channel findOrCreateChannel(ChannelData config) throws ChannelException {
		String channelName = config.getName();
        Channel channel = LocalCommChannelFactory.existingChannels.get(channelName);
        if (channel == null) {
            // Create the new channel with the input configuration
            channel = new LocalCommChannel(config);
            LocalCommChannelFactory.existingChannels.put(channelName, channel);
        }
        
        return channel;
	}

	@Override
	public void updateProperties(Map<Object, Object> properties)
			throws ChannelFactoryPropertyIgnoredException {
		// TODO: Figure out if we need to merge property maps.
		this.properties = properties;
	}

	@Override
	public void init(ChannelFactoryData data) throws ChannelFactoryException {
	}

	@Override
	public void destroy() {}

	@Override
	public Map<Object, Object> getProperties() {
		return properties;
	}

	@Override
	public Class<?> getApplicationInterface() {
		return LocalCommServiceContext.class;
	}

	@Override
	public Class<?>[] getDeviceInterface() {
		throw new IllegalStateException("Not implemented and should not be used");
	}

	@Override
	public OutboundChannelDefinition getOutboundChannelDefinition(
			Map<Object, Object> props) {
		return null;
	}

}
