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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.chains.InboundChain;
import com.ibm.ws.channelfw.internal.chains.OutboundChain;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.RetryableChainEventListener;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;
import com.ibm.wsspi.channelfw.exception.IncoherentChainException;

/**
 * The implementation of the chain configuration.
 */
public class ChainDataImpl implements ChainData {

    /** Serialization ID string */
    private static final long serialVersionUID = 4499898682104370008L;

    /** Trace service */
    private static final TraceComponent tc = Tr.register(ChainDataImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);
    /**
     * name of the chain
     */
    private String name = null;
    /**
     * type (INBOUND || OUTBOUND)
     */
    private FlowType type = null;
    /**
     * ChannelData for the chains
     */
    private ChannelData[] channelDataArray = null;

    /**
     * listeners for this chain
     */
    private transient final CopyOnWriteArraySet<ChainEventListener> chainEventListeners;

    /**
     * Hash used to make future searching fast.
     */
    private int channelFactoryHash = 0;

    /**
     * Map of chain properties.
     */
    private Map<Object, Object> properties = null;

    /** Flag on whether this chain is enabled or not */
    private transient boolean enabled = true;
    /** Endpoint object created when queried */
    private transient CFEndPointImpl endPoint = null;

    /**
     * Constructor.
     * 
     * @param chainName
     * @param chainType
     * @param channelList
     * @param inputProperties
     * @throws IncoherentChainException
     */
    public ChainDataImpl(String chainName, FlowType chainType, ChannelData[] channelList, Map<Object, Object> inputProperties) throws IncoherentChainException {
        this(chainName, chainType, channelList, inputProperties, new CopyOnWriteArraySet<ChainEventListener>());
    }

