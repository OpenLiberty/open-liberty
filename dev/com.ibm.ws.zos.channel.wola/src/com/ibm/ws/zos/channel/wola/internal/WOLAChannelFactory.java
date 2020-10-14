/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

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
 * WOLA channel factory.
 */
public class WOLAChannelFactory implements ChannelFactory {

    /**
     * Map caching previously created channels for this factory.
     */
    private final Map<String, Channel> channels = new HashMap<String, Channel>();

    /**
     * Property map for the factory.
     */
    private Map<Object, Object> properties = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel findOrCreateChannel(ChannelData config) throws ChannelException {
        String channelName = config.getName();
        Channel channel = this.channels.get(channelName);
        if (channel == null) {
            channel = new WOLAChannel(config);
            channels.put(channelName, channel);
        }
        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateProperties(Map<Object, Object> properties) throws ChannelFactoryPropertyIgnoredException {
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ChannelFactoryData data) throws ChannelFactoryException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // TODO.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Object, Object> getProperties() {
        return this.properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getApplicationInterface() {
        throw new IllegalStateException("Not implemented. This channel is at the top of the chain");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?>[] getDeviceInterface() {
        return new Class<?>[] { LocalCommServiceContext.class };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return null;
    }
}
