/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Abstract channel factory class for the HTTP channel. This holds
 * the basic variables and provides the common methods to both
 * Inbound and Outbound channels.
 * 
 */
public abstract class HttpChannelFactory implements ChannelFactory {

    /** Configuration for this channel factory */
    private HttpFactoryConfig myConfig;
    /** Application side interface class */
    private Class<?> appInterface = null;
    /** Array of device side interface classes */
    private Class<?>[] devInterfaces = { TCPConnectionContext.class };
    /** Factory to create HTTP related objects */
    private static final HttpObjectFactory myObjectFactory = new HttpObjectFactory();
    /** Flag on whether to allow a large message by one of the channels */
    private boolean allowLargeMessage = true;
    /** Sync object for checking the overall allow large message flag */
    private Object key = new Object()
    {
                    };
    /** Map of the existing channels for this factory. */
    private Map<String, Channel> existingChannels = null;
    /** Property map that may or may not exist for the factory. */
    private Map<Object, Object> commonProperties = null;

    /**
     * Constructor.
     * 
     * @param input_class
     *            (application interface)
     */
    public HttpChannelFactory(Class<?> input_class) {
        this.appInterface = input_class;
        this.existingChannels = new HashMap<String, Channel>();
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getApplicationInterface()
     */
    public Class<?> getApplicationInterface() {
        return this.appInterface;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getDeviceInterface()
     */
    public Class<?>[] getDeviceInterface() {
        return this.devInterfaces;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#init(com.ibm.websphere.channelfw
     * .ChannelFactoryData)
     */
    public void init(ChannelFactoryData data) {
        updateProperties(data.getProperties());
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#destroy()
     */
    public void destroy() {
        // channelfactory has no objects to destroy
    }

    /**
     * Get access to the object factory shared by all the channels.
     * 
     * @return HttpObjectFactory
     */
    protected HttpObjectFactory getObjectFactory() {
        return myObjectFactory;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.ProtocolChannelFactory#updateProperties(java
     * .util.Map)
     */
    public void updateProperties(Map<Object, Object> properties) {
        this.commonProperties = properties;
        this.myConfig = new HttpFactoryConfig(properties);
    }

    /**
     * Get access to the configuration for this channel factory.
     * 
     * @return HttpFactoryConfig
     */
    public HttpFactoryConfig getConfig() {
        return this.myConfig;
    }

    /**
     * Once a large message has filtered out of the system, this method will
     * be used to notify the factory that it has done so.
     */
    public void releaseLargeMessage() {

        // check to see if messages are limited, otherwise we don't need to do
        // anything about this call
        if (getConfig().areMessagesLimited()) {
            synchronized (this.key) {
                this.allowLargeMessage = true;
            }
        }
    }

    /**
     * Query whether a large message is allowed or not at this time. This means
     * the factory checks whether all of the created channels satisfy the
     * criteria for allowing a large message into the system.
     * <p>
     * This is intended for the 31-bit z/os install where one large message is allowed into the system beyond the standard channel level limit; however, it might be used if a user
     * configures the factory config with the large buffer property.
     * 
     * @param size
     *            of message
     * @return boolean (true means yes, false means no and the caller should
     *         take steps to prevent the message from being processed fully)
     */
    public boolean allowLargeMessage(long size) {
        if (!getConfig().areMessagesLimited()) {
            // no limits on messages, so default to yes
            return true;
        }

        if (HttpConfigConstants.UNLIMITED == getConfig().getLargerBufferSize()) {
            // no second upper limit, the first is a hard ceiling
            // unlimited is the same as not-set
            return false;
        }
        // otherwise compare sizes
        if (size > getConfig().getLargerBufferSize()) {
            return false;
        }

        // if the current setting is true, then change it to false for anybody
        // else checking and let the last caller in. If it is false, then let
        // them know that.
        synchronized (this.key) {
            if (this.allowLargeMessage) {
                this.allowLargeMessage = false;
                return true;
            }
            return false;
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#findOrCreateChannel(ChannelData)
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
     * Create a channel based on this configuration.
     * <p>
     * Each implementation should have one single unique channel per channel configuration. This method will not be called for existing channel implementations.
     * 
     * @param config
     * @return Channel
     * @throws ChannelException
     *             if channel cannot be created
     */
    abstract protected Channel createChannel(ChannelData config) throws ChannelException;

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

}
