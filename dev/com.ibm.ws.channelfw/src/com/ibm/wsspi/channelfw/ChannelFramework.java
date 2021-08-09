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
package com.ibm.wsspi.channelfw;

import java.util.Map;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.CFEndPointCriteria;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.event.Topic;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;

/**
 * The core ChannelFramework API providing for most of the access and
 * functionality.
 * <p>
 * This interface can be catergorized into 11 sections...
 * <p>
 * <ol>
 * <li>Virtual Connection Factory methods</li>
 * <li>Channel Factory Configuration methods</li>
 * <li>Channel Configuration methods</li>
 * <li>Channel Life cycle methods</li>
 * <li>Chain Configuration methods</li>
 * <li>Chain life cycle methods</li>
 * <li>Chain Event Listener Methods</li>
 * <li>Chain Group Configuration methods</li>
 * <li>Chain group life cycle methods 10</li>
 * <li>Chain Group registration methods</li>
 * <li>Channel Framework lifecycle methods</li>
 * </ol>
 * <p>
 * The Virtual Connection Factories are the connection lifecycle administrators
 * within the channel framework. the methods used to get these are specific to
 * Inbound and Outbound connections while these connections have different
 * internal and external meanings.
 * <p>
 * The Channel, Chain, and Chain Group Configuration methods all have common
 * add, remove, update, and get methods.
 * <p>
 * The Channel, Chain, and Chain Group life cycle methods allow the starting,
 * stopping, and initializing of each of these seperate pieces.
 * <p>
 * The Chain Event Listener methods provide for a way to register to listen for
 * configuration and administration events that occur during runtime to Chains
 * and Chain Groups.
 * <p>
 * The Channel Framework Lifecycle essentially just provides for a destroy to
 * clean up everything. Normally, the channel framework is a singleton and this
 * need not be called.
 */
public interface ChannelFramework {

    /** Event topic for stopping a chain */
    Topic EVENT_STOPCHAIN = new Topic("com/ibm/websphere/channelfw/STOPCHAIN");
    /** Event property for a chain name value (String) */
    String EVENT_CHAINNAME = "ChainName";
    /** Event property for a channel name value (String) */
    String EVENT_CHANNELNAME = "ChannelName";
    /** Event property for a time value (Long) */
    String EVENT_TIME = "Time";

    // ****************************************
    // Virtual Connection Factory Methods
    // ****************************************

    /**
     * Get a virtual connection factory for this outbound chain.
     * <p>
     * As an alternative, this method may be used to get outbound virtual
     * connections for a subset of the channels in an existing outbound chain. In
     * this case, the parameter should be the name of an internal channel in an
     * existing outbound chain. All channels below the one specified will be
     * included in the virtual connection. This capability allows outbound
     * channels the ability to multiplex outbound connections.
     * 
     * @param chainName
     *            The name of the chain configured to get an Outbound Virtual
     *            Connection Factory for.
     * @return VirtualConnectionFactory
     * @throws ChannelException
     *             Indicates a problem within a related channel.
     * @throws ChainException
     *             Indicates a problem within a related chain.
     */
    VirtualConnectionFactory getOutboundVCFactory(String chainName) throws ChannelException, ChainException;

    /**
     * Get an inbound virtual connection factory for general use.
     * <p>
     * All connector channels wishing to create and push new VirtualConnections up
     * a Chain need to fetch this VirtualConnectionFactory interface.
     * 
     * @return VirtualConnectionFactory
     */
    VirtualConnectionFactory getInboundVCFactory();

    // ****************************************
    // Channel Factory Configuration Methods.
    // ****************************************

    /**
     * This method registers common properties that are to be shared by all
     * instances of a channel generated by a given channel factory. This method
     * will instantiate the given factory if it does not already exist.
     * 
     * @param factoryType
     *            ChannelFactory class for which <em>properties</em> are
     *            to be associated.
     * @param properties
     *            Common properties to be shared by all channel instances
     *            generated by an instance of <em>factoryType</em>.
     * @return ChannelFactoryData
     * @throws ChannelFactoryException
     */
    ChannelFactoryData updateAllChannelFactoryProperties(Class<?> factoryType, Map<Object, Object> properties) throws ChannelFactoryException;

