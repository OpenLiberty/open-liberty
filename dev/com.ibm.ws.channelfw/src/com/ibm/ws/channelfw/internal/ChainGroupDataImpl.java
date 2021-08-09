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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.exception.ChainException;

/**
 * This class maintains all the ChainData objects in a chain group
 */
public class ChainGroupDataImpl implements ChainGroupData {

    /** Serialization ID string */
    private static final long serialVersionUID = 3062239582748589190L;

    /** Trace service */
    private static final TraceComponent tc = Tr.register(ChainGroupDataImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Name of this group.
     */
    private String name = null;
    /**
     * List of chains in this group.
     */
    private ChainData[] chainArray = null;

    /**
     * Listeners for this group.
     */
    private transient List<ChainEventListener> chainEventListeners = null;

    /**
     * Reference to the framework for access to APIs.
     */
    private transient ChannelFrameworkImpl framework = null;

    /**
     * Constructor
     * 
     * @param groupName
     * @param groupChains
     * @param frameworkRef
     */
    public ChainGroupDataImpl(String groupName, ChainData[] groupChains, ChannelFrameworkImpl frameworkRef) {
        this.name = groupName;
        this.chainArray = groupChains;
        this.chainEventListeners = new ArrayList<ChainEventListener>(0);
        this.framework = frameworkRef;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChainGroupData#getName()
     */
    public String getName() {
        return this.name;
    }

    /*
     * @see com.ibm.websphere.channelfw.ChainGroupData#getChains()
     */
    public ChainData[] getChains() {
        return this.chainArray;
    }

    /*
     * @see
     * com.ibm.websphere.channelfw.ChainGroupData#containsChain(java.lang.String)
     */
    public boolean containsChain(String target) {
        for (ChainData chain : getChains()) {
            if (chain.getName().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Overlay the list of chains managed by this group.
     * 
     * @param newChainArray
     */
    public void setChains(ChainData[] newChainArray) {
        this.chainArray = newChainArray;
    }

    /**
     * Adds the input chain to the group. All chain event listeners associated
     * with the group are also added to the chain.
     * 
     * @param newChain
     * @throws ChainException
     *             if chain already exists in group.
     */
    public void addChain(ChainData newChain) throws ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addChain: " + newChain.getName());
        }

        if (containsChain(newChain.getName())) {
            ChainException e = new ChainException("Chain already exists: " + newChain.getName());
            FFDCFilter.processException(e, getClass().getName() + ".addChain", "116", this, new Object[] { newChain });
            throw e;
        }

        // Create a chain array that is one element bigger than the current one.
        int currentLength = this.chainArray.length;
        ChainData[] newChains = new ChainData[currentLength + 1];
        // Copy the existing elements to the new array.
        System.arraycopy(getChains(), 0, newChains, 0, currentLength);
        // Add the new chain to the end of the new list.
        newChains[currentLength] = newChain;
        // Update this group's chain list
        setChains(newChains);
        // Add all group listeners to this chain. If some already exist,
        // this doesn't break anything.
        for (ChainEventListener listener : getChainEventListeners()) {
            ((ChainDataImpl) newChain).addChainEventListener(listener);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addChain");
        }
    }

    /**
     * Removes the input chain from the group. All group chain event listeners all
     * also removed from the chain unless the chain is in another group which is
     * associated with the listener.
     * 
     * @param inputChain
     *            chain object to be removed.
     * @throws ChainException
     *             if chain doesn't exist in group.
     */
    public void removeChain(ChainData inputChain) throws ChainException {
        String chainname = inputChain.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeChain: " + chainname);
        }

        if (!containsChain(chainname)) {
            ChainException e = new ChainException("Unable to find chain: " + chainname);
            FFDCFilter.processException(e, getClass().getName() + ".removeChain", "157", this, new Object[] { inputChain, this.framework });
            throw e;
        }
        // Create a chain array that is one element smaller than the current one.
        int currentLength = this.chainArray.length;
        ChainData[] newChains = new ChainData[currentLength - 1];
        // Copy all but the input chain into the new array.
        for (int i = 0; i < currentLength; i++) {
            if (!chainname.equals(this.chainArray[i].getName())) {
                newChains[i] = this.chainArray[i];
            }
        }
        // Update this group's chain list.
        setChains(newChains);

        // Remove group associated listeners from the chain, only if not
        // associated with other group.
        for (ChainEventListener listener : getChainEventListeners()) {
            removeListenerFromChain(listener, inputChain);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "removeChain");
        }
    }

    /**
     * Search the group for the input chain. If found, update it. Otherwise do
     * nothing.
     * 
     * @param inputChain
     */
    public void updateChain(ChainData inputChain) {
        String chainname = inputChain.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateChain: " + chainname);
        }

        // Find the chain in the group.
        for (int i = 0; i < this.chainArray.length; i++) {
            if (chainname.equals(this.chainArray[i].getName())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Updating chain " + chainname + " in group " + getName());
                }
                // Found the chain. Update the array with the new data.
                this.chainArray[i] = inputChain;
                break;
            }
        }
    }

    // *************************************
    // Event Listener Methods
    // *************************************

    /**
     * @return number of listeners associated with this group.
     */
    public int getNumChainEventListeners() {
        return this.chainEventListeners.size();
    }

    /**
     * @return the chain event listeners of this group.
     */
    public List<ChainEventListener> getChainEventListeners() {
        return this.chainEventListeners;
    }

    /**
     * @param listener
     * @return if the specified listener is referenced in this group.
     */
    public boolean containsChainEventListener(ChainEventListener listener) {
        return this.chainEventListeners.contains(listener);
    }

    /**
     * Remove the listener from the chain, but only if the chain is not associated
     * with
     * the listener through another group.
     * 
     * @param listener
     *            chain event listener to be removed from the chain
     * @param chainData
     *            chain from which to remove chain event listener
     */
    private void removeListenerFromChain(ChainEventListener listener, ChainData chainData) {
        ChainGroupData[] otherGroups = null;
        boolean foundOtherGroupWithListener = false;
        // Extract the groups that this chain is involved in.
        try {
            otherGroups = this.framework.getAllChainGroups(chainData.getName());
            foundOtherGroupWithListener = false;
            int i = 0;
            for (i = 0; i < otherGroups.length; i++) {
                if (((ChainGroupDataImpl) otherGroups[i]).containsChainEventListener(listener)) {
                    // Chain is in another group that has this listener.
                    foundOtherGroupWithListener = true;
                    break;
                }
            }
            if (!foundOtherGroupWithListener) {
                // Chain is NOT in another group that has this listener, so listener can
                // be removed.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Removing listener from chain config, " + chainData.getName());
                }
                ((ChainDataImpl) chainData).removeChainEventListener(listener);
            } else {
                // Chain was found in another group with this listener
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found chain " + chainData.getName() + " in another group, " + otherGroups[i].getName());
                }
            }
        } catch (ChainException e) {
            // This shouldn't ever happen, but in case it does, we know no refs were
            // found.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Chain not found in config: " + chainData.getName() + ", but will remove listener.");
            }
            ((ChainDataImpl) chainData).removeChainEventListener(listener);
        }
    }

    /**
     * Method addChainEventListener. Enables external entities to be
     * notified of chain events on each of the chains in the group
     * described in ChainEventListener interface.
     * 
     * @param listener
     */
    public final void addChainEventListener(ChainEventListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addChainEventListener: " + listener);
        }
        if ((null != listener) && (!getChainEventListeners().contains(listener))) {
            // Add the listener to the set monitored by this group.
            getChainEventListeners().add(listener);
            // Add the listener to each of the chains in this group.
            // Extract the chain data array from the group data object
            for (ChainData chain : getChains()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding listener to chain, " + chain.getName());
                }
                ((ChainDataImpl) chain).addChainEventListener(listener);
            }
        }
    }

    /**
     * Method removeChainEventListener. Removes a listener from the list
     * of those being informed of chain events on this chain. The listener is also
     * removed from each chain in the group unless the chain is in another group
     * which is associated with the listener.
     * 
     * @param listener
     */
    public final void removeChainEventListener(ChainEventListener listener) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeChainEventListener: " + listener);
        }

        if (null != listener) {
            // Remove the listener from the list monitored by this group.
            if (!getChainEventListeners().remove(listener)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Listener " + listener + " was not found in list monitored by group " + getName());
                }
            }
            // Remove the listener from each of the chains in this group.
            for (ChainData chain : getChains()) {
                removeListenerFromChain(listener, chain);
            }
        }
    }

    // *************************************
    // Debug Methods
    // *************************************

    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("ChainGroupData:\r\n\tname = ");
        result.append(getName());
        result.append("\r\n\tchainconfigs:");

        for (ChainData chain : getChains()) {
            result.append("\r\n\t\t");
            result.append(chain.getName());
        }

        return result.toString();
    }
}
