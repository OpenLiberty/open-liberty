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
package com.ibm.ws.channelfw.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryPropertyIgnoredException;

/**
 * Holds data about the Channel Factory.
 * 
 */
public class ChannelFactoryDataImpl implements ChannelFactoryData {
    /** Serialization ID string */
    private static final long serialVersionUID = 9036496735090447667L;

    /**
     * Properties associated with this ChannelFactory
     */
    private Map<Object, Object> myProperties = null;

    /**
     * device interfaces for this ChannelFactory
     */
    private Class<?>[] deviceInterface = null;

    /**
     * application interface class
     */
    private Class<?> applicationInterface = null;

    /**
     * this channel's factory class
     */
    private Class<?> factory = null;

    /**
     * ChannelFactory created for this data
     */
    private transient ChannelFactory cf = null;

    /**
     * Constructor for channelfactorydataImpl
     * 
     * @param factory
     * @param deviceInterface
     * @param applicationInterface
     */
    public ChannelFactoryDataImpl(Class<?> factory, Class<?>[] deviceInterface, Class<?> applicationInterface) {
        this.factory = factory;
        this.deviceInterface = deviceInterface;
        this.applicationInterface = applicationInterface;
        this.myProperties = new HashMap<Object, Object>();
    }

    /*
     * @see
     * com.ibm.websphere.channelfw.ChannelFactoryData#getApplicationInterface()
     */
    public Class<?> getApplicationInterface() {
        return this.applicationInterface;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelFactoryData#getDeviceInterface()
     */
    public Class<?>[] getDeviceInterface() {
        return this.deviceInterface;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelFactoryData#getProperties()
     */
    public synchronized Map<Object, Object> getProperties() {
        return this.myProperties;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelFactoryData#getFactory()
     */
    public Class<?> getFactory() {
        return this.factory;
    }

    /**
     * internally set the properties associated with this object
     * 
     * @param m
     * @throws ChannelFactoryPropertyIgnoredException
     */
    synchronized void setProperties(Map<Object, Object> m) throws ChannelFactoryPropertyIgnoredException {
        this.myProperties = m;

        if (cf != null) {
            cf.updateProperties(m);
        }
    }

    /**
     * Iternally set a property associated with this object
     * 
     * @param key
     * @param value
     * @throws ChannelFactoryPropertyIgnoredException
     */
    synchronized void setProperty(Object key, Object value) throws ChannelFactoryPropertyIgnoredException {
        if (null == key) {
            throw new ChannelFactoryPropertyIgnoredException("Ignored channel factory property key of null");
        }
        if (myProperties == null) {
            this.myProperties = new HashMap<Object, Object>();
        }
        this.myProperties.put(key, value);
        if (cf != null) {
            cf.updateProperties(myProperties);
        }
    }

    /**
     * internal getChannelFactory associated with this Data object
     * 
     * @return factory
     */
    synchronized ChannelFactory getChannelFactory() {
        return this.cf;
    }

    /**
     * Internally set the channel factory in this data object.
     * 
     * @param factory
     * @throws ChannelFactoryException
     */
    synchronized void setChannelFactory(ChannelFactory factory) throws ChannelFactoryException {
        if (factory != null && cf != null) {
            throw new ChannelFactoryException("ChannelFactory already exists");
        }
        this.cf = factory;
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("ChannelFactoryData: ");
        sb.append(factory);
        sb.append(" devices=[");
        if (deviceInterface != null && 0 != deviceInterface.length) {
            for (int i = 0; i < deviceInterface.length; i++) {
                sb.append(deviceInterface[i]);
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
        } else {
            sb.append(']');
        }
        sb.append(" app=").append(applicationInterface);
        if (null != myProperties && !myProperties.isEmpty()) {
            sb.append(" [");
            for (Entry<Object, Object> entry : this.myProperties.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue());
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
        }
        return sb.toString();
    }
}
