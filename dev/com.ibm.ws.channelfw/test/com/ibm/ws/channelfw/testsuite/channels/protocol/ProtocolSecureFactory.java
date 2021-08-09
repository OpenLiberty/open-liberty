/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import com.ibm.wsspi.channelfw.SSLChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Secure protocol (mid-chain) factory.
 */
@SuppressWarnings("unused")
public class ProtocolSecureFactory implements SSLChannelFactory {
    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     */
    public ProtocolSecureFactory() {
        this.existingChannels = new HashMap<String, Channel>();
    }

    @Override
    public void destroy() {
        // nothing
    }

    @Override
    public Channel findOrCreateChannel(ChannelData config) throws ChannelException {
        String channelName = config.getName();
        Channel rc = this.existingChannels.get(channelName);
        if (null == rc) {
            rc = new ProtocolSecureChannel(config);
            this.existingChannels.put(channelName, rc);
        }
        return rc;
    }

    @Override
    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

    @Override
    public Class<?>[] getDeviceInterface() {
        return new Class<?>[] { TCPConnectionContext.class };
    }

    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return new OutDef(props);
    }

    @Override
    public Map<Object, Object> getProperties() {
        return this.commonProperties;
    }

    @Override
    public void init(ChannelFactoryData data) throws ChannelFactoryException {
        // nothing
    }

    @Override
    public void updateProperties(Map<Object, Object> properties)
                    throws ChannelFactoryPropertyIgnoredException {
        this.commonProperties = properties;
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
            return ProtocolSecureFactory.class;
        }

        @Override
        public Map<Object, Object> getOutboundFactoryProperties() {
            return null;
        }
    }
}
