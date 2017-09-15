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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ChannelFramework;

/**
 * Runtime channel configuration object used within the framework.
 */
public class ChannelDataImpl implements ChannelData {

    /** Serialization ID string */
    private static final long serialVersionUID = -280440118973381476L;

    private static final TraceComponent tc = Tr.register(ChannelDataImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /** String added to the end of external channel names to make child names. */
    public static final String CHILD_STRING = "_CFINTERNAL_CHILD_";
    /** Name of the channel */
    private String name = null;

    /** Bag of channel specific properties - taken from discriptor */
    private transient Map<Object, Object> properties = null;

    /** Type of factory */
    private Class<?> factoryType = null;
    /** Weight used by previous channels when doing discrimination. */
    private int discWeight = 0;

    /** Reference to the top level channel framework */
    private transient ChannelFramework framework = null;

    /** Incrementing counter used to generate child channel data object names */
    private int childId = 0;
    /** List of child data objects. key=name, value=ChildChannelDataImpl */
    private List<ChildChannelDataImpl> children = null;
    /**
     * Class of the device interface that device side channels should use to
     * communicate with this channel.
     */
    private Class<?> deviceInterface;
    /** Whether the channel represented by this data is inbound or outbound. */
    private boolean isInbound = true;

    /**
     * Constructor.
     * 
     * @param channelName
     * @param factoryType
     * @param propertyBag
     * @param discriminatorWeight
     * @param inputCF
     */
    public ChannelDataImpl(String channelName, Class<?> factoryType, Map<Object, Object> propertyBag, int discriminatorWeight, ChannelFramework inputCF) {
        this.name = channelName;
        this.factoryType = factoryType;
        this.properties = propertyBag;
        this.discWeight = discriminatorWeight;
        // Note, the isInbound flag only has to be valid for child channel data
        // objects. This is taken
        // care of at chain creation. This is a best effort to get the parent
        // channel data right.
        // However, it shouldn't matter as it is not meant to be used in the parent,
        // and it won't be exposed
        // to callers of the Channel Framework interface.
        if (this.discWeight == ChannelFrameworkImpl.DEFAULT_DISC_WEIGHT) {
            this.isInbound = false;
        } else {
            this.isInbound = true;
        }
        this.deviceInterface = null;
        this.framework = inputCF;
        this.childId = 0;
        this.children = new ArrayList<ChildChannelDataImpl>(0);
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#getName()
     */
    public String getName() {
        return this.name;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#getFactoryType()
     */
    public Class<?> getFactoryType() {
        return this.factoryType;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#getPropertyBag()
     */
    public Map<Object, Object> getPropertyBag() {
        return this.properties;
    }

    /**
     * Set the properties to be used by this channel.
     * 
     * @param inputProperties
     */
    public void setPropertyBag(Map<Object, Object> inputProperties) {
        this.properties = inputProperties;
    }

    /**
     * Set the properties to be used by this channel.
     * 
     * @param propertyKey
     * @param propertyValue
     */
    public void setProperty(Object propertyKey, Object propertyValue) {
        this.properties.put(propertyKey, propertyValue);
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#getDiscriminatorWeight()
     */
    public int getDiscriminatorWeight() {
        return this.discWeight;
    }

    /**
     * Set the discrimination weight to be used by this channel.
     * 
     * @param inputWeight
     */
    public void setDiscriminatorWeight(int inputWeight) {
        this.discWeight = inputWeight;
    }

    /**
     * Query the class supported by this channel underneath it on a chain.
     * 
     * @return Class<?>
     */
    public Class<?> getDeviceInterface() {
        return this.deviceInterface;
    }

    /**
     * Set the device side interface. This is called right before a channel is
     * created.
     * 
     * @param inputDeviceInterface
     */
    public void setDeviceInterface(Class<?> inputDeviceInterface) {
        this.deviceInterface = inputDeviceInterface;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#isInbound()
     */
    public boolean isInbound() {
        return this.isInbound;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#isOutbound()
     */
    public boolean isOutbound() {
        return !this.isInbound;
    }

    /**
     * This is called when a chain is being built and the caller knows for sure
     * whether
     * this channel is being used for inbound or outbound.
     * 
     * @param inputIsInbound
     */
    public void setIsInbound(boolean inputIsInbound) {
        this.isInbound = inputIsInbound;
    }

    /**
     * Get a reference to the framework including this channel.
     * 
     * @return framework
     */
    public ChannelFramework getChannelFramework() {
        return this.framework;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChannelData#getExternalName()
     */
    public String getExternalName() {
        return getName();
    }

    /**
     * Get the next child (unique) child id to be used to create a child.
     * 
     * @return child id
     */
    private int nextChildId() {
        return this.childId++;
    }

    /**
     * Create a child data object. Add it to the list of children and return it.
     * 
     * @return child channel data object
     */
    public ChildChannelDataImpl createChild() {
        String childName = this.name + CHILD_STRING + nextChildId();
        ChildChannelDataImpl child = new ChildChannelDataImpl(childName, this);
        this.children.add(child);
        return child;
    }

    /**
     * Get an inbound child of this parent. If one doesn't exist, return null.
     * 
     * @return inbound child
     */
    public ChildChannelDataImpl getInboundChild() {
        for (ChildChannelDataImpl child : this.children) {
            if (child.isInbound()) {
                return child;
            }
        }
        return null;
    }

    /**
     * Get an outbound child of this parent. If one doesn't exist, return null.
     * 
     * @return outbound child
     */
    public ChildChannelDataImpl getOutboundChild() {
        for (ChildChannelDataImpl child : this.children) {
            if (child.isOutbound()) {
                return child;
            }
        }
        return null;
    }

    /**
     * Remove a child from the list of children, if it exists.
     * 
     * @param child
     *            channel data object to be dereferenced
     */
    public void removeChild(ChildChannelDataImpl child) {
        this.children.remove(child);
    }

    /**
     * Get the number of children.
     * 
     * @return number of children
     */
    public int getNumChildren() {
        return this.children.size();
    }

    /**
     * Get iterator of children to look through.
     * 
     * @return iterator
     */
    public Iterator<ChildChannelDataImpl> children() {
        return this.children.iterator();
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("ChannelData:");
        sb.append("\r\n\tname = ").append(this.name);
        sb.append("\r\n\tdiscweight = ").append(this.discWeight);
        sb.append("\r\n\tfactoryType = ").append(this.factoryType);
        sb.append("\r\n\tdeviceInterface = ").append(this.deviceInterface);
        sb.append("\r\n");
        if (null != this.properties) {
            sb.append("\tproperties: ").append(this.properties.size()).append("\r\n");
            Object[] keys = this.properties.keySet().toArray();
            Object[] values = this.properties.values().toArray();
            for (int i = 0; i < keys.length; i++) {
                sb.append("\t\tkey = ").append(keys[i]).append(", value = ");
                if (values[i] instanceof ChainDataImpl) {
                    sb.append(((ChainDataImpl) values[i]).getName());
                } else {
                    sb.append(values[i]);
                }
                sb.append("\r\n");
            }
        }
        sb.append("\tchildId = ").append(this.childId).append("\r\n");
        sb.append("\tchildren: ").append(this.children.size()).append("\r\n");
        for (ChildChannelDataImpl child : this.children) {
            sb.append("\t\tchild = ").append(child.getName()).append("\r\n");
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Private Methods
    // -------------------------------------------------------------------------

    /**
     * Override the Serializable method.
     * 
     * @param stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {

        stream.defaultWriteObject();

        // not all values in the property bag are going to be serializable; we
        // need to be careful which parts of the bag we serialize
        //
        Map<Object, Object> tempProperties = new HashMap<Object, Object>();
        for (Object key : this.properties.keySet()) {
            Object value = this.properties.get(key);

            if (value instanceof Serializable || value instanceof Externalizable) {

                tempProperties.put(key, value);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "value for key \"" + key + "\" is not serializable");
                }
            }
        }

        stream.writeObject(tempProperties);
    }

    /**
     * Override the Serializable method.
     * 
     * @param stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.properties = (Map<Object, Object>) stream.readObject();
    }
}
