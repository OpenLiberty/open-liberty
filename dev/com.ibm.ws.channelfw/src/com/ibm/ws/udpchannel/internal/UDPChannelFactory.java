/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.udpchannel.UDPContext;

/**
 * @author mjohnson
 */
public class UDPChannelFactory implements ChannelFactory {
    private static final TraceComponent tc = Tr.register(UDPChannelFactory.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    private static final Class<?> appSideClass = UDPContext.class;

    protected UDPChannelFactoryConfiguration factoryConfig = null;
    private WorkQueueManager workQueueManager = null;
    private VirtualConnectionFactory vcFactory = null;
    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     */
    public UDPChannelFactory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Created " + this);
        }
        this.existingChannels = new HashMap<String, Channel>();
    }

    protected Channel createChannel(ChannelData data) throws ChannelException {
        UDPChannelConfiguration newCC = new UDPChannelConfiguration();
        newCC.setChannelData(data);

        // check to see if we should dispatch to threadss
        try {
            return new UDPChannel(newCC, getWorkQueueManager());
        } catch (IOException ioe) {
            throw new ChannelException("Unable to create UDP channel", ioe);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#init(com.ibm.websphere.channelfw
     * .ChannelFactoryData)
     */
    public void init(ChannelFactoryData data) throws ChannelFactoryException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "init: " + this);
        }

        this.commonProperties = data.getProperties();
        this.factoryConfig = new UDPChannelFactoryConfiguration(data);
        this.vcFactory = ChannelFrameworkFactory.getChannelFramework().getInboundVCFactory();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "init");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#destroy()
     */
    public void destroy() {
        this.existingChannels.clear();
        this.existingChannels = null;
        try {
            this.vcFactory.destroy();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Error disconnecting from vc factory; " + e);
            }
        }
        this.vcFactory = null;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getApplicationInterface()
     */
    public Class<?> getApplicationInterface() {
        return appSideClass;
    }

    protected VirtualConnectionFactory getVCFactory() {
        return this.vcFactory;
    }

    private WorkQueueManager getWorkQueueManager() throws IOException {
        WorkQueueManager localWorkQueueManager = null;
        if (factoryConfig.isUniqueWorkerThreads()) {
            localWorkQueueManager = new WorkQueueManager(this);
        } else {
            if (workQueueManager == null) {
                workQueueManager = new WorkQueueManager(this);
            }
            localWorkQueueManager = workQueueManager;
        }

        return localWorkQueueManager;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#findOrCreateChannel(ChannelData)
     */
    public synchronized Channel findOrCreateChannel(ChannelData channelData) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "findOrCreateChannel: " + channelData.getName());
        }
        String channelName = channelData.getName();
        Channel ret = this.existingChannels.get(channelName);
        if (ret == null) {
            // Create the new channel with the input configuration
            ret = createChannel(channelData);
            this.existingChannels.put(channelName, ret);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "findOrCreateChannel");
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
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getDeviceInterface()
     */
    public final Class<?>[] getDeviceInterface() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.ConnectorChannelFactory#updateProperties(java
     * .util.Map)
     */
    public void updateProperties(Map<Object, Object> properties) {
        this.commonProperties = properties;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java
     * .util.Map)
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return new UDPOutboundDefinition(props);
    }

    /**
     * UDP channel implementation of an outbound definition.
     */
    private static class UDPOutboundDefinition implements OutboundChannelDefinition {
        private static final long serialVersionUID = -7427145547238486815L;

        /**
         * Constructor.
         * 
         * @param props
         */
        protected UDPOutboundDefinition(Map<Object, Object> props) {
            // no carry over from inbound props to outbound
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#
         * getOutboundChannelProperties()
         */
        @Override
        public Map<Object, Object> getOutboundChannelProperties() {
            // nothing necessary
            return null;
        }

        /*
         * @see
         * com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactory
         * ()
         */
        @Override
        public Class<?> getOutboundFactory() {
            return UDPChannelFactory.class;
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#
         * getOutboundFactoryProperties()
         */
        @Override
        public Map<Object, Object> getOutboundFactoryProperties() {
            // nothing necessary
            return null;
        }
    }

}