    /**
     * This method updates a single ChannelFactory property. Note: the
     * implementation of the
     * channel factory may reject this property update.
     * 
     * @param factoryType
     *            ChannelFactory class for which <em>properties</em> are
     *            to be associated.
     * @param key
     *            Name or key of the property.
     * @param value
     *            Value to set this property to.
     * @return ChannelFactoryData
     * @throws ChannelFactoryException
     *             Thrown if the ChannelFactory is not found or there was a
     *             problem updating this property.
     */
    ChannelFactoryData updateChannelFactoryProperty(Class<?> factoryType, Object key, Object value) throws ChannelFactoryException;

    /**
     * This method will fetch an existing channel factory's data from the
     * framework.
     * 
     * @param type
     *            The class of the factory being asked for.
     * @return ChannelFactoryData
     * @throws ChannelFactoryException
     *             Thrown if the channel factory is not found
     */
    ChannelFactoryData getChannelFactory(Class<?> type) throws ChannelFactoryException;

    /**
     * Query the default chain quiesce timeout to use.
     * 
     * @return long
     */
    long getDefaultChainQuiesceTimeout();

    // ****************************************
    // Channel Configuration Methods.
    // ****************************************

    /**
     * This method creates a named channel data object and instantiates an
     * instance of its
     * channel factory (if one does not already exist).
     * 
     * @param channelName
     *            Unique name of a channel. This name cannot already exist for any
     *            channel type.
     * @param factoryType
     *            Class of the Factory for this channel type.
     * @param properties
     *            Set of properties for this channel.
     * @param weight
     *            Discriminator weight for use in inbound chains.
     * @return ChannelData If a channel by the parameter name already exists, then
     *         it will be returned.
     * @throws ChannelException
     *             indicates a problem within a related channel
     */
    ChannelData addChannel(String channelName, Class<?> factoryType, Map<Object, Object> properties, int weight) throws ChannelException;

    /**
     * This method creates a named channel data objet and instantiates an instance
     * of its
     * channel factory (if one does not already exist).
     * 
     * @param channelName
     *            Unique name of a channel. This name cannot already exist for any
     *            channel type.
     * @param factoryType
     *            Class of the Factory for this channel type.
     * @param properties
     *            Set of properties for this channel.
     * @return ChannelData described by parameters. If a channel by the parameter
     *         name already
     *         exists, then it will be returned.
     * @throws ChannelException
     *             indicates a problem within a related channel
     */
    ChannelData addChannel(String channelName, Class<?> factoryType, Map<Object, Object> properties) throws ChannelException;

    /**
     * This will remove the channel configuration. If the configuration is in use
     * by the framework,
     * an exception will be thrown. In order to remove a channel config, it must
     * not be in use
     * by the runtime. If this configuration is in use by a chain configuration,
     * then the chain
     * configuration will be removed as well. If the chain is in use by a chain
     * group, the group will be updated.
     * 
     * @param channelName
     *            Name of channel to be removed.
     * @return Channel configuration that was removed.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    ChannelData removeChannel(String channelName) throws ChannelException, ChainException;

    /**
     * This will update the new discrimination weight to be used when this channel
     * is running.
     * 
     * @param channelName
     *            Name of channel to be updated.
     * @param discriminationWeight
     *            New weight.
     * @return ChannelData Updated object representing the input channel name.
     * @throws ChannelException
     */
    ChannelData updateChannelWeight(String channelName, int discriminationWeight) throws ChannelException;