    private ChainDataImpl(String chainName, FlowType chainType, ChannelData[] channelList, Map<Object, Object> inputProperties, CopyOnWriteArraySet<ChainEventListener> listeners) throws IncoherentChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "constructor", listeners);
        }
        this.name = chainName;
        this.type = chainType;
        this.channelDataArray = channelList;
        this.chainEventListeners = listeners;
        this.properties = inputProperties;
        // Verify coherency of the chain.
        if (chainType.equals(FlowType.INBOUND)) {
            InboundChain.verifyChainCoherency(this);
        } else {
            OutboundChain.verifyChainCoherency(this);
        }
        // Create hash key used for future fast searching.
        for (int i = 0; i < this.channelDataArray.length; i++) {
            this.channelFactoryHash += this.channelDataArray[i].getFactoryType().hashCode();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (this.properties == null) {
                Tr.debug(tc, "Properties are null");
            } else {
                Tr.debug(tc, "Properties: " + this.properties.size());
                for (Object key : this.properties.keySet()) {
                    Tr.debug(tc, "\tkey=" + key + ", value=" + this.properties.get(key));
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "constructor");
        }
    }

    /**
     * Constructor used to update the list of channel data objects, but to
     * replicate all remaining data from an existing instantiation.
     * <p>
     * ** THIS CREATES A SHARING RELATIONSHIP BETWEEN THE OLD AND NEW CHAIN DATA FOR
     * THE LIST OF LISTENERS. **
     * 
     * @param oldChainData
     * @param newChannelList
     * @throws IncoherentChainException
     */
    public ChainDataImpl(ChainDataImpl oldChainData, ChannelData[] newChannelList) throws IncoherentChainException {
        this(oldChainData.getName(), oldChainData.getType(), newChannelList, oldChainData.getPropertyBag(), oldChainData.chainEventListeners);
    }

    /*
     * @see com.ibm.wsspi.channelfw.framework.ChainData#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of this chain configuration to the input string.
     * 
     * @param inputName
     */
    public void setName(String inputName) {
        this.name = inputName;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChainData#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChainData#setEnabled(boolean)
     */
    @Override
    public void setEnabled(boolean flag) {
        this.enabled = flag;
    }

    /*
     * @see com.ibm.wsspi.channelfw.framework.ChainData#getType()
     */
    @Override
    public FlowType getType() {
        return this.type;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChainData#getChannelList()
     */
    @Override
    public ChannelData[] getChannelList() {
        return this.channelDataArray;
    }

    /**
     * Hash used to make future searching fast. Sum of channel
     * factory class name hashcodes.
     * 
     * @return int
     */
    public int getChannelFactoryHash() {
        return this.channelFactoryHash;
    }

    /**
     * For internal testing only to force code paths.
     * THIS SHOULD NOT BE CALLED OUTSIDE OF TESTING
     * 
     * @param hash
     */
    public void setChannelFactoryHash(int hash) {
        this.channelFactoryHash = hash;
    }

    /**
     * Fetch the property bag associated with this Chain.
     * 
     * @return Map<Object,Object>
     */
    public Map<Object, Object> getPropertyBag() {
        if (this.properties == null) {
            this.properties = new HashMap<Object, Object>();
        }
        return this.properties;
    }

    /**
     * Assign the the map of properties. This is typically called during a
     * chain update.
     * 
     * @param inputProperties
     */
    public void setPropertyBag(Map<Object, Object> inputProperties) {
        this.properties = inputProperties;
    }

    /**
     * Check whether this chain contains the input channel.
     * 
     * @param channelName
     * @return boolean
     */
    public boolean containsChannel(String channelName) {
        boolean found = false;
        for (int i = 0; i < this.channelDataArray.length; i++) {
            if (this.channelDataArray[i].getName().equals(channelName)) {
                found = true;
                break;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "containsChannel: " + channelName + " " + found);
        }
        return found;
    }

    /**
     * Create a cloned representation of this chain data which only includes
     * references to parent channel data objects. It hides the children and
     * their names.
     * 
     * @return cloned chain data including parent channel data objects
     */
    public ChainDataImpl getExternalChainData() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getExternalChainData");
        }
        // Create the list of the parents of the child data objects.
        ChannelData[] parents = new ChannelData[this.channelDataArray.length];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = ((ChildChannelDataImpl) this.channelDataArray[i]).getParent();
        }

        ChainDataImpl externalChainData = null;
        try {
            externalChainData = new ChainDataImpl(getName(), getType(), parents, null);
        } catch (IncoherentChainException e) {
            // No FFDC Needed
            // This should never happen
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Unable to build external version of chain data, " + getName());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getExternalChainData");
        }
        return externalChainData;
    }

    // *************************************
    // Event Listener Methods
    // *************************************

    /**
     * Enables external entities to be notified of chain events described in
     * ChainEventListener interface.
     * 
     * @param listener
     */
    public final void addChainEventListener(ChainEventListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "addChainEventListener: " + listener);
        }
        if (null != listener) {
            this.chainEventListeners.add(listener);
        }
    }

    /**
     * Removes a listener from the list of those being informed of chain events
     * on this chain.
     * 
     * @param listener
     */
    public final void removeChainEventListener(ChainEventListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "removeChainEventListener: " + listener);
        }
        if (null != listener) {
            if (!this.chainEventListeners.remove(listener)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Cannot find listener to be removed");
                }
            }
        }
    }

    /**
     * Remove the chain event listeners from this configuration and return them
     * in a new hash set. This method is called during the updateChain
     * in order to move the event listeners from the old config to the new one.
     * 
     * @return ArrayList<ChainEventListener> of chain event listeners
     */
    public Set<ChainEventListener> removeAllChainEventListeners() {
        Set<ChainEventListener> returnListeners = new HashSet<ChainEventListener>(this.chainEventListeners);
        this.chainEventListeners.clear();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "removeAllChainEventListeners", returnListeners);
        }
        return returnListeners;
    }

    /**
     * Set the list of chain event listeners for this chain configuration. This
     * method was originally created to pass along chain event listeners from one
     * chain config to another chain config during the updateChain method.
     * 
     * @param newListeners
     */
    public void setChainEventListeners(Set<ChainEventListener> newListeners) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "removeAllChainEventListeners", newListeners);
        }
        chainEventListeners.clear();
        chainEventListeners.addAll(newListeners);
    }

    /**
     * @param listener
     * @return if the specified listener is referenced in this group.
     */
    public boolean containsChainEventListener(ChainEventListener listener) {
        return this.chainEventListeners.contains(listener);
    }

    /**
     * This method is called when the chain has been initialized. It informs
     * each of the chain event listeners.
     */
    public final void chainInitialized() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainInitialized, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            listener.chainInitialized(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainInitialized");
        }
    }

    /**
     * This method is called when the chain has been started. It informs
     * each of the chain event listeners.
     */
    public final void chainStarted() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainStarted, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            listener.chainStarted(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainStarted");
        }
    }

    /**
     * This method is called when the chain start fails. It informs
     * each of the chain event listeners.
     * 
     * @param attemptsMade
     * @param attemptsLeft
     */
    public final void chainStartFailed(int attemptsMade, int attemptsLeft) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainStartFailed, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            if (listener instanceof RetryableChainEventListener) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling chain retryable chain event listener");
                }
                ((RetryableChainEventListener) listener).chainStartFailed(this, attemptsMade, attemptsLeft);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainStartFailed");
        }
    }

    /**
     * This method is called when the chain has been stopped. It informs
     * each of the chain event listeners.
     */
    public final void chainStopped() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainStopped, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            listener.chainStopped(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainStopped");
        }
    }

    /**
     * Called when the chain is quiesced, which is the first step of
     * stopping the chain.
     */
    public final void chainQuiesced() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainQuiesced, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            listener.chainQuiesced(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainQuiesced");
        }
    }

    /**
     * This method is called when the chain has been destroyed. It informs
     * each of the chain event listeners.
     */
    public final void chainDestroyed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainDestroyed, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            listener.chainDestroyed(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainDestroyed");
        }
    }

    /**
     * This method is called when the chain has been updated. It informs
     * each of the chain event listeners.
     */
    protected void chainUpdated() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "chainUpdated, chain: " + this.name);
        }
        // Clone the list in case one of the event listeners modifies
        // the chain during this iteration.
        for (ChainEventListener listener : chainEventListeners) {
            listener.chainUpdated(this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "chainUpdated");
        }
    }

    /**
     * Access the possible endpoint representation of this chain. This will
     * throw an exception if the chain is not inbound.
     * 
     * @return CFEndPoint
     * @throws ChannelFrameworkException
     */
    public CFEndPoint getEndPoint() throws ChannelFrameworkException {
        if (null == this.endPoint) {
            if (FlowType.INBOUND.equals(this.type)) {
                this.endPoint = new CFEndPointImpl(this);
            } else {
                throw new ChannelFrameworkException(this.name + " is not inbound chain");
            }
        }
        return this.endPoint;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(128);
        result.append("ChainData: ");
        result.append(this.name);
        result.append(" type=").append(this.type.getOrdinal());

        result.append(" channels [");
        for (int i = 0; i < this.channelDataArray.length; i++) {
            result.append(this.channelDataArray[i].getName());
            result.append(',');
        }
        result.setCharAt(result.length() - 1, ']');

        if (this.chainEventListeners != null && !this.chainEventListeners.isEmpty()) {
            result.append(" listeners [");
            for (ChainEventListener listener : chainEventListeners) {
                result.append(listener.getClass());
                result.append(',');
            }
            result.setCharAt(result.length() - 1, ']');
        }

        return result.toString();
    }
}
