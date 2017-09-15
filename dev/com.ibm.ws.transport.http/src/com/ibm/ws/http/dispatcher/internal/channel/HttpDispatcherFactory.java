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
package com.ibm.ws.http.dispatcher.internal.channel;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * Factory used to create the individual HTTP Dispatcher channels that will
 * cap each HTTP protocol chain.
 */
public class HttpDispatcherFactory implements ChannelFactory {

    /** Property for the default buffer size in output streams */
    public static final String PROP_BUFFERSIZE = "bufferSize";

    /** List of interfaces that channels of this type support underneath them */
    private final Class<?>[] devSide = new Class<?>[] { HttpInboundServiceContext.class };
    /** Factory level buffer size for all channels created */
    private Object globalBufferSize = null;
    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     */
    public HttpDispatcherFactory() {
        this.existingChannels = new HashMap<String, Channel>();
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getDeviceInterface()
     */
    public Class<?>[] getDeviceInterface() {
        return this.devSide;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#init(ChannelFactoryData)
     */
    @SuppressWarnings("unused")
    public void init(ChannelFactoryData data) throws ChannelFactoryException {
        // do nothing
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#destroy()
     */
    public void destroy() {
        // nothing to clean up
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#findOrCreateChannel(ChannelData)
     */
    @SuppressWarnings("unused")
    public synchronized Channel findOrCreateChannel(ChannelData channelData) throws ChannelException {
        String channelName = channelData.getName();
        Channel ret = this.existingChannels.get(channelName);
        if (ret == null) {
            // Create the new channel with the input configuration
            if (null != this.globalBufferSize) {
                // if the factory setting exists, see if the channel overrides it
                // otherwise apply it now
                Map<Object, Object> props = channelData.getPropertyBag();
                if (!props.containsKey(PROP_BUFFERSIZE)) {
                    props.put(PROP_BUFFERSIZE, this.globalBufferSize);
                }
            }
            ret = new HttpDispatcherChannel(channelData, this);
            this.existingChannels.put(channelName, ret);
        }
        return ret;
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
    public synchronized void updateProperties(Map<Object, Object> properties) {
        this.commonProperties = properties;
        this.globalBufferSize = properties.get(PROP_BUFFERSIZE);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getApplicationInterface()
     */
    public final Class<?> getApplicationInterface() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java.util.Map)
     */
    @Override
    @SuppressWarnings("unused")
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return null;
    }
}