    /**
     * Update a single property that is use by the input named channel. If the
     * property
     * already exists, it will overlay what is there. Note: the implementation of
     * the
     * channel may reject this property update.
     * 
     * @param channelName
     *            Name of channel to be updated.
     * @param propertyKey
     *            Name or key of the property to be updated.
     * @param propertyValue
     *            New value of the property.
     * @return ChannelData Updated object representing the input channel name.
     * @throws ChannelException
     */
    ChannelData updateChannelProperty(String channelName, Object propertyKey, Object propertyValue) throws ChannelException;

    /**
     * Update the entire set of properties in use by the input named channel.
     * Note: the
     * implementation of the channel may reject this property update.
     * 
     * @param channelName
     *            Name of channel to be updated.
     * @param newProperties
     *            Property map for this channel.
     * @return ChannelData Updated channel data object.
     * @throws ChannelException
     */
    ChannelData updateAllChannelProperties(String channelName, Map<Object, Object> newProperties) throws ChannelException;

    /**
     * This method returns a channel data object for a specific named channel
     * configuration,
     * 
     * @param channelName
     *            Name of channel to fecth configuration information.
     * @return ChannelData Channel configuration requested, or null if it was not
     *         found.
     */
    ChannelData getChannel(String channelName);

    /**
     * This method will return the entire of set of channels that have been
     * added via this interface's addChannel methods.
     * 
     * @return array of channel data objects, or an empty array if none exist
     */
    ChannelData[] getAllChannels();

    /**
     * This method will return the entire of set of channels that have been
     * added via this interface's addChannel methods and are in use by the
     * runtime.
     * 
     * @return array of channel data objects, or an empty array if none exist
     */
    ChannelData[] getRunningChannels();

    /**
     * This method will return the port on which the provided inbound chain is
     * listening on.
     * 
     * @param chainName
     *            Name of the inbound chain.
     * @return the listening port of the inbound chain
     * @throws ChainException
     *             if the provided chain name doesn't exist, isn't
     *             running, or isn't inbound.
     */
    int getListeningPort(String chainName) throws ChainException;

    /**
     * This method will return the host on which the provided inbound chain is
     * listening on.
     * 
     * @param chainName
     *            Name of the inbound chain.
     * @return the listening host of the inbound chain
     * @throws ChainException
     *             if the provided chain name doesn't exist, isn't
     *             running, or isn't inbound.
     */
    String getListeningHost(String chainName) throws ChainException;

    // ****************************************
    // Chain Configuration Methods
    // ****************************************

    /**
     * This method will create a specific named chain configuration and the
     * structures
     * to prepare this chain to be created. It assumes that channel configurations
     * already exist for the names in the channel list. If the chain name passed
     * in already
     * exists, nothing is changed and the existing chain configuration is
     * returned.
     * 
     * @param chainName
     *            unique name of the chain.
     * @param chainType
     *            Inbound or Outbound Flowtype.
     * @param channelList
     *            List of channel names.
     * @return ChainData Configuration of the new chain. If the chain already
     *         exists, it will be returned.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    ChainData addChain(String chainName, FlowType chainType, String[] channelList) throws ChannelException, ChainException;

    /**
     * This will remove the chain's configuration from the runtime. If it is
     * in use by any chains in the runtime, an exception will be thrown. All
     * chains using this configuration must be destroyed before the
     * configuration may be removed. In addition, all channels in these chains
     * will be destroyed if not in use by another chain. If the chain
     * configuration is included in a chain group, it will be removed from
     * the group. If this leaves the group with no chain references, the
     * group will NOT be removed.
     * 
     * @param chainName
     *            Name of Chain to remove.
     * @return ChainData Object of removed Chain.
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    ChainData removeChain(String chainName) throws ChainException;

    /**
     * This will remove the chain's configuration from the runtime. If it is
     * in use by any chains in the runtime, an exception will be thrown. All
     * chains using this configuration must be destroyed before the
     * configuration may be removed. In addition, all channels in these chains
     * will be destroyed if not in use by another chain. If the chain
     * configuration is included in a chain group, it will be removed from
     * the group. If this leaves the group with no chain references, the
     * group will NOT be removed.
     * 
     * @param chain
     *            to remove.
     * @return ChainData Object of removed Chain.
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    ChainData removeChain(ChainData chain) throws ChainException;

    /**
     * This method updates a chain configuration with a new set of channels. If it
     * is
     * in use by a runtime chain, an exception will be thrown. In order to update
     * the chain
     * configuration, all runtime chains using the configuration must be
     * destroyed.
     * 
     * @param chainName
     *            Name of Chain to update.
     * @param channelList
     *            New List of channel names.
     * @return ChainData Updated chain configuration requested for update.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    ChainData updateChain(String chainName, String[] channelList) throws ChannelException, ChainException;

    /**
     * This method gets a specific named chain configuration object from the
     * runtime.
     * 
     * @param chainName
     *            Name of Chain to fetch,
     * @return ChainData Object requested, or null if not found.
     */
    ChainData getChain(String chainName);

