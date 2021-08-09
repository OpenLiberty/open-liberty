/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.CrossRegionSharable;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Factory used to create TCP channel instances.
 */
public class TCPChannelFactory implements ChannelFactory, CrossRegionSharable {

    private static final TraceComponent tc = Tr.register(TCPChannelFactory.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private static final Class<?> appSideClass = TCPConnectionContext.class;

    private Hashtable<String, ChannelTermination> terminationList = new Hashtable<String, ChannelTermination>();
    // alternate class used to create channels
    private static String commClassName = null;
    private static Class<?> commClass = null;
    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     */
    public TCPChannelFactory() {
        this.existingChannels = new HashMap<String, Channel>();
    }

    /*
     * @see CrossRegionSharable#isSharable(Map)
     */
    public boolean isSharable(Map<String, com.ibm.websphere.channelfw.ChannelData> channelConfiguration) {
        return false;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#init(com.ibm.websphere.channelfw
     * .ChannelFactoryData)
     */
    public void init(ChannelFactoryData data) throws ChannelFactoryException {
        // Note, this call will throw an exception if any properties are invalid.
        new TCPFactoryConfiguration(data.getProperties());

        boolean asyncIOEnabled = ChannelFrameworkImpl.getRef().getAsyncIOEnabled();
        // see if an alternate Comm channel class was specified, if so load it
        commClassName = TCPFactoryConfiguration.getCommClass(asyncIOEnabled);
        if (commClassName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Using comm class name [" + commClassName + "]");
            }
            boolean loaded = false;
            final ClassLoader cl = TCPChannelFactory.class.getClassLoader();
            if (cl != null) {
                try {
                    final String name = commClassName;
                    commClass = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>()
                    {
                        public Class<?> run() throws Exception
                    {
                        return cl.loadClass(name);
                    }
                    });
                    loaded = true;
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "loadClass failed; " + e);
                    }
                    FFDCFilter.processException(e, getClass().getName() + ".init", "132", this);
                }
            }

            // try using the default loader, although that will probably fail also
            if (!loaded) {
                try {
                    final String name = commClassName;
                    commClass = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>()
                    {
                        public Class<?> run() throws Exception
                    {
                        return Class.forName(name);
                    }
                    });
                    loaded = true;
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Class.forName failed; " + e);
                    }
                    FFDCFilter.processException(e, getClass().getName() + ".init", "144", this);
                }
            }
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.ConnectorChannelFactory#updateProperties(java
     * .util.Map)
     */
    public void updateProperties(Map<Object, Object> properties) {
        this.commonProperties = properties;
        try {
            new TCPFactoryConfiguration(properties);
        } catch (ChannelFactoryException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to consume property updates");
            }
        }
    }

    /**
     * Declared abstract in the ChannelFactoryImpl class.
     * 
     * @param channelData
     * @return Channel
     * @throws ChannelException
     */
    protected Channel createChannel(ChannelData channelData) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createChannel");
        }

        TCPChannelConfiguration newCC = new TCPChannelConfiguration(channelData);
        TCPChannel channel = null;

        boolean isOverrideClassUsed = false;
        if ((!newCC.isNIOOnly())
            && (commClass != null)
            && (ChannelFrameworkImpl.getRef().getAsyncIOEnabled())) {
            try {
                channel = (TCPChannel) commClass.newInstance();
                ChannelTermination ct = channel.setup(channelData, newCC, this);
                if (ct != null) {
                    this.terminationList.put(commClassName, ct);
                }
                isOverrideClassUsed = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "using CommClass: " + commClass);
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception trying to instantiate CommClass: " + e);
                }
            }
        }

        if (!isOverrideClassUsed) {
            channel = new NioTCPChannel();
            ChannelTermination ct = channel.setup(channelData, newCC, this);
            if (ct != null) {
                this.terminationList.put("NioTCPChannel", ct);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "createChannel");
        }
        return channel;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getApplicationInterface()
     */
    public Class<?> getApplicationInterface() {
        return appSideClass;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#destroy()
     */
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }

        // Go through the hashtable and destroy all long-lived, single instance,
        // resources related to a given TCP Channel type (NIO, AIO, etc...)
        for (ChannelTermination ct : this.terminationList.values()) {
            ct.terminate();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#findOrCreateChannel(ChannelData)
     */
    public synchronized Channel findOrCreateChannel(ChannelData channelData) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "findOrCreateChannel: " + channelData.getName());
        }
        String channelName = channelData.getName();
        Channel ret = this.existingChannels.get(channelName);
        if (ret == null) {
            // Create the new channel with the input configuration
            ret = createChannel(channelData);
            this.existingChannels.put(channelName, ret);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "findOrCreateChannel");
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
     * com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java
     * .util.Map)
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return new TCPOutboundDefinition(props);
    }

    /**
     * TCP channel implementation of an outbound definition.
     */
    private static class TCPOutboundDefinition implements OutboundChannelDefinition {
        private static final long serialVersionUID = -7427145547238486815L;

        /**
         * Constructor.
         * 
         * @param props
         */
        protected TCPOutboundDefinition(Map<Object, Object> props) {
            // nothing to do, no carry over from inbound props to outbound
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
            return TCPChannelFactory.class;
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
