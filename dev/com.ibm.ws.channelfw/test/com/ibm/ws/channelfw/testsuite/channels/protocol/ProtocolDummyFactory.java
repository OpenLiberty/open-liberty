/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.channels.protocol;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Dummy protocol-type factory for testing.
 */
@SuppressWarnings("unused")
public class ProtocolDummyFactory implements ChannelFactory {
    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     */
    public ProtocolDummyFactory() {
        this.existingChannels = new HashMap<String, Channel>();
    }

    protected Channel createChannel(ChannelData config) throws ChannelException {
        return new ProtocolDummyChannel(config, this);
    }

    public void destroy() {
        // nothing
    }

    public Class<?> getApplicationInterface() {
        return ProtocolDummyContext.class;
    }

    public Class<?>[] getDeviceInterface() {
        return new Class<?>[] { TCPConnectionContext.class };
    }

    public void init(ChannelFactoryData data) throws ChannelFactoryException {
        // nothing
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#findOrCreateChannel(ChannelData)
     */
    public synchronized Channel findOrCreateChannel(ChannelData channelData) throws ChannelException {
        String channelName = channelData.getName();
        Channel rc = this.existingChannels.get(channelName);
        if (null == rc) {
            rc = createChannel(channelData);
            this.existingChannels.put(channelName, rc);
        }
        return rc;
    }

    /**
     * Remove a channel from the existing channels list.
     * 
     * @param channelName
     */
    public synchronized void removeChannel(String channelName) {
        this.existingChannels.remove(channelName);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getProperties()
     */
    public Map<Object, Object> getProperties() {
        return this.commonProperties;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#updateProperties(java.util.Map)
     */
    public void updateProperties(Map<Object, Object> properties) throws ChannelFactoryPropertyIgnoredException {
        this.commonProperties = properties;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java.util.Map)
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return new OutDef(props);
    }

    private class OutDef implements OutboundChannelDefinition {
        private static final long serialVersionUID = -7625993397141832104L;

        public OutDef(Map<Object, Object> props) {
            // nothing
        }

        @Override
        public Map<Object, Object> getOutboundChannelProperties() {
            return null;
        }

        @Override
        public Class<?> getOutboundFactory() {
            return ProtocolDummyFactory.class;
        }

        @Override
        public Map<Object, Object> getOutboundFactoryProperties() {
            return null;
        }
    }
}