    /**
     * Fetches all chain data objects in the framework.
     * 
     * @return array of chain data objects.
     */
    ChainData[] getAllChains();

    /**
     * Fetch all chains in the framework which include the input channel.
     * 
     * @param channelName
     *            Channel name to filter the list by.
     * @return ChainData[]
     * @throws ChannelException
     *             error found in input
     */
    ChainData[] getAllChains(String channelName) throws ChannelException;

    /**
     * Fetch all chains in the framework that include channels created
     * from the input factory class.
     * 
     * @param factoryClass
     *            Class to filter the list of Chains by.
     * @return ChainData[]
     * @throws InvalidChannelFactoryException
     *             error found in input
     */
    ChainData[] getAllChains(Class<?> factoryClass) throws InvalidChannelFactoryException;

    /**
     * Query whether the input chain name points to a chain that is running.
     * 
     * @param chainName
     * @return boolean
     */
    boolean isChainRunning(String chainName);

    /**
     * Query whether the input chain reference points to a running chain.
     * 
     * @param chain
     * @return boolean
     */
    boolean isChainRunning(ChainData chain);

    /**
     * Fetch all chains in the framework that are in the runtime.
     * 
     * @return array of chain data objects
     */
    ChainData[] getRunningChains();

    /**
     * Fetch all chains in the framework that are in the runtime
     * and include the input channel.
     * 
     * @param channelName
     *            Name of channel to filter list by.
     * @return array of chain data objects
     * @throws ChannelException
     *             error found in input
     */
    ChainData[] getRunningChains(String channelName) throws ChannelException;

    /**
     * Fetch all chains in the framework that are in the runtime and include
     * the input internal channel.
     * 
     * @param internalChannelName
     *            referring to a name only known by channel implementation and the
     *            framework
     * @return array of chain data objects
     * @throws ChannelException
     *             error found in input
     */
    ChainData[] getInternalRunningChains(String internalChannelName) throws ChannelException;

    /**
     * Fetch all chains in the framework that are in the runtime
     * and include channels created from the input factory class.
     * 
     * @param factoryClass
     *            Class name to filter the list by.
     * @return array of chain data objects
     * @throws InvalidChannelFactoryException
     *             error found in input
     */
    ChainData[] getRunningChains(Class<?> factoryClass) throws InvalidChannelFactoryException;

    // ****************************************
    // Chain Life Cycle Methods
    // ****************************************

