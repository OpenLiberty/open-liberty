/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.SSLChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * This class is a factory responsible for generating SSL channel instances and
 * managing any existing common resources.
 */
public class SSLChannelFactoryImpl implements SSLChannelFactory {

    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     */
    public SSLChannelFactoryImpl() {
        this.existingChannels = new HashMap<String, Channel>();
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getApplicationInterface()
     */
    @Override
    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getDeviceInterface()
     */
    @Override
    public final Class<?>[] getDeviceInterface() {
        return new Class[] { TCPConnectionContext.class };
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#init(com.ibm.websphere.channelfw.ChannelFactoryData)
     */
    @Override
    public void init(ChannelFactoryData data) {
        // Currently no properties supported, so do nothing.
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#destroy()
     */
    @Override
    public void destroy() {
        // Nothing to do at this point.
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#findOrCreateChannel(ChannelData)
     */
    @Override
    public synchronized Channel findOrCreateChannel(ChannelData channelData) throws ChannelException {
        String channelName = channelData.getName();
        Channel rc = this.existingChannels.get(channelName);
        if (null == rc) {
            rc = new SSLChannel(channelData, this);
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
    @Override
    public Map<Object, Object> getProperties() {
        return this.commonProperties;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#updateProperties(java.util.Map)
     */
    @Override
    public void updateProperties(Map<Object, Object> props) {
        this.commonProperties = props;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java.util.Map)
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return new SSLOutboundDefinition(props);
    }

    /**
     * SSL channel implementation of an outbound definition.
     */
    private static class SSLOutboundDefinition implements OutboundChannelDefinition {
        private static final long serialVersionUID = 1555556446109438220L;

        private Map<Object, Object> config = null;

        /**
         * Constructor.
         * 
         * @param props
         */
        protected SSLOutboundDefinition(Map<Object, Object> props) {
            this.config = new HashMap<Object, Object>();
            String repertoire = (String) props.get(SSLChannelData.ALIAS_KEY);
            if (null != repertoire) {
                this.config.put(SSLChannelData.ALIAS_KEY, repertoire);
            }
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundChannelProperties()
         */
        @Override
        public Map<Object, Object> getOutboundChannelProperties() {
            return this.config;
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactory()
         */
        @Override
        public Class<?> getOutboundFactory() {
            return SSLChannelFactoryImpl.class;
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactoryProperties()
         */
        @Override
        public Map<Object, Object> getOutboundFactoryProperties() {
            // nothing necessary
            return null;
        }
    }
}
