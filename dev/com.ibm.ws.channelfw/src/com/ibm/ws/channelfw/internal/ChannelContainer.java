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

import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.wsspi.channelfw.Channel;

/**
 * This class is used by the channel framework to track the state of channels
 * in the runtime.
 */
public class ChannelContainer {

    // Instance variables
    /**
     * Channel in this container
     */
    private Channel channel = null;
    /**
     * state of this channel
     */
    private RuntimeState state = null;
    /**
     * map of chains this channel is in.
     */
    private Map<String, Chain> chainMap = null;
    /**
     * channel data of this child channel
     */
    private ChildChannelDataImpl channelData = null;

    /**
     * Constructor.
     * 
     * @param inputChannel
     *            represented by this container
     * @param inputData
     *            related to the channel
     */
    public ChannelContainer(Channel inputChannel, ChildChannelDataImpl inputData) {
        this.channel = inputChannel;
        this.channelData = inputData;
        this.state = RuntimeState.INITIALIZED;
        this.chainMap = new HashMap<String, Chain>();
    }

    /**
     * get the Channel instance this chain is associated with
     * 
     * @return Channel
     */
    public Channel getChannel() {
        return this.channel;
    }

    /**
     * return state of this Channel
     * 
     * @return RuntimeState
     */
    public RuntimeState getState() {
        return this.state;
    }

    /**
     * return Map of chains this channel is in
     * 
     * @return Map
     */
    public Map<String, Chain> getChainMap() {
        return this.chainMap;
    }

    /**
     * return this ChildChannelData
     * 
     * @return ChildChannelDataImpl
     */
    public ChildChannelDataImpl getChannelData() {
        return this.channelData;
    }

    /**
     * Update the chain map with a reference to the input chain.
     * 
     * @param chain
     */
    public void addChainReference(Chain chain) {
        this.chainMap.put(chain.getName(), chain);
    }

    /**
     * Remove the reference to the input chain in the chain map.
     * 
     * @param chainName
     */
    public void removeChainReference(String chainName) {
        this.chainMap.remove(chainName);
    }

    /**
     * Update the state of the channel.
     * 
     * @param inputState
     */
    public void setState(RuntimeState inputState) {
        this.state = inputState;
    }
}