    /**
     * Initialize the chain in the runtime.
     * <p>
     * This will : 1) find or create the channels in the chain 3) create the chain
     * 2) call the init method on each channel
     * 
     * @param chainName
     *            Chain name to initialize.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void initChain(String chainName) throws ChannelException, ChainException;

    /**
     * Start the chain in the runtime.
     * <p>
     * This will: 1) initialize the chain if neccessary 2) start each channel (if
     * necessary) 3) start the chain. 4) add the channels to the discriminator
     * group of previous channel (if its an inbound chain)
     * 
     * @param chainName
     *            Name of chain to start.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void startChain(String chainName) throws ChannelException, ChainException;

    /**
     * Start the chain in the runtime.
     * <p>
     * This will: 1) initialize the chain if neccessary 2) start each channel (if
     * necessary) 3) start the chain. 4) add the channels to the discriminator
     * group of previous channel (if its an inbound chain)
     * 
     * @param chain
     *            to start.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void startChain(ChainData chain) throws ChannelException, ChainException;

    /**
     * Stop the Chain in the runtime.
     * <p>
     * If millisec parameter is set to zero, this will 1) remove the channel from
     * the previous channel's discriminator if needed. 2) stop the channels if
     * they are not in other channel's chains. Otherwise, it will inform the chain
     * (and underlying channels) that a stop will be happening in millisec time.
     * This serves as a quiesce function.
     * 
     * @param chainName
     *            Name of chain to stop.
     * @param millisec
     *            Time until the stop will be asserted.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void stopChain(String chainName, long millisec) throws ChannelException, ChainException;

    /**
     * Stop the Chain in the runtime.
     * <p>
     * If millisec parameter is set to zero, this will 1) remove the channel from
     * the previous channel's discriminator if needed. 2) stop the channels if
     * they are not in other channel's chains. Otherwise, it will inform the chain
     * (and underlying channels) that a stop will be happening in millisec time.
     * This serves as a quiesce function.
     * 
     * @param chain
     *            to stop.
     * @param millisec
     *            Time until the stop will be asserted.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void stopChain(ChainData chain, long millisec) throws ChannelException, ChainException;

    /**
     * Destroy of chain in the Runtime.
     * <p>
     * This will release the reference to the chains and remove it from the
     * initialized state.
     * 
     * @param chainName
     *            Name of Chain to destroy.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void destroyChain(String chainName) throws ChannelException, ChainException;

    /**
     * Destroy of chain in the Runtime.
     * <p>
     * This will release the reference to the chains and remove it from the
     * initialized state.
     * 
     * @param chain
     *            to destroy.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void destroyChain(ChainData chain) throws ChannelException, ChainException;

    // ****************************************
    // Chain Event Listener Methods
    // ****************************************

    /**
     * This adds a listener for chain lifecycle events.
     * 
     * @param cel
     *            Chain event listener instance
     * @param chainName
     *            Name of the chain for which this listener will be registered.
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void addChainEventListener(ChainEventListener cel, String chainName) throws ChainException;

    /**
     * This removes an event listener. This is referenced via the hashcode.
     * 
     * @param cel
     *            Chain event listener instance
     * @param chainName
     *            Name of the chain for which the listener will be unregistered.
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    void removeChainEventListener(ChainEventListener cel, String chainName) throws ChainException;

    /**
     * Add a chainEventListener to a specific group of chains.
     * 
     * @param cel
     *            Chain event listener instance
     * @param groupName
     *            Name of ChainGroup.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    void addGroupEventListener(ChainEventListener cel, String groupName) throws ChainGroupException;

    /**
     * Remove the ChainEventListener from a group.
     * 
     * @param cel
     *            Chain event listener instance
     * @param groupName
     *            Name of Group.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    void removeGroupEventListener(ChainEventListener cel, String groupName) throws ChainGroupException;

    // ****************************************
    // Chain Group Methods
    // ****************************************

    /**
     * This method creates a chain group and adds the following chains to it.
     * If the chain group already exists, it will be updated with the input
     * chainNames list.
     * 
     * @param groupName
     *            Unique name of Group.
     * @param chainNames
     *            Names of Chains to add.
     * @return data objects representing the chain group
     * @throws ChainGroupException
     *             if input group name is null.
     * @throws ChainException
     *             indicates a problem within a related chain.
     */
    ChainGroupData addChainGroup(String groupName, String[] chainNames) throws ChainException, ChainGroupException;

    /**
     * This method removes a chain group from the framework.
     * 
     * @param groupName
     *            Name of group to remove.
     * @return data objects representing the chain group
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    ChainGroupData removeChainGroup(String groupName) throws ChainGroupException;

    /**
     * This method modifies the chain configurations that are managed in the
     * group. If
     * the group cannot be found, it is added.
     * 
     * @param groupName
     *            Name of group.
     * @param chainnames
     *            Names of all Chains in Group.
     * @return data objects representing the chain group
     * @throws ChainException
     *             indicates a problem within a related chain.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    ChainGroupData updateChainGroup(String groupName, String[] chainnames) throws ChainException, ChainGroupException;

    /**
     * This gets a data object holding all the chains within this group
     * 
     * @param groupName
     * @return data objects representing the chain group, or null if not found
     */
    ChainGroupData getChainGroup(String groupName);

    /**
     * This adds the input named chain to the input named group.
     * 
     * @param groupName
     *            name of the group to which the chain should be added.
     * @param chainName
     *            name of the chain to be added to the chain group
     * @return chain group data object resulting after the change
     * @throws ChainGroupException
     *             if the input group is invalid
     * @throws ChainException
     *             if the input chain is invalid
     */
    ChainGroupData addChainToGroup(String groupName, String chainName) throws ChainGroupException, ChainException;

    /**
     * This removes in the input named chain from the input named group.
     * 
     * @param groupName
     *            name of the group from which the chain should be removed
     * @param chainName
     *            name of the chain to be removed from the chain group
     * @return chain group data object resulting after the change
     * @throws ChainGroupException
     *             if the input group is invalid
     * @throws ChainException
     *             if the input chain is invalid
     */
    ChainGroupData removeChainFromGroup(String groupName, String chainName) throws ChainGroupException, ChainException;

    /**
     * Retrieves the entire set of chain groups in the framework.
     * 
     * @return chain group data array
     */
    ChainGroupData[] getAllChainGroups();

    /**
     * Retrieves all chain group in the framework which contain the named chain.
     * 
     * @param chainName
     *            name of the chain which must exist in all returned chain groups
     * @return chain group data array
     * @throws ChainException
     */
    ChainGroupData[] getAllChainGroups(String chainName) throws ChainException;

    // ****************************************
    // Chain Group Life Cycle Methods
    // ****************************************

    /**
     * This method is used to initialize a group of chains.
     * 
     * @param groupName
     *            Name of Group to initialize.
     * @return list of runtime chain configurations that were initialized.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    ChainData[] initChainGroup(String groupName) throws ChannelException, ChainException, ChainGroupException;

    /**
     * This method is used to strart all chains within a group.
     * 
     * @param groupName
     *            Name of group to start.
     * @return list of runtime chain configurations that were started.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    ChainData[] startChainGroup(String groupName) throws ChannelException, ChainException, ChainGroupException;

    /**
     * This method is used to stop all chains within a group that can be stopped.
     * 
     * @param groupName
     *            Name of group to stop.
     * @param millisec
     *            time until the stop will be asserted
     * @return list of runtime chain configurations that were stopped.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    ChainData[] stopChainGroup(String groupName, long millisec) throws ChannelException, ChainException, ChainGroupException;

    /**
     * This method is used to destroy all chains within a group that can be
     * destroyed.
     * <p>
     * This does not destroy the group but just destroy each chain.
     * 
     * @param groupName
     *            Name of the group of chains to fetch each and destroy.
     * @return list of runtime chain configurations that were destroyed.
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    ChainData[] destroyChainGroup(String groupName) throws ChannelException, ChainException, ChainGroupException;

    // ****************************************
    // Channel Framework Life Cycle Methods
    // ****************************************

    /**
     * Destroy the channel framework and release its resources. This will
     * stop, destroy, and clean up the resources used by the channel framework.
     * 
     * @throws ChannelException
     *             indicates a problem within a related channel
     * @throws ChainException
     *             indicates a problem within a related chain.
     * @throws ChainGroupException
     *             if problem is identified withing a related chain group.
     */
    void destroy() throws ChannelException, ChainException, ChainGroupException;

    // **************************************
    // Channel Framework Services
    // **************************************

    /**
     * Register a service of any kind for later reference. If the service already
     * exists,
     * then it will be overwritten.
     * 
     * @param clazz
     *            Class name of Service.
     * @param service
     *            Service obect to register.
     */
    void registerService(Class<?> clazz, Object service);

    /**
     * Deregister a service, removing it from future reference.
     * 
     * @param clazz
     *            Class name of service.
     * @return the service that was removed, or null if it doesn't exist.
     */
    Object deregisterService(Class<?> clazz);

    /**
     * Look up a service that was previously registered.
     * 
     * @param clazz
     *            Class name of service.
     * @return registered service or null if it does not exist.
     */
    Object lookupService(Class<?> clazz);

    /**
     * Register a factory class under the meta-name for later usage when
     * creating channels.
     * 
     * @param name
     * @param factory
     */
    void registerFactory(String name, Class<? extends ChannelFactory> factory);

    /**
     * De-register the given factory meta-name.
     * 
     * @param name
     */
    void deregisterFactory(String name);

    /**
     * Allow a factory provider to register several factories at once.
     * 
     * @param provider ChannelFactoryProvider
     * @see #registerFactory(String, Class)
     */
    void registerFactories(ChannelFactoryProvider provider);

    /**
     * Allow a factory provider to deregister several factories at once.
     * 
     * @param provider ChannelFactoryProvider
     * @see #deregisterFactory(String, Class)
     */
    void deregisterFactories(ChannelFactoryProvider provider);

    /**
     * Query whether any current factory has been registered for the input
     * meta-name.
     * 
     * @param name
     * @return String, null if nothing registered with that name
     */
    Class<? extends ChannelFactory> lookupFactory(String name);

    /**
     * The purpose of this method is to choose the appropriate CFEndPoint from
     * the input list based on the input criteria. A set of EndPoints will be
     * provided along with a descriptor providing details of the request.
     * <P>
     * The logic first searches to match the name provided in the criteria to the
     * name of one of the endpoints provided. If a name is provided and no
     * matching endpoint is found, a null is returned.
     * <P>
     * If the name in the criteria is set to null, then a different logical path
     * is taken. This criteria's isSSLRequired flag will be used to match one of
     * the endpoints. If one doesn't exist that matches, null will be returned. In
     * cases where multiple endpoints match the criteria, favor will be given to
     * endpoints that are local only. This implies they are in-process.
     * 
     * @param endPointList
     *            list of EndPoints to choose from
     * @param criteria
     *            information to help choose the CFEndPoint
     * @return chosen CFEndPoint, or null if no matches possible
     */
    CFEndPoint determineBestEndPoint(CFEndPoint[] endPointList, CFEndPointCriteria criteria);

    /**
     * The purpose of this method is to choose the appropriate CFEndPoints from
     * the input list based on the input criteria. A set of EndPoints will be
     * provided along with a descriptor providing details of the request.
     * <P>
     * The matching criteria and logic used are the same as described in
     * determineBestEndPoint method.
     * 
     * @param endPointList
     *            list of EndPoints to choose from
     * @param criteria
     *            information to help choose the CFEndPoint
     * @return chosen CFEndPoints, or null if no matches possible
     */
    CFEndPoint[] getEndPoints(CFEndPoint[] endPointList, CFEndPointCriteria criteria);

    /**
     * Fetch the CFEndPoint representation of the given inbound chain. This will
     * return null if no endpoint is defined for the chain or the chain is not
     * found. An exception is thrown if the target chain is outbound.
     * 
     * @param chainName
     * @return CFEndPoint, null if not found
     * @throws ChannelFrameworkException
     */
    CFEndPoint getEndPoint(String chainName) throws ChannelFrameworkException;

}
