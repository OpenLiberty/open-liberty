/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.io.async.IAsyncProvider.AsyncIOHelper;
import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.CFEndPointCriteria;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChainGroupData;
import com.ibm.websphere.channelfw.ChainStartMode;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.ChannelUtils;
import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.channelfw.RegionType;
import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.chains.Chain;
import com.ibm.ws.channelfw.internal.chains.EndPointMgrImpl;
import com.ibm.ws.channelfw.internal.chains.InboundChain;
import com.ibm.ws.channelfw.internal.chains.OutboundChain;
import com.ibm.ws.channelfw.internal.chains.StopChainTask;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.channelfw.BoundRegion;
import com.ibm.wsspi.channelfw.ChainEventListener;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.CrossRegionSharable;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChainGroupException;
import com.ibm.wsspi.channelfw.exception.ChainTimerException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;
import com.ibm.wsspi.channelfw.exception.IncoherentChainException;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelNameException;
import com.ibm.wsspi.channelfw.exception.InvalidRuntimeStateException;
import com.ibm.wsspi.channelfw.exception.InvalidWeightException;
import com.ibm.wsspi.channelfw.exception.RetryableChannelException;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

/**
 * This class is the implementation of the configuration and runtime interface
 * for modifying the Channel Framework.
 */
public class ChannelFrameworkImpl implements ChannelFramework, FFDCSelfIntrospectable {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(ChannelFrameworkImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);
    private static final String nl = System.getProperty("line.separator");

    /**
     * Set of all channel configurations.
     * This map includes parent channel data objects only.
     */
    private final Map<String, ChannelData> channelDataMap;
    /**
     * Set of all channelRunningMap instantiated in runtime.
     * This map includes child channel data objects only.
     */
    private final Map<String, ChannelContainer> channelRunningMap;
    /**
     * Set of all chain configurations.
     * The channel data arrays within are parents, not children.
     */
    private final Map<String, ChainDataImpl> chainDataMap;
    /**
     * Set of all chains instantiated in runtime.
     * The channel data arrays within are children, not parents.
     */
    private final Map<String, Chain> chainRunningMap;
    /**
     * Set of channel factories.
     * This stores ChannelFactoryDataImpls which actually has a reference to the
     * ChannelFactory inside
     */
    private final Map<Class<?>, ChannelFactoryDataImpl> channelFactories;
    /**
     * Set of VirtualConnectionFactories for outbound chains.
     */
    protected Map<String, OutboundVirtualConnectionFactoryImpl> outboundVCFactories;
    /**
     * Single VirtualConnectionFactory for the inbound connector channelRunningMap
     * to share
     */
    private final VirtualConnectionFactory inboundVCFactory;
    /**
     * Set of chain groups and their respective chains.
     */
    private final Map<String, ChainGroupData> chainGroups;
    /**
     * List of listeners listening for events of all chains (like WLM).
     */
    private final List<ChainEventListener> globalChainEventListeners;
    /**
     * Timer for asserting runtime stop to chains after timeout expires
     */
    private final Timer stopTimer;

    /**
     * Default discrimination weight used when not specified in adding a channel.
     */
    public static final int DEFAULT_DISC_WEIGHT = 10;

    /**
     * Map to contain all registered services.
     */
    private Map<Class<?>, Object> services = null;

    /** Property name for the chain restart interval timer */
    public static final String PROPERTY_CHAIN_START_RETRY_INTERVAL = "chainStartRetryInterval";
    /** Property name for the chain start retry attempt counter */
    public static final String PROPERTY_CHAIN_START_RETRY_ATTEMPTS = "chainStartRetryAttempts";
    /** Property name for the default chain quiesce timeout length */
    public static final String PROPERTY_CHAIN_QUIESCETIMEOUT = "chainQuiesceTimeout";
    /** Property name for the missing config warning message delay */
    public static final String PROPERTY_MISSING_CONFIG_WARNING = "warningWaitTime";
    /** Alias used in metatype */
    public static final String PROPERTY_CONFIG_ALIAS = "channelfw";

    /**
     * Custom property configured in the framework for the length of time in
     * milliseconds between chain start retries when a RetryableChannelException
     * results from starting a chain, in milliseconds
     */
    protected long chainStartRetryInterval = 5000L;

    /**
     * Custom property configured in the framework for the number of chain start
     * retries
     * that can happen when a RetryableChannelException results from starting a
     * chain.
     */
    protected int chainStartRetryAttempts = 60;
    /** Custom property for timed delay before warning about missing config in milliseconds */
    private long missingConfigWarning = 10000L;
    /** Property for the chain quiescetimeout to default to for various paths in milliseconds */
    private long chainQuiesceTimeout = 0L;

    /**
     * Table to keep track of which Regions a channel is being asked to be run on
     */
    private final Map<String, Integer> ChannelZRegions = new HashMap<String, Integer>();

    /** Channel factory type to class mappings */
    private Map<String, Class<? extends ChannelFactory>> factories = null;
    /** List of channel factory providers found */
    private Map<String, ChannelFactoryProvider> providers = null;
    /** Providers that have been notified on the first lookup of their types */
    private List<ChannelFactoryProvider> activatedProviders = null;
    /** Unique counter for dynamically generated chain names */
    private final AtomicLong chainNameCounter = new AtomicLong(0);
    /** Unique counter for dynamically generated channel names */
    private final AtomicLong channelNameCounter = new AtomicLong(0);

    private AsyncIOHelper asyncIOHelper = null;

    /** Singleton instance of the framework */
    private volatile static ChannelFrameworkImpl singleton = null;

    /**
     * Constructor for the channel framework.
     */
    public ChannelFrameworkImpl() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor");
        }
        this.channelDataMap = new HashMap<String, ChannelData>();
        this.channelRunningMap = new HashMap<String, ChannelContainer>();
        this.chainDataMap = new ConcurrentHashMap<String, ChainDataImpl>();
        this.chainRunningMap = new HashMap<String, Chain>();
        this.channelFactories = new HashMap<Class<?>, ChannelFactoryDataImpl>();
        this.outboundVCFactories = new HashMap<String, OutboundVirtualConnectionFactoryImpl>();
        this.inboundVCFactory = new InboundVirtualConnectionFactoryImpl();
        this.chainGroups = new HashMap<String, ChainGroupData>();
        this.services = new HashMap<Class<?>, Object>();
        this.globalChainEventListeners = new ArrayList<ChainEventListener>();
        this.stopTimer = new Timer(true);
        this.factories = new HashMap<String, Class<? extends ChannelFactory>>();
        this.providers = new HashMap<String, ChannelFactoryProvider>();
        this.activatedProviders = new LinkedList<ChannelFactoryProvider>();

        singleton = this;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "constructor");
        }
    }

    /**
     * Access the singleton instance.
     *
     * @return ChannelFrameworkImpl
     */
    public static ChannelFrameworkImpl getRef() {
        if (null == singleton) {
            synchronized (ChannelFrameworkImpl.class) {
                if (null == singleton) {
                    new ChannelFrameworkImpl();
                }
            }
        }
        return singleton;
    }

    /**
     * Setter method for the interval of time between chain restart attempts.
     *
     * @param value
     * @throws NumberFormatException
     *                                   if the value is not a number or is less than zero
     */
    public void setChainStartRetryInterval(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting chain start retry interval [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_START_RETRY_INTERVAL, value, chainStartRetryInterval);
            if (0L <= num) {
                this.chainStartRetryInterval = num;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Value is too low");
                }
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Value is not a number");
            }
        }
    }

    /**
     * Setter method for the number of chain restart attempts.
     *
     * @param value
     * @throws NumberFormatException
     *                                   if the value is not a number or is less than zero
     */
    public void setChainStartRetryAttempts(Object value) throws NumberFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting chain start retry attempts [" + value + "]");
        }
        try {
            int num = MetatypeUtils.parseInteger(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_START_RETRY_ATTEMPTS, value, chainStartRetryAttempts);
            if (-1 <= num) {
                this.chainStartRetryAttempts = num;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Value too low");
                }
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Vaue is not a number");
            }
        }
    }

    /**
     * Set the default chain quiesce timeout property from config.
     *
     * @param value
     */
    public void setDefaultChainQuiesceTimeout(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting default chain quiesce timeout [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_CHAIN_QUIESCETIMEOUT, value, chainQuiesceTimeout);
            if (0 < num) {
                this.chainQuiesceTimeout = num;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Timeout is too low");
                }
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Timeout is not a number");
            }
        }
    }

    /**
     * Set the custom property for the delay before warning about missing
     * configuration values.
     *
     * @param value
     */
    public void setMissingConfigWarning(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting missing config warning delay to [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_MISSING_CONFIG_WARNING, value, missingConfigWarning);
            if (0L <= num) {
                this.missingConfigWarning = num;
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Value is not a number");
            }
        }
    }

    /**
     * Process configuration information.
     *
     * @param config
     */
    public void updateConfig(Map<String, Object> config) {

        Object value = config.get(PROPERTY_CHAIN_START_RETRY_ATTEMPTS);
        if (null != value) {
            setChainStartRetryAttempts(value);
        }
        value = config.get(PROPERTY_CHAIN_START_RETRY_INTERVAL);
        if (null != value) {
            setChainStartRetryInterval(value);
        }
        value = config.get(PROPERTY_CHAIN_QUIESCETIMEOUT);
        if (null != value) {
            setDefaultChainQuiesceTimeout(value);
        }
        value = config.get(PROPERTY_MISSING_CONFIG_WARNING);
        if (null != value) {
            setMissingConfigWarning(value);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#destroy()
     */
    @Override
    public synchronized void destroy() throws ChannelException, ChainException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }

        // Kill off and null out the timer.
        this.stopTimer.cancel();

        // Gracefully remove the components of the framework.
        clear();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /**
     * This method will remove and destroy all parts of the framework. It
     * is called by the channel service when it is destroyed.
     *
     * @throws ChannelException
     * @throws ChainException
     * @throws ChainGroupException
     */
    public synchronized void clear() throws ChannelException, ChainException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "clear");
        }

        StringBuilder sbErrors = new StringBuilder();

        // Stop destroy and remove all running chains.
        Chain[] chains = chainRunningMap.values().toArray(new Chain[chainRunningMap.size()]);
        for (int i = 0; i < chains.length; i++) {
            Chain chain = chains[i];
            if (chain.getState() == RuntimeState.STARTED || chain.getState() == RuntimeState.QUIESCED) {
                try {
                    stopChainInternal(chain, 0);
                } catch (Exception e) {
                    FFDCFilter.processException(e, getClass().getName() + ".clear", "322", this, new Object[] { chain, this });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught stopChainInternal exception " + e.getMessage());
                    }
                    sbErrors.append(e.toString());
                }
            }
            try {
                destroyChainInternal(chain);
            } catch (ChannelException e) {
                // FFDC handled in destroyChainInternal.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught destroyChainInternal exception " + e.getMessage());
                }
                sbErrors.append(e.toString());
            } catch (ChainException e) {
                // FFDC handled in destroyChainInternal.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught destroyChainInternal exception " + e.getMessage());
                }
                sbErrors.append(e.toString());
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName() + ".clear", "329", this, new Object[] { chain, this });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught destroyChainInternal exception " + e.getMessage());
                }
                sbErrors.append(e.toString());
            }
            try {
                removeChain(chain.getName());
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName() + ".clear", "335", this, new Object[] { chain, this });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught removeChain exception " + e.getMessage());
                }
                sbErrors.append(e.toString());
            }
        }
        this.chainRunningMap.clear();
        this.channelRunningMap.clear();

        // Remove all chain groups
        String[] keys = chainGroups.keySet().toArray(new String[chainGroups.size()]);
        for (int i = 0; i < keys.length; i++) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Remove chainGroup, " + keys[i]);
            }
            try {
                removeChainGroup(keys[i]);
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName() + ".clear", "351", this, new Object[] { keys[i], this });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught removeChainGroup exception " + e.getMessage());
                }
                sbErrors.append(e.toString());
            }
        }
        this.chainGroups.clear();

        // Remove all channels.
        keys = channelDataMap.keySet().toArray(new String[channelDataMap.size()]);
        for (int i = 0; i < keys.length; i++) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Remove channelData, " + keys[i]);
            }
            try {
                String name = keys[i];
                if (name != null)
                    removeChannel(name);
            } catch (Exception e) {
                FFDCFilter.processException(e, getClass().getName() + ".clear", "366", this, new Object[] { keys[i], this });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught removeChannel exception " + e.getMessage());
                }
                sbErrors.append(e.toString());
            }
        }
        this.channelDataMap.clear();
        this.chainDataMap.clear();

        // Destroy all channel factories
        Class<?>[] factoryList = channelFactories.keySet().toArray(new Class<?>[channelFactories.size()]);
        for (Class<?> target : factoryList) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Remove channelFactory, " + target);
            }
            ChannelFactory factory = channelFactories.remove(target).getChannelFactory();
            if (factory != null) {
                try {
                    factory.destroy();
                } catch (Exception e) {
                    FFDCFilter.processException(e, getClass().getName() + ".clear", "384", this, new Object[] { factory, this });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught factory.destroy exception " + e.getMessage());
                    }
                    sbErrors.append(e.toString());
                }
            }
        }
        this.channelFactories.clear();

        // Clear the chain event listeners
        this.globalChainEventListeners.clear();

        // Clear out the services.
        this.services.clear();

        // Check if an error occurred.
        if (sbErrors.length() > 0) {
            // Found an error. Throw an exception containing all the details of the
            // errors caught.
            throw new ChannelException(sbErrors.toString());
        }

        this.activatedProviders.clear();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "clear");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getOutboundVCFactory(String)
     */
    @Override
    public synchronized VirtualConnectionFactory getOutboundVCFactory(String chainName) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getOutboundVCFactory: " + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Unable to get VCFactory for null chain name");
        }

        OutboundVirtualConnectionFactoryImpl vc = outboundVCFactories.get(chainName);
        if (vc == null) {
            // VC Factory does not exist
            ChainData chainData = chainDataMap.get(chainName);
            if (null != chainData) {
                // Create new VCF. Note this initializes the chain and sets the ref
                // count to 1.
                vc = createVirtualConnectionFactory(chainData);
                outboundVCFactories.put(chainName, vc);
            } else if (-1 != chainName.indexOf(ChannelDataImpl.CHILD_STRING)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "getOutboundVCFactory");
                }
                return getNestedOutboundVCFactory(chainName);
            } else {
                InvalidChainNameException exp = new InvalidChainNameException("Chain configuration not found in framework, " + chainName);
                // FFDCFilter.processException(exp,
                // getClass().getName()+".getOutboundVCFactory", "459", this, new
                // Object[] {chainName});
                throw exp;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found existing VCF, " + chainName);
            }
            vc.incrementRefCount();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getOutboundVCFactory");
        }
        return vc;
    }

    /**
     * This method is called from getOutboundVCFactory in cases where a chain name
     * was not given, but rather the
     * internal name of a channel that exists in a runtime chain. A VC will be
     * created which will include con
     * links from channels below the one specified. This allows outbound VCs to
     * multiplex outbound connections.
     *
     * @param channelName
     *                        internal channel name of a current runtime channel.
     * @return a virtual connection composed of channels beneath the one specified
     * @throws ChannelException
     * @throws ChainException
     */
    private synchronized VirtualConnectionFactory getNestedOutboundVCFactory(String channelName) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getNestedOutboundVCFactory");
        }
        OutboundVirtualConnectionFactoryImpl vc = null;

        // The chain name parameter contains an internal runtime channel subname.
        // This triggers of logic to
        // build a VCF based on all channels below the specifed on in its chain.
        // Search for the runtime chain including the named channel.
        ChainData[] chainDataArray = getInternalRunningChains(channelName);
        // Ensure the specified name is correct and a chain is found.
        if (chainDataArray.length != 0) {
            // Extract the chain data from the array. There should only be one.
            ChainData chainData = chainDataArray[0];
            // Find the specified channel in the chainData array of channels.
            // Note that the name of the channel in the chainData is the external one.
            ChannelData[] existingChannelData = chainData.getChannelList();
            int channelIndex = 0;
            boolean foundChannel = false;
            for (channelIndex = 0; channelIndex < existingChannelData.length; channelIndex++) {
                if (channelName.startsWith(existingChannelData[channelIndex].getName() + ChannelDataImpl.CHILD_STRING)) {
                    // Found the named channel in the chain.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found named channel, " + channelName + " in chain " + chainData.getName() + " at index " + channelIndex);
                    }
                    foundChannel = true;
                    break;
                }
            }
            // Verify that the channel was found in the chain.
            if (foundChannel) {
                // Found the channel in the chain. Create the subset of channels.
                int length = existingChannelData.length - channelIndex - 1;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Length of chain to build: " + length);
                }
                ChannelData newChannelData[] = new ChannelData[length];
                String newChannelNames[] = new String[length];
                int newIndex = 0;
                for (int i = channelIndex + 1; i < existingChannelData.length; i++) {
                    newChannelData[newIndex] = existingChannelData[i];
                    newChannelNames[newIndex] = newChannelData[newIndex].getName();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Channel '" + newChannelData[newIndex].getName() + "' added to new nested chain, index=" + newIndex);
                    }
                    newIndex++;
                }
                // Create the new chain data based on the subset of channels.
                ChainDataImpl newChainData = new ChainDataImpl(channelName, FlowType.OUTBOUND, newChannelData, null);
                // Track this chain in our configuration.
                addChain(channelName, FlowType.OUTBOUND, newChannelNames);
                // Create new VCF. Note this initializes the chain and sets the ref
                // count to 1.
                vc = createVirtualConnectionFactory(newChainData);
                // Use the internal channel name as this internal chain name for future
                // lookups (this method).
                outboundVCFactories.put(channelName, vc);
            } else {
                // Did not find the channel.
                InvalidChainNameException exp = new InvalidChainNameException("Chain or channel not found in framework, " + channelName);
                FFDCFilter.processException(exp, getClass().getName() + ".getNestedOutboundVCFactory", "543", this, new Object[] { chainData });
                throw exp;
            }
        } else {
            InvalidChainNameException exp = new InvalidChainNameException("Chain or channel not found in framework, " + channelName);
            FFDCFilter.processException(exp, getClass().getName() + ".getNestedOutboundVCFactory", "548", this, new Object[] { channelName });
            throw exp;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getNestedOutboundVCFactory");
        }
        return vc;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getInboundVCFactory()
     */
    @Override
    public VirtualConnectionFactory getInboundVCFactory() {
        return this.inboundVCFactory;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#setChannelFactoryProperties(Class,
     * Map)
     */
    @Override
    public synchronized ChannelFactoryData updateAllChannelFactoryProperties(Class<?> factoryType, Map<Object, Object> properties) throws ChannelFactoryException {
        ChannelFactoryDataImpl cfd = findOrCreateChannelFactoryData(factoryType);
        cfd.setProperties(properties);
        // Trace each of the properties being added.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateAllChannelFactoryProperties for factory " + factoryType + ", properties:\n" + stringForMap(properties));
        }
        return cfd;
    }

    /**
     * Utility method to extract a string representing the contents of a map.
     * This is currently used by various methods as part of debug tracing.
     *
     * @param map
     * @return string representing contents of Map
     */
    public static String stringForMap(Map<Object, Object> map) {
        StringBuilder sbOutput = new StringBuilder();
        if (map == null) {
            sbOutput.append("\tNULL");
        } else {
            for (Entry<Object, Object> entry : map.entrySet()) {
                sbOutput.append('\t').append(entry).append('\n');
            }
        }
        return sbOutput.toString();
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#updateChannelFactoryProperty(java
     * .lang.Class, java.lang.Object, java.lang.Object)
     */
    @Override
    public synchronized ChannelFactoryData updateChannelFactoryProperty(Class<?> factoryType, Object key, Object value) throws ChannelFactoryException {
        ChannelFactoryDataImpl cfd = findOrCreateChannelFactoryData(factoryType);
        cfd.setProperty(key, value);
        // Trace each of the properties being added.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateChannelFactoryProperty for factory " + factoryType + ", key=" + key + ", value=" + value);
        }

        return cfd;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getChannelFactory(java.lang.Class)
     */
    @Override
    public synchronized ChannelFactoryData getChannelFactory(Class<?> type) throws ChannelFactoryException {
        return findOrCreateChannelFactoryData(type);
    }

    /**
     * find or create a ChannelFactoryDataImpl object based on a this
     * ChannelFactory type
     *
     * @param type
     * @return ChannelFactoryDataImpl
     * @throws ChannelFactoryException
     */
    // Note: this is different than getChannelFactory because it returns a
    // ChannelFactoryDataImpl
    // whereas the getChannelFactory does not return the Impl. Also, it is used by
    // *ChainImpl and
    // is needed outside of this package.
    public synchronized ChannelFactoryDataImpl findOrCreateChannelFactoryData(Class<?> type) throws ChannelFactoryException {
        ChannelFactoryDataImpl cfd = channelFactories.get(type);
        if (cfd == null) {
            ChannelFactory cf = getChannelFactoryInternal(type, false);
            Class<?>[] deviceInf = null;
            Class<?> applicationInf = null;
            try {
                deviceInf = cf.getDeviceInterface();
            } catch (Exception e) {
                // No FFDC Needed
            }
            try {
                applicationInf = cf.getApplicationInterface();
            } catch (Exception e) {
                // No FFDC Needed
            }
            cfd = new ChannelFactoryDataImpl(type, deviceInf, applicationInf);
            this.channelFactories.put(type, cfd);
        }
        return cfd;
    }

    /**
     * This method will do the real work of accessing a channel factory. Note,
     * multiple
     * methods in this class call into this method, some of which are
     * synchronized. According
     * to the input parameters, the factory that is returned may or may not be
     * persisted
     * in the framework. In some cases, like simple channel adds, there is no need
     * for the
     * factory in the framework, an instantiation is only needed to verify that
     * the factory
     * is valid. Other times, like during chain initialization, persistence is
     * necessary.
     *
     * @param type
     *                         The class of the factory being queried
     * @param isPersistent
     *                         flag to indicate whether factory should be saved in framework
     * @return ChannelFactory
     * @throws ChannelFactoryException
     */
    public synchronized ChannelFactory getChannelFactoryInternal(Class<?> type, boolean isPersistent) throws ChannelFactoryException {
        if (type == null) {
            throw new InvalidChannelFactoryException("ChannelFactory type is null");
        }

        ChannelFactory factory = null;
        ChannelFactoryDataImpl cfd = null;
        try {
            // check to see if an instance of the channel factory already exists; if
            // not the contract of this method states that we will create it
            //
            cfd = channelFactories.get(type);
            if (cfd != null) {
                factory = cfd.getChannelFactory();
            }
            if (null == factory) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Create channel factory; " + type);
                }
                // create a new channel factory reflectively & register
                // it by its type
                factory = (ChannelFactory) type.newInstance();
                if (isPersistent) {
                    initChannelFactory(type, factory, null);
                }
            }
        } catch (Exception exp) {
            FFDCFilter.processException(exp, getClass().getName() + ".getChannelFactoryInternal", "675", this, new Object[] { cfd });
            throw new InvalidChannelFactoryException("Can't create instance of channel factory " + type.getName() + " " + exp.getMessage());
        }

        return factory;
    }

    /**
     * This method will store factory properties, initialize the factory & map
     * the factory to it's type. This method exists primarily as a performance
     * enhancement for the WebSphere-specific implementation; without this
     * method we would have to create two factory instances: one to translate
     * WCCM objects to property maps, the other to be the actual factory in use.
     *
     * @param type
     * @param factory
     * @param properties
     */
    protected synchronized void initChannelFactory(Class<?> type, ChannelFactory factory, Map<Object, Object> properties) throws ChannelFactoryException {

        // if no properties were provided, then we must retrieve them from
        // channelFactoriesProperties using the factory type as the key; if
        // the properties were provided, make sure that they are in
        // channelFactoriesProperties for potential future use
        //

        ChannelFactoryDataImpl cfd = findOrCreateChannelFactoryData(type);
        cfd.setChannelFactory(factory);

        if (properties != null) {
            cfd.setProperties(properties);
        }

        try {
            factory.init(cfd);
        } catch (ChannelFactoryException ce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Factory " + factory + " threw ChannelFactoryException " + ce.getMessage());
            }
            throw ce;
        } catch (Throwable e) {
            FFDCFilter.processException(e, getClass().getName() + ".initChannelFactory", "770", this, new Object[] { factory });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Factory " + factory + " threw non-ChannelFactoryException " + e.getMessage());
            }
            throw new ChannelFactoryException(e);
        }

    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#addChannel(String, Class,
     * Map, int)
     */
    @Override
    public synchronized ChannelData addChannel(String channelName, Class<?> factoryType, Map<Object, Object> inputPropertyBag, int weight) throws ChannelException {
        return addChannelInternal(channelName, factoryType, inputPropertyBag, weight);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#addChannel(String, Class,
     * Map)
     */
    @Override
    public synchronized ChannelData addChannel(String channelName, Class<?> factoryType, Map<Object, Object> inputPropertyBag) throws ChannelException {
        return addChannelInternal(channelName, factoryType, inputPropertyBag, DEFAULT_DISC_WEIGHT);
    }

    /**
     * This method does the work of adding a channel data object to the
     * framework. It is called internally by both the addInbound and
     * addOutbound channel methods.
     *
     * @param channelName
     * @param factoryType
     * @param inputPropertyBag
     * @param weight
     * @return channel data object resulting from the add.
     * @throws ChannelException
     */
    private ChannelData addChannelInternal(String channelName, Class<?> factoryType, Map<Object, Object> inputPropertyBag, int weight) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addChannelInternal: channelName=" + channelName + ", factoryType=" + factoryType + ", weight=" + weight);
        }

        // Ensure the weight is non negative.
        if (weight < 0) {
            throw new InvalidWeightException("Invalid weight for channel, " + weight);
        }

        // Ensure the input channel name is not null
        if (null == channelName) {
            throw new InvalidChannelNameException("Input channel name is null");
        }

        // Check if the channel name already exists in the configuration.
        ChannelData channelData = channelDataMap.get(channelName);
        if (null != channelData) {
            throw new InvalidChannelNameException("Channel already exists: " + channelName);
        }

        // Check the factory type and its validity. No need to actually retrieve the
        // factory. We don't want it to persist and be saved, potentially wasting
        // memory.
        // Rather, just do the check to ensure the factory type is valid.
        getChannelFactoryInternal(factoryType, false);

        // Prepare the property bag.
        Map<Object, Object> propertyBag = inputPropertyBag;
        if (null == propertyBag) {
            propertyBag = new HashMap<Object, Object>();
        }
        channelData = createChannelData(channelName, factoryType, propertyBag, weight);
        this.channelDataMap.put(channelName, channelData);
        return channelData;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#removeChannel(String)
     */
    @Override
    public synchronized ChannelData removeChannel(String channelName) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeChannel: " + channelName);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Input channel name is null");
        }

        // Fetch the channel data from the framework.
        ChannelDataImpl channelData = (ChannelDataImpl) channelDataMap.get(channelName);
        if (null == channelData) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                // Due to timing windows (i.e. multiple channel providers going away), there is
                // a race to stop and/or clean up chains. This is not a notable condition: the chain can only be removed
                // once.
                Tr.exit(tc, "removeChannel: " + channelName, "Channel not found");
            }
            return null;
        }

        // Channel configs can only be removed if they are not in use by the
        // runtime.
        if (0 != channelData.getNumChildren()) {
            throw new ChannelException("Can't remove channel config " + channelName + " in runtime.  Destroy must happen first. ");
        }

        // Remove the chain config from the framework's map.
        this.channelDataMap.remove(channelName);

        // iterating chain configs will clear out live chains/channelRunningMap
        // Remove any chain configurations that refer to this channel.
        // Get a temp array so we can release the lock needed by removeChain.
        // This isn't a clone, but we should be okay holding the references.
        Object[] chainDataArray = this.chainDataMap.values().toArray();
        ChainDataImpl chainData = null;
        for (int i = 0; i < chainDataArray.length; i++) {
            chainData = (ChainDataImpl) chainDataArray[i];
            if (chainData.containsChannel(channelName)) {
                // Found reference to channel config. Remove chain config.
                // This ripples into the chain groups and cleans them up too.
                removeChain(chainData.getName());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "removeChannel");
        }
        return channelData;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#updateAllChannelProperties(java
     * .lang.String, java.util.Map)
     */
    @Override
    public synchronized ChannelData updateAllChannelProperties(String channelName, Map<Object, Object> newProperties) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateAllChannelProperties: " + channelName);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Input channel name is null");
        }
        if (null == newProperties) {
            throw new ChannelException("Null properties found.");
        }

        // Find the existing channel config
        ChannelDataImpl channelData = (ChannelDataImpl) channelDataMap.get(channelName);
        if (null == channelData) {
            throw new InvalidChannelNameException("Unable to find input channel, " + channelName);
        }

        // Update the property bag. Note this may throw an exception.
        channelData.setPropertyBag(newProperties);
        // Trace each of the properties being added.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "New properties for channel " + channelName + ", properties:\n" + stringForMap(newProperties));
        }

        // Update each running channel that is using this parent channel data
        // object.
        updateRunningChannels(channelData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateAllChannelProperties");
        }
        return channelData;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#updateChannelProperty(java.lang
     * .String, java.lang.String, java.lang.Object)
     */
    @Override
    public synchronized ChannelData updateChannelProperty(String channelName, Object propertyKey, Object propertyValue) throws ChannelException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateChannelProperty");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName + "propertyKey=" + propertyKey + ", propertyValue=" + propertyValue);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Input channel name is null");
        }
        if (null == propertyKey || null == propertyValue) {
            throw new ChannelException("Null property key or value found.");
        }

        // Find the existing channel config
        ChannelDataImpl channelData = (ChannelDataImpl) channelDataMap.get(channelName);
        if (null == channelData) {
            throw new InvalidChannelNameException("Unable to find input channel, " + channelName);
        }

        // Update the property bag. Note this may throw an exception.
        channelData.setProperty(propertyKey, propertyValue);
        // Update each running channel that is using this parent channel data
        // object.
        updateRunningChannels(channelData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateChannelProperty");
        }
        return channelData;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#updateChannelWeight(java.lang.
     * String, int)
     */
    @Override
    public synchronized ChannelData updateChannelWeight(String channelName, int newWeight) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateChannelWeight");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName + ", newWeight=" + newWeight);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Input channel name is null");
        }
        if (newWeight < 0) {
            throw new InvalidWeightException("Invalid input weight, " + newWeight);
        }

        // Find the existing channel config
        ChannelDataImpl channelData = (ChannelDataImpl) channelDataMap.get(channelName);
        if (null == channelData) {
            throw new InvalidChannelNameException("Unable to find input channel, " + channelName);
        }

        // Set the new weight in the framework.
        channelData.setDiscriminatorWeight(newWeight);
        // Update each running channel that is using this parent channel data
        // object.
        updateRunningChannels(channelData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateChannelWeight");
        }
        return channelData;
    }

    /**
     * Update all running channels using the input channel data.
     *
     * @param channelData
     *                        (parent)
     */
    private void updateRunningChannels(ChannelDataImpl channelData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateRunningChannels");
        }
        Channel channel = null;
        ChannelContainer channelContainer = null;
        Iterator<ChildChannelDataImpl> children = channelData.children();
        while (children.hasNext()) {
            // Find the running channel with the child name.
            channelContainer = channelRunningMap.get(children.next().getName());
            channel = channelContainer.getChannel();
            // Inform the channel of the update. Note, parent data is ref'd in the
            // child data.
            channel.update(channelContainer.getChannelData());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateRunningChannels");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getChannel(String)
     */
    @Override
    public synchronized ChannelData getChannel(String channelName) {
        ChannelData channel = null;
        if (null != channelName) {
            channel = this.channelDataMap.get(channelName);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getChannel: " + channelName + " found=" + (null != channel));
        }
        return channel;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getAllChannels()
     */
    @Override
    public synchronized ChannelData[] getAllChannels() {
        return this.channelDataMap.values().toArray(new ChannelData[this.channelDataMap.size()]);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getRunningChannels()
     */
    @Override
    public synchronized ChannelData[] getRunningChannels() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRunningChannels");
        }

        // Note, runtime has child data objects so duplicates may be found.
        List<ChannelData> list = new ArrayList<ChannelData>();
        for (ChannelContainer channel : this.channelRunningMap.values()) {
            list.add(channel.getChannelData().getParent());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRunningChannels");
        }
        return list.toArray(new ChannelData[list.size()]);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getListeningPort(java.lang.String)
     */
    @Override
    public int getListeningPort(String chainName) throws ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getListeningPort: " + chainName);
        }

        int listeningPort = -1;
        // Search the runtime chain list for the specified chain.
        Chain chain = this.chainRunningMap.get(chainName);
        if (null == chain) {
            throw new ChainException("Chain " + chainName + " not found in runtime.");
        }
        // Found the chain. Pull out the data on the chain.
        ChainData chainData = chain.getChainData();
        if (chainData.getType().equals(FlowType.INBOUND)) {
            ChannelData channelData = chainData.getChannelList()[0];
            Map<Object, Object> channelProperties = channelData.getPropertyBag();
            if (channelProperties != null) {
                String portString = (String) channelProperties.get(ChannelFrameworkConstants.PORT);
                if (portString != null && !portString.trim().equals("0")) {
                    listeningPort = Integer.parseInt(portString);
                } else {
                    // Port is not directly set. Check listenPort (in cases where port is
                    // config'd as *.
                    portString = (String) channelProperties.get(ChannelFrameworkConstants.LISTENING_PORT);
                    if (portString != null) {
                        listeningPort = Integer.parseInt(portString);
                    } else {
                        throw new ChainException("Chain " + chainName + " has no port in the device channel properties.");
                    }
                }
            } else {
                throw new ChainException("Chain " + chainName + " has no properties.");
            }
        } else {
            throw new ChainException("Chain " + chainName + " is not inbound.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getListeningPort: " + listeningPort);
        }
        return listeningPort;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getListeningHost(java.lang.String)
     */
    @Override
    public String getListeningHost(String chainName) throws ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getListeningHost: " + chainName);
        }

        String listeningHost = null;
        // Search the runtime chain list for the specified chain.
        Chain chain = this.chainRunningMap.get(chainName);
        if (chain != null) {
            ChainData chainData = chain.getChainData();
            if (chainData.getType().equals(FlowType.INBOUND)) {
                // Extract the first channel in the chain. The device side channel.
                ChannelData channelData = chainData.getChannelList()[0];
                Map<Object, Object> channelProperties = channelData.getPropertyBag();
                if (channelProperties != null) {
                    // Extract the host string from the chain properties.
                    listeningHost = (String) channelProperties.get(ChannelFrameworkConstants.HOST_NAME);
                    if (listeningHost == null) {
                        throw new ChainException("Chain " + chainName + " has no host in the device channel properties.");
                    }
                } else {
                    throw new ChainException("Chain " + chainName + " has no properties.");
                }
            } else {
                throw new ChainException("Chain " + chainName + " is not inbound.");
            }
        } else {
            throw new ChainException("Chain " + chainName + " not found in runtime.");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getListeningHost: " + listeningHost);
        }
        return listeningHost;
    }

    /**
     * This method won't actually call the init method of the channel
     * implemenation unless the channel is uninitialized. If the channel
     * doesn't exist in the runtime yet, it will be put in here. The channel
     * in the runtime will then be updated with a reference to the input chain.
     * This method is invoked from the chain implementation.
     *
     * @param channel
     *                    being initialized
     * @param chain
     *                    the channel is in
     * @return if the chain was initialized. Note, it may already be initialized.
     * @throws ChannelException
     */
    private boolean initChannelInChain(Channel channel, Chain chain) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initChannelInChain");
        }
        // No need to check parameters since this is called from internal only.
        String channelName = channel.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName + " chainName=" + chain.getName());
        }
        boolean channelInitialized = false;
        ChannelContainer channelContainer = channelRunningMap.get(channelName);
        if (null == channelContainer) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel not found in runtime so build it");
            }
            // Get the child channel data for this channel
            ChannelData channelsData[] = chain.getChannelsData();
            // Search through the chain's list of channel data. Note, it will always
            // be here.
            int index = 0;
            for (; index < channelsData.length; index++) {
                if (channelsData[index].getName().equals(channel.getName())) {
                    // Found it.
                    break;
                }
            }
            if (index == channelsData.length) {
                ChannelException e = new ChannelException("Channel providing incorrect name; " + channel.getName());
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "Channel provided the wrong name, probably the external name instead of the internal one; " + channel.getName());
                }
                throw e;
            }
            // Initialize the runtime channel
            try {
                channel.init();
            } catch (ChannelException ce) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Channel " + channel + " threw ChannelException " + ce.getMessage());
                throw ce;
            } catch (Throwable e) {
                FFDCFilter.processException(e, getClass().getName() + ".initChannelInChain", "1168", this, new Object[] { channel });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Channel " + channel + " threw non-ChannelException " + e.getMessage());
                throw new ChannelException(e);
            }
            channelInitialized = true;
            // Create channel container to store information in framework.
            channelContainer = new ChannelContainer(channel, (ChildChannelDataImpl) channelsData[index]);
            // Add channel container to the framework.
            this.channelRunningMap.put(channelName, channelContainer);
        }
        channelContainer.addChainReference(chain);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initChannelInChain");
        }
        return channelInitialized;
    }

    /**
     * Start the input channel. While it isn't needed yet, the chain from which
     * the start is coming from is in the parameter list. This method will only
     * be called from the chain implementation. Regardless of what chains are
     * referenced by the channel, if the state is initialized or quiesced, then
     * the channel will be started.
     *
     * @param targetChannel
     *                          being started
     * @param chain
     *                          the channel is in.
     * @return if the channel was started or not. Note, it may have already been
     *         started.
     * @throws ChannelException
     */
    private boolean startChannelInChain(Channel targetChannel, Chain chain) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startChannelInChain");
        }
        // No need to check parameters since this is called from internal only.
        String channelName = targetChannel.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName + " chainName=" + chain.getName());
        }
        boolean channelStarted = false;
        ChannelContainer channelContainer = channelRunningMap.get(channelName);
        // No need to check returned channelContainer since this is called from
        // internal only.
        RuntimeState channelState = channelContainer.getState();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Found channel, state: " + channelState.ordinal);
        }
        Channel channel = channelContainer.getChannel();
        if ((RuntimeState.INITIALIZED == channelState) || (RuntimeState.QUIESCED == channelState)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Starting channel");
            }
            try {
                channel.start();
            } catch (ChannelException ce) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Channel " + channel + " threw ChannelException " + ce.getMessage());
                }
                throw ce;
            } catch (Throwable e) {
                FFDCFilter.processException(e, getClass().getName() + ".startChannelInChain", "1228", this, new Object[] { channel });
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Channel " + channel + " threw non-ChannelException " + e.getMessage());
                }
                throw new ChannelException(e);
            }
            channelStarted = true;
            channelContainer.setState(RuntimeState.STARTED);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Skip channel start, invalid former state: " + channelState.ordinal);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startChannelInChain");
        }
        return channelStarted;
    }

    /**
     * This method is done in preparation for stopping a channel. It pull's its
     * discriminator from the device side channel's discrimination process in the
     * chain.
     *
     * @param targetChannel
     *                          being stopped
     * @param chain
     *                          the channel is in
     * @return if the channel was disabled
     * @throws ChannelException
     * @throws ChainException
     */
    private boolean disableChannelInChain(Channel targetChannel, Chain chain) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "disableChannelInChain");
        }
        String channelName = targetChannel.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName + " chainName=" + chain.getName());
        }
        ChannelContainer channelContainer = channelRunningMap.get(channelName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Found channel, state: " + channelContainer.getState().ordinal);
        }
        Channel channel = channelContainer.getChannel();
        RuntimeState chainState = null;
        boolean stillInUse = false;
        for (Chain channelChain : channelContainer.getChainMap().values()) {
            chainState = channelChain.getState();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found chain reference: " + channelChain.getName() + ", state: " + chainState.ordinal);
            }
            if (channelChain.getName().equals(chain.getName())) {
                // Don't analyze this channel. It is either in STARTED or QUIESCED
                // state.
                continue;
            } else if ((RuntimeState.STARTED == chainState) || (RuntimeState.QUIESCED == chainState)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found chain that is not ready to stop, " + channelChain.getName());
                }
                stillInUse = true;
                break;
            }
        }
        // Check to ensure that no other chains will be broken.
        if (!stillInUse) {
            // Disable the channel from handling any more connections in the chain.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Disabling channel, " + channelName);
            }
            ((InboundChain) chain).disableChannel(channel);
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Skip channel stop, in use by other chain(s)");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "disableChannelInChain");
        }
        return !stillInUse;
    }

    /**
     * Stop a channel. It is assumed that the channel has already been disabled
     * (inbound).
     * Once this method is called for each channel in the chain, the chain's state
     * will
     * change to INITIALIZED.
     *
     * @param channel
     *                    to stop
     */
    private void stopChannel(Channel channel) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stopChannel");
        }
        String channelName = channel.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName);
        }
        ChannelContainer channelContainer = channelRunningMap.get(channelName);
        try {
            channel.stop(0);
        } catch (ChannelException ce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Channel " + channel + " threw ChannelException " + ce.getMessage());
            throw ce;
        } catch (Throwable e) {
            FFDCFilter.processException(e, getClass().getName() + ".stopChannel", "1338", this, new Object[] { channel });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Channel " + channel + " threw non-ChannelException " + e.getMessage());
            throw new ChannelException(e);
        }
        // Update the state.
        channelContainer.setState(RuntimeState.INITIALIZED);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stopChannel");
        }
    }

    /**
     * Destroy the channel of the specified chain if it is not in use by another
     * chain. This
     * method is called by the chain implementation during destroy processing.
     * Once complete, the
     * state of the chain will go to DESTROYED.
     *
     * @param targetChannel
     *                          being destroyed
     * @param chain
     *                          of the channel
     * @param channelData
     *                          associated with channel
     * @throws ChannelException
     * @throws ChainException
     */
    private synchronized void destroyChannelInChain(Channel targetChannel, Chain chain, ChannelData channelData) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroyChannelInChain");
        }
        // No need to check parameters since this is called from internal only.
        String channelName = targetChannel.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "channelName=" + channelName + " chainName=" + chain.getName());
        }
        ChannelContainer channelContainer = channelRunningMap.get(channelName);
        // No need to check returned channelContainer since this is called from
        // internal only.
        Channel channel = channelContainer.getChannel();
        Map<String, Chain> chainMap = channelContainer.getChainMap();
        RuntimeState channelState = channelContainer.getState();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Remove chain reference for channel, " + channelName);
        }
        channelContainer.removeChainReference(chain.getName());
        int numChains = chainMap.size();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Remaining chain refs, " + numChains);
        }

        // The only way to destroy a channel is if no other chains reference it at
        // all.
        if (numChains == 0) {
            if (RuntimeState.INITIALIZED != channelState) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skip channel destroy, not in correct state, state=" + channelState.ordinal);
                }
            } else {
                if (chain.getChainData().getType().equals(FlowType.INBOUND)) {
                    // Disable the channel from handling any more connections in the
                    // chain.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Disabling channel, " + channel.getName());
                    }
                    ((InboundChain) chain).disableChannel(channel);
                }
                // Destroy the channel itself.
                try {
                    channel.destroy();
                } catch (ChannelException ce) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Channel " + channel + " threw ChannelException " + ce.getMessage());
                    }
                    throw ce;
                } catch (Throwable e) {
                    FFDCFilter.processException(e, getClass().getName() + ".destroyChannelInChain", "1408", this, new Object[] { channel });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Channel " + channel + " threw non-ChannelException " + e.getMessage());
                    }
                    throw new ChannelException(e);
                }
                // Remove the channel from the framework runtime. Opposite of init.
                this.channelRunningMap.remove(channelName);
                // Remove reference to child in parent channel data object.
                ChildChannelDataImpl child = (ChildChannelDataImpl) channelData;
                child.getParent().removeChild(child);
                // Get running channels with this type
                ChainData[] rChains = getRunningChains(channelData.getFactoryType());
                if (rChains == null || rChains.length == 0 || rChains[0] == null || (rChains.length == 1 && rChains[0].getName().equals(chain.getChainData().getName()))) {
                    // remove the ChannelFactory if it is no longer in use.
                    ChannelFactoryDataImpl cfd = channelFactories.remove(channelData.getFactoryType());
                    ChannelFactory factory = cfd.getChannelFactory();
                    cfd.setChannelFactory(null);
                    if (factory != null) {
                        try {
                            factory.destroy();
                        } catch (Throwable e) {
                            FFDCFilter.processException(e, getClass().getName() + ".destroyChannelInChain", "1450", this, new Object[] { factory });
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Factory " + factory + " threw non-ChannelFactoryException " + e.getMessage());
                            }
                            // no throwing this up being that we don't care about this case
                            // anymore
                        }
                    }

                }
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Skip channel destroy, in use by other chain(s)");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroyChannelInChain");
        }
    }

    /**
     * Determines if we are running on a Z/OS system.
     * If we are running on WAS, then this will be overridden by
     * com.ibm.ws.channel.framework.WSChannelFrameworkImpl.currentlyOnZ()
     * If we are not on WAS, assume that we are not on Z, and return false
     *
     * @return if true we are running on Z/OS, else false
     */
    protected boolean currentlyOnZ() {
        return false;
    }

    /**
     * Determines what Z region we are running on.
     * If we are running on WAS, then this will be overridden by
     * com.ibm.ws.channel.framework.WSChannelFrameworkImpl.currentlyZRegion()
     * If we are not on WAS, then this function should never be called, but put it
     * here for testing purposes and for compiling.
     *
     * @return the constand defining the Z Region thate we are running on
     */
    protected int currentZRegion() {
        // this routine should not be called, unless doing unit test, so return
        // the most logical value.
        return RegionType.NOT_ON_Z;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#addChain(String, FlowType,
     * String[])
     */
    @Override
    public synchronized ChainData addChain(String chainName, FlowType chainType, String[] channelNameList) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addChain: " + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Input chain name is null");
        }
        if (null == channelNameList || 0 == channelNameList.length) {
            throw new InvalidChannelNameException("Invalid channel list");
        }

        ChainDataImpl chainData = chainDataMap.get(chainName);
        // Ensure the chain name doesn't already exist in the framework.
        if (null != chainData) {
            InvalidChainNameException e = new InvalidChainNameException("Chain config already exists, " + chainName);
            FFDCFilter.processException(e, getClass().getName() + ".addChain", "1411", this, new Object[] { chainData });
            throw e;
        }

        int zRegion = RegionType.NOT_ON_Z;
        boolean okToAdd = true;
        ChannelFactory factory = null;

        // Determine if we are running on Z, and if so, what region we are in
        if (currentlyOnZ()) {
            zRegion = currentZRegion();
        }

        // Ensure each of the channel configurations already exist in the framework.
        ChannelData[] channelDataArray = new ChannelData[channelNameList.length];
        ChannelData channelData = null;
        int i = 0;
        int lastIndex = channelNameList.length - 1;
        for (i = 0; i <= lastIndex; i++) {
            factory = null;
            channelData = channelDataMap.get(channelNameList[i]);
            if (null == channelData) {
                InvalidChannelNameException e = new InvalidChannelNameException("Can't add chain config due to unknown channel, " + channelNameList[i]);
                FFDCFilter.processException(e, getClass().getName() + ".addChain", "1443", this, new Object[] { channelNameList[i] });
                throw e;
            }
            if (zRegion != RegionType.NOT_ON_Z && chainType.equals(FlowType.INBOUND)) {
                // On Z and inbound, need to do some special Z things
                if (i == lastIndex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "On Z, looking at Application Channel, " + channelData.getName());
                    }
                    // Looking at the Inbound Application Channel
                    factory = getChannelFactoryInternal(channelData.getFactoryType(), false);

                    if (zRegion == RegionType.SR_REGION) {
                        // in SR, check if we should start this chain
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Operating in Servant Region");
                        }
                        if (factory instanceof BoundRegion) {
                            if (!(((BoundRegion) factory).isServantStartable(channelDataMap))) {
                                // channel does not want to start given the current
                                // configuration
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Channel says it is not startable in Servant Region");
                                }
                                okToAdd = false;
                            }
                        }
                    } else if ((zRegion == BoundRegion.CR_REGION) || (zRegion == BoundRegion.CRA_REGION)) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Operating in CR or CRA Region, Region constant: " + zRegion);
                        }
                        // In a secure region. check if the channel implements BoundRegion,
                        // and is configured to run in this region
                        if (factory instanceof BoundRegion) {
                            if (zRegion != (((BoundRegion) factory).getRegion(channelDataMap))) {
                                // channel does not support running in this region
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "This Channel says it is not startable in this Region: getRegion returned: " + ((BoundRegion) factory).getRegion(channelDataMap));
                                }
                                okToAdd = false;
                            }
                        }
                    }
                } else {
                    // Not Looking at an application channel in the chain
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "On Z, looking at non-application Channel, " + channelData.getName());
                    }

                    if ((zRegion == BoundRegion.CR_REGION) || (zRegion == BoundRegion.CRA_REGION)) {
                        // mark what region this channel is being asked to run in, and
                        // log a warning if this gives a sharable problem
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Operating in CR or CRA Region, Region constant: " + zRegion);
                        }

                        // Get Channel entry from HashMap
                        Integer channelEntry = ChannelZRegions.get(channelData.getName());
                        if (channelEntry == null) {
                            // new entry
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "First time setting region entry for this channel");
                            }
                            ChannelZRegions.put(channelData.getName(), Integer.valueOf(zRegion));
                        } else {
                            // check to see if we are being asked to run in a different region
                            int entryValue = channelEntry.intValue();

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Not first time setting region entry for this channel, current region entry is: " + entryValue);
                            }

                            if ((entryValue & zRegion) == 0) {
                                // being asked to be run in a different region, see if this is
                                // sharable
                                factory = getChannelFactoryInternal(channelData.getFactoryType(), false);
                                if (factory instanceof CrossRegionSharable) {
                                    if (!(((CrossRegionSharable) factory).isSharable(channelDataMap))) {
                                        // non-sharable channel is being put into different regions
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                            Tr.debug(tc, "Channel says it is not sharable");
                                        }
                                        Tr.warning(tc, "channel.shared.warning", new Object[] { channelData.getName() });

                                    }
                                }
                                // update regions that this channel is being used in
                                ChannelZRegions.put(channelData.getName(), Integer.valueOf(entryValue | zRegion));
                            }
                        }
                    }
                }
            }
            channelDataArray[i] = channelData;
        } // end-for loop on channels

        if (okToAdd) {
            try {
                // Store values for the host and port in the chain if it is inbound.
                Map<Object, Object> chainProperties = null;
                if (FlowType.INBOUND.equals(chainType)) {
                    chainProperties = new HashMap<Object, Object>();
                    Map<Object, Object> tcpProperties = channelDataArray[0].getPropertyBag();
                    chainProperties.put(ChannelFrameworkConstants.HOST_NAME, tcpProperties.get(ChannelFrameworkConstants.HOST_NAME));
                    chainProperties.put(ChannelFrameworkConstants.PORT, tcpProperties.get(ChannelFrameworkConstants.PORT));
                    chainProperties.put(ChannelFrameworkConstants.LISTENING_PORT, tcpProperties.get(ChannelFrameworkConstants.LISTENING_PORT));
                }
                chainData = (ChainDataImpl) createChainData(chainName, chainType, channelDataArray, chainProperties);
                // Add the new chain to the framework configuration.
                this.chainDataMap.put(chainName, chainData);
                // Associate any global chain event listeners.
                for (int j = 0; j < globalChainEventListeners.size(); j++) {
                    chainData.addChainEventListener(globalChainEventListeners.get(j));
                }
            } catch (IncoherentChainException e) {
                FFDCFilter.processException(e, getClass().getName() + ".addChain", "1601", this, new Object[] { chainName, chainType, channelDataArray });
                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addChain");
        }
        return chainData;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#removeChain(com.ibm.websphere.
     * channelfw.ChainData)
     */
    @Override
    public synchronized ChainData removeChain(ChainData chain) throws ChainException {
        String chainName = (null != chain) ? chain.getName() : null;
        String entryMsg = "removeChain: " + chainName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, entryMsg);
        }
        if (null == chain) {
            throw new InvalidChainNameException("Input chain is null");
        }

        // Verify this chain does exist
        if (!chainDataMap.containsKey(chainName)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                // Due to timing windows (i.e. multiple channel providers going away), there is
                // a race to stop and/or clean up chains. This is not a notable condition: the chain can only be removed
                // once.
                Tr.exit(tc, entryMsg, "Chain not found");
            }
            return null;
        }

        removeChainInternal(chain);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, entryMsg);
        }
        return chain;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#removeChain(String)
     */
    @Override
    public synchronized ChainData removeChain(String chainName) throws ChainException {
        String entryMsg = "removeChain: " + chainName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, entryMsg);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Input chain name is null");
        }

        // Look for runtime chains.
        Chain chain = chainRunningMap.get(chainName);
        if (chain != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                // Due to timing windows (i.e. multiple channel providers going away), there is
                // a race to stop and/or clean up chains. This is not a notable condition: the chain can only be removed
                // once.
                Tr.exit(tc, entryMsg, "Chain not found");
            }
            return null;
        }

        // Find the chain config
        ChainData chainData = chainDataMap.get(chainName);
        if (null == chainData) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                // Due to timing windows (i.e. multiple channel providers going away), there is
                // a race to stop and/or clean up chains. This is not a notable condition: the chain can only be removed
                // once.
                Tr.exit(tc, entryMsg, "ChainData not found");
            }
            return null;
        }
        removeChainInternal(chainData);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, entryMsg);
        }
        return chainData;
    }

    /**
     * Remove the chain from the framework and disconnect it from any groups
     * that contain it.
     *
     * @param chaindata
     */
    private synchronized void removeChainInternal(ChainData chaindata) {
        String chainName = chaindata.getName();
        ChainData[] chains = null;
        for (String groupName : chainGroups.keySet()) {
            chains = chainGroups.get(groupName).getChains();
            int j = 0;
            for (; j < chains.length; j++) {
                if (chainName.equals(chains[j].getName())) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Removing chain from chain group, " + groupName);
                    }
                    break; // out of for
                }
            }
            // If the chain was removed from this group, update the group.
            if (j < chains.length) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Updating chain group with new chain config list, " + groupName);
                }
                ChainData[] newChains = new ChainData[chains.length - 1];
                // copy chains listed before this one...
                int k = 0;
                for (; k < j; k++) {
                    newChains[k] = chains[k];
                }
                // copy chains listed after this one...
                for (++j; j < chains.length; k++, j++) {
                    newChains[k] = chains[j];
                }
                chainGroups.put(groupName, createChainGroupData(groupName, newChains));
            }
        }
        // Remove the chain configuration.
        chainDataMap.remove(chainName);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#updateChain(String, String[])
     */
    @Override
    public synchronized ChainData updateChain(String chainName, String[] newChannelList) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateChain: " + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Null chain name");
        }
        if ((null == newChannelList) || (0 == newChannelList.length)) {
            throw new InvalidChannelNameException("Null or empty channel list");
        }
        // Verify chain config exists.
        ChainDataImpl oldChainData = chainDataMap.get(chainName);
        if (null == oldChainData) {
            InvalidChainNameException e = new InvalidChainNameException("Unable to update unknown chain, " + chainName);
            FFDCFilter.processException(e, getClass().getName() + ".updateChain", "1724", this, new Object[] { chainName });
            throw e;
        }

        // Verify the chain config is not currently in use by the runtime.
        Chain chain = chainRunningMap.get(chainName);
        if (chain != null) {
            ChainException e = new ChainException("Unable to update runtime chain " + chainName + ".  Destroy chain first.");
            FFDCFilter.processException(e, getClass().getName() + ".updateChain", "1733", this, new Object[] { chain });
            throw e;
        }

        // Verify all channel configs were found in the framework.
        ChannelData[] newChannelData = new ChannelData[newChannelList.length];
        for (int i = 0; i < newChannelList.length; i++) {
            // Build up list of new channel configs for the new chain.
            newChannelData[i] = channelDataMap.get(newChannelList[i]);
            if (null == newChannelData[i]) {
                InvalidChannelNameException e = new InvalidChannelNameException("Unable to update chain config with unknown channel, " + newChannelList[i]);
                FFDCFilter.processException(e, getClass().getName() + ".updateChain", "1752", this, new Object[] { newChannelList[i] });
                throw e;
            }
        }

        // Ensure that new configuration is different from old one.
        ChannelData[] oldChannelDataArray = oldChainData.getChannelList();
        boolean configurationDifferent = true;
        if (oldChannelDataArray.length == newChannelData.length) {
            // Same number of channelRunningMap. Now look for exact same
            // channelRunningMap.
            String oldChannelName = null;
            String newChannelName = null;
            boolean foundOldChannel = false;
            configurationDifferent = false;
            for (int j = 0; j < oldChannelDataArray.length; j++) {
                oldChannelName = oldChannelDataArray[j].getName();
                foundOldChannel = false;
                for (int k = 0; k < newChannelData.length; k++) {
                    newChannelName = newChannelData[k].getName();
                    if (oldChannelName.equals(newChannelName)) {
                        foundOldChannel = true;
                        break;
                    }
                }
                if (!foundOldChannel) {
                    // Never found the old channel in the new channel list.
                    configurationDifferent = true;
                    break;
                }
            }
        }
        if (!configurationDifferent) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Identical channel list, no update");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "updateChain");
            }
            return oldChainData;
        }

        // Create the new chain configuration object with the input chain name.
        ChainDataImpl newChainData = null;
        try {
            newChainData = (ChainDataImpl) createChainData(chainName, FlowType.INBOUND, newChannelData, oldChainData.getPropertyBag());
        } catch (IncoherentChainException e) {
            FFDCFilter.processException(e, getClass().getName() + ".updateChain", "1792", this, new Object[] { chainName, newChannelData });
            throw e;
        }

        // Ensure existing listeners in old chain config move to new chain config.
        newChainData.setChainEventListeners(oldChainData.removeAllChainEventListeners());
        // Create or swap in new chain config.
        this.chainDataMap.put(chainName, newChainData);

        // Update any chain groups including this chain.
        ChainGroupDataImpl groupData = null;
        Iterator<ChainGroupData> groupIter = chainGroups.values().iterator();
        while (groupIter.hasNext()) {
            groupData = (ChainGroupDataImpl) groupIter.next();
            if (groupData.containsChain(chainName)) {
                groupData.updateChain(newChainData);
            }
        }

        // Alert the chain event listener.
        newChainData.chainUpdated();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateChain");
        }
        return newChainData;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getChain(String)
     */
    @Override
    public synchronized ChainData getChain(String chainName) {
        ChainData chainData = null;
        // Ensure the input chain name is not null
        if (null != chainName) {
            chainData = this.chainDataMap.get(chainName);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getChain: " + chainName + " found=" + (null != chainData));
        }
        return chainData;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getAllChains()
     */
    @Override
    public synchronized ChainData[] getAllChains() {
        return this.chainDataMap.values().toArray(new ChainData[this.chainDataMap.size()]);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getAllChains(java.lang.String)
     */
    @Override
    public synchronized ChainData[] getAllChains(String channelName) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getAllChains: " + channelName);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Null channelName found");
        }

        ChainDataImpl chainData = null;
        List<ChainData> chainDataList = new ArrayList<ChainData>();

        // Collect all chains referring to the channel
        Iterator<ChainDataImpl> chainDataIter = chainDataMap.values().iterator();
        while (chainDataIter.hasNext()) {
            chainData = chainDataIter.next();
            if (chainData.containsChannel(channelName)) {
                chainDataList.add(chainData);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAllChains");
        }
        return chainDataList.toArray(new ChainData[chainDataList.size()]);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getAllChains(Class)
     */
    @Override
    public synchronized ChainData[] getAllChains(Class<?> factoryClass) throws InvalidChannelFactoryException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getAllChains(factory)");
        }
        if (null == factoryClass) {
            throw new InvalidChannelFactoryException("Null factory class found");
        }

        String inputFactoryClassName = factoryClass.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "factory=" + inputFactoryClassName);
        }

        // Collect all chains referring to the channel
        List<ChainData> chainDataList = new ArrayList<ChainData>();
        ChannelData[] channels = null;
        for (ChainData chainData : this.chainDataMap.values()) {
            // Look at each channel associated with this chain.
            channels = chainData.getChannelList();
            for (int i = 0; i < channels.length; i++) {
                if (channels[i].getFactoryType().getName().equals(inputFactoryClassName)) {
                    chainDataList.add(chainData);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getAllChains");
        }
        return chainDataList.toArray(new ChainData[chainDataList.size()]);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getRunningChains()
     */
    @Override
    public synchronized ChainData[] getRunningChains() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRunningChains");
        }

        ChainData chainDataArray[] = new ChainData[chainRunningMap.size()];
        ChainDataImpl chainData = null;
        int index = 0;

        Iterator<Chain> chainIter = chainRunningMap.values().iterator();
        while (chainIter.hasNext()) {
            chainData = (ChainDataImpl) chainIter.next().getChainData();
            // Extract the external version, hiding all child channel data objects.
            chainDataArray[index++] = chainData.getExternalChainData();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRunningChains");
        }
        return chainDataArray;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getRunningChains(java.lang.String)
     */
    @Override
    public synchronized ChainData[] getRunningChains(String channelName) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRunningChains: " + channelName);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Null channelName found");
        }

        // Find the channel data representing the channel name.
        ChannelDataImpl parent = (ChannelDataImpl) channelDataMap.get(channelName);
        if (parent == null) {
            throw new InvalidChannelNameException("Channel not found in config, " + channelName);
        }

        ChainDataImpl chainData = null;
        List<ChainData> chainDataList = new ArrayList<ChainData>();
        ChannelContainer channelContainer = null;
        Iterator<Chain> chainIter = null;

        // The children of the parent represent the runtime channels.
        Iterator<ChildChannelDataImpl> childIter = parent.children();
        while (childIter.hasNext()) {
            // Extract the channel container for each.
            // Note, the child name is different from the parent.
            channelContainer = channelRunningMap.get(childIter.next().getName());
            // Iterate the chains.
            chainIter = channelContainer.getChainMap().values().iterator();
            while (chainIter.hasNext()) {
                chainData = (ChainDataImpl) chainIter.next().getChainData();
                chainDataList.add(chainData.getExternalChainData());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRunningChains");
        }
        return chainDataList.toArray(new ChainData[chainDataList.size()]);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getInternalRunningChains(java.
     * lang.String)
     */
    @Override
    public ChainData[] getInternalRunningChains(String channelName) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getInternalRunningChains: " + channelName);
        }
        if (null == channelName) {
            throw new InvalidChannelNameException("Null channelName found");
        }

        ChainDataImpl chainData = null;
        List<ChainData> chainDataList = new ArrayList<ChainData>();

        // Extract the channel container for the internal/child channel.
        ChannelContainer channelContainer = channelRunningMap.get(channelName);
        if (channelContainer == null) {
            throw new InvalidChannelNameException("Channel not found in runtime, " + channelName);
        }

        // Iterate the chains.
        Iterator<Chain> chainIter = channelContainer.getChainMap().values().iterator();
        while (chainIter.hasNext()) {
            chainData = (ChainDataImpl) chainIter.next().getChainData();
            chainDataList.add(chainData.getExternalChainData());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getInternalRunningChains");
        }
        return chainDataList.toArray(new ChainData[chainDataList.size()]);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getRunningChains(Class)
     */
    @Override
    public synchronized ChainData[] getRunningChains(Class<?> factoryClass) throws InvalidChannelFactoryException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRunningChains");
        }
        if (null == factoryClass) {
            throw new InvalidChannelFactoryException("Null factory class found");
        }
        String inputFactoryClassName = factoryClass.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "factory=" + inputFactoryClassName);
        }

        ChainDataImpl chainData = null;
        List<ChainData> chainDataList = new ArrayList<ChainData>();

        // Collect all runtime chains referring to the channel
        ChannelData[] channels = null;
        for (Chain chain : this.chainRunningMap.values()) {
            chainData = (ChainDataImpl) chain.getChainData();
            channels = chainData.getChannelList();
            // Look at each channel associated with this runtime chain.
            for (int i = 0; i < channels.length; i++) {
                if (channels[i].getFactoryType().getName().equals(inputFactoryClassName)) {
                    chainDataList.add(chainData.getExternalChainData());
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRunningChains");
        }
        return chainDataList.toArray(new ChainData[chainDataList.size()]);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#initChain(String)
     */
    @Override
    public synchronized void initChain(String chainName) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initChain: " + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Null chain name");
        }

        // Find the chain in the framework configuration.
        ChainData chainData = chainDataMap.get(chainName);
        if (null == chainData) {
            InvalidChainNameException e = new InvalidChainNameException("Unable to init unknown chain, " + chainName);
            FFDCFilter.processException(e, getClass().getName() + ".initChain", "2142", this, new Object[] { chainName });
            throw e;

        }

        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chainData.getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        // Verify the chain isn't already in the runtime.
        Chain chain = getRunningChain(chainName);
        if (null != chain) {
            InvalidRuntimeStateException e = new InvalidRuntimeStateException("Chain cannot be initialized, its already in the runtime.");
            FFDCFilter.processException(e, getClass().getName() + ".initChain", "2158", this, new Object[] { chain });
            throw e;
        }

        initChainInternal(chainData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initChain");
        }
    }

    /**
     * This method handles the initialization of chains for inbound and outbound
     * chains.
     *
     * @param inputChainData
     *                           that already exists in the framework.
     * @throws ChannelException
     * @throws ChainException
     */
    synchronized void initChainInternal(ChainData inputChainData) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initChainInternal");
        }
        String inputChainName = inputChainData.getName();
        Chain chain = null;
        ChainData newChainData = null;
        ChannelData inputChannelDataArray[] = inputChainData.getChannelList();
        ChannelData newChannelDataArray[] = new ChannelData[inputChannelDataArray.length];
        ChannelDataImpl parent = null;
        ChildChannelDataImpl child = null;
        // Track whether new children are created (or simply reused) in case we need
        // to rollback from an excepction
        boolean childrenNew[] = new boolean[inputChannelDataArray.length];
        // Initialize array to false.
        for (int i = 0; i < childrenNew.length; i++) {
            childrenNew[i] = false;
        }

        try {
            if (FlowType.INBOUND.equals(inputChainData.getType())) {

                // Construct an inbound chain - note nothing is inited yet
                // Use parents to create new channel data array of children (reuse
                // existing children where possible)
                newChannelDataArray = generateChildDataArray(inputChannelDataArray, childrenNew);
                // Create new chain data with child channel data objects.
                newChainData = new ChainDataImpl((ChainDataImpl) inputChainData, newChannelDataArray);
                try {
                    chain = new InboundChain(newChainData, this);
                } catch (ChannelException e) {
                    // The channel exception may specify that FFDC should be suppressed
                    if (!e.suppressFFDC()) {
                        FFDCFilter.processException(e, getClass().getName() + ".initChainInternal", "2206", this, new Object[] { newChainData });
                        Tr.error(tc, "chain.initialization.error", new Object[] { inputChainData.getName(), e.toString() });
                    } else if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "An exception occurred when initializing chain", new Object[] { inputChainData.getName(), e.toString() });
                    }
                    // Clean up the already created child refs in the parent.
                    cleanChildRefsInParent(newChannelDataArray, childrenNew);
                    throw e;
                } catch (IncoherentChainException e) {
                    // No FFDC Needed as it is called in the constructor above.
                    // Clean up the already created child refs in the parent.
                    cleanChildRefsInParent(newChannelDataArray, childrenNew);
                    // Throw exception to caller.
                    throw e;
                }
            } else {

                // Construct an outbound chain - note nothing is inited yet
                // Get children for each outbound parent channel data.
                // Loop through each channel of this new chain - note, these are parent
                // names
                for (int i = 0; i < inputChannelDataArray.length; i++) {
                    // Get the parent channel data object.
                    parent = (ChannelDataImpl) channelDataMap.get(inputChannelDataArray[i].getName());
                    // Get an existing one from the runtime.
                    newChannelDataArray[i] = parent.getOutboundChild();
                    if (null == newChannelDataArray[i]) {
                        // None available in runtime. Create one.
                        newChannelDataArray[i] = parent.createChild();
                        childrenNew[i] = true;
                    } else {
                        childrenNew[i] = false;
                    }
                }
                // Create new outbound chain data with child channel data objects.
                newChainData = new ChainDataImpl(inputChainName, inputChainData.getType(), newChannelDataArray, null);
                try {
                    chain = new OutboundChain(newChainData, this);
                } catch (ChannelException e) {
                    // The channel exception may specify that FFDC should be suppressed
                    if (!e.suppressFFDC()) {
                        FFDCFilter.processException(e, getClass().getName() + ".initChainInternal", "2241", this, new Object[] { newChainData });
                        Tr.error(tc, "chain.initialization.error", new Object[] { inputChainData.getName(), e.toString() });
                    } else if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "An exception occurred when initializing chain", new Object[] { inputChainData.getName(), e.toString() });
                    }
                    // Clean up the already created child refs in the parent.
                    cleanChildRefsInParent(newChannelDataArray, childrenNew);
                    throw e;
                } catch (IncoherentChainException e) {
                    // No FFDC Needed as it is called in the constructor called above.
                    // Clean up the already created child refs in the parent.
                    cleanChildRefsInParent(newChannelDataArray, childrenNew);
                    // Continue to throw exception to caller.
                    throw e;
                }
            }

            // Initialize the chain and its channels
            Channel[] chainChannels = chain.getChannels();
            List<Channel> chainsDone = new ArrayList<Channel>();
            Channel channelX = null; // may need this in the exception clause
            try {
                for (int i = 0; i < chainChannels.length; ++i) {
                    channelX = chainChannels[i];
                    initChannelInChain(channelX, chain);
                    chainsDone.add(channelX);
                    channelX = null;
                }
                // Update the chain state and its event listeners.
                chain.init();
            } catch (ChannelException e) {
                if (e instanceof RetryableChannelException) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught RetryableChannelException indicating the port is busy");
                    }
                } else {
                    Tr.error(tc, "chain.initialization.error", new Object[] { chain.getName(), e.toString() });
                    // The channel exception may specify that FFDC should be suppressed
                    if (!e.suppressFFDC()) {
                        FFDCFilter.processException(e, getClass().getName() + ".initChainInternal", "2266", this, new Object[] { chainChannels, chainsDone, chain });
                    }
                }

                // Handle partially created chain. Undo any inits that have been done
                // thus far.
                for (int i = 0; i < newChannelDataArray.length; i++) {
                    if (childrenNew[i]) {
                        child = (ChildChannelDataImpl) newChannelDataArray[i];
                        child.getParent().removeChild(child);
                    }
                }

                // try to destroy a channel that failed but was not add to the "done" list.
                if (channelX != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "calling destory on channel outside of destroyChannelInChain: " + channelX);
                    }
                    channelX.destroy();
                }

                for (int i = 0; i < chainsDone.size(); i++) {
                    try {
                        destroyChannelInChain(chainsDone.get(i), chain, newChannelDataArray[i]);
                        // Clean up the already created child refs in the parent.
                    } catch (Exception e1) {
                        // No FFDC needed.
                    }
                }
                // Throw the exception up to the caller.
                throw e;
            }

            // Add the new chain to the framework.
            this.chainRunningMap.put(inputChainName, chain);
        } catch (IncoherentChainException e) {
            FFDCFilter.processException(e, getClass().getName() + ".initChainInternal", "2290", this, new Object[] { inputChainData, chain });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during chain init: " + e);
            }
            // Can only happen in cases where the factory has multiple dev side
            // interfaces.
            // And the channel being created uses one that isn't compatible in the
            // chain.
            throw e;
        } catch (InvalidChannelNameException e) {
            FFDCFilter.processException(e, getClass().getName() + ".initChainInternal", "2298", this, new Object[] { inputChainData, chain });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception during chain init: " + e);
            }
            // This should never happen. If the chain config exists, then the channel
            // configs must exist.
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initChainInternal");
        }
    }

    /**
     * This is a helper function. The same logic needs to be done in multiple
     * places so
     * this method was written to break it out. It will be called in times when an
     * exception
     * occurred during the construction of a chain. It cleans up some of the
     * objects that
     * were lined up ahead of time.
     *
     * @param channelDataArray
     * @param childrenNew
     */
    private void cleanChildRefsInParent(ChannelData channelDataArray[], boolean childrenNew[]) {
        ChildChannelDataImpl child = null;
        // Clean up the already child refs in the parent.
        for (int i = 0; i < channelDataArray.length; i++) {
            if (childrenNew[i] == true) {
                child = (ChildChannelDataImpl) channelDataArray[i];
                child.getParent().removeChild(child);
            }
        }
    }

    /**
     * Based on the input array of parent channel data objects, extract
     * children for a new chain. Checking is done here to ensure that
     * no chain convergence happens. In those cases, a new child is
     * created as opposed to reused from an existing chain.
     *
     * @param parentChannelData
     *                              array of channel data objects representing parents
     * @param childrenCreated
     *                              array to track where new children are created
     * @return ChannelData[]
     */
    public ChannelData[] generateChildDataArray(ChannelData[] parentChannelData, boolean[] childrenCreated) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "generateChildDataArray");
        }
        ChannelData newChannelDataArray[] = new ChannelData[parentChannelData.length];
        ChannelDataImpl parent = null;
        ChildChannelDataImpl child = null;
        ChainDataImpl runningChainData = null;
        List<ChainData> chainDataList = new ArrayList<ChainData>();

        for (int i = 0; i < parentChannelData.length; i++) {
            // Get the parent channel data object.
            parent = (ChannelDataImpl) channelDataMap.get(parentChannelData[i].getName());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Find or create a child for parent " + parent.getName());
            }
            // Check if it is in the runtime yet.
            if (parent.getNumChildren() == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parent not in runtime");
                }
                // Channel not in runtime yet. Create it.
                newChannelDataArray[i] = parent.createChild();
                childrenCreated[i] = true;
                // All next channels will either converge or need to be created.
                for (int j = i + 1; j < newChannelDataArray.length; j++) {
                    // Get the parent channel data object.
                    parent = (ChannelDataImpl) channelDataMap.get(parentChannelData[j].getName());
                    // Create the child for use in this chain.
                    newChannelDataArray[j] = parent.createChild();
                    childrenCreated[j] = true;
                }
                // Children have all been created. Break out of for loop.
                break;
            } else if (i == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found connector channel");
                }
                // Get list of all running chain data using this channel ... for future
                // interrogation
                chainDataList = getRunningChains(parent);
                // Found connector channel. Special case. Only ever zero or one instance
                // in runtime
                newChannelDataArray[i] = parent.getInboundChild();
                if (newChannelDataArray[i] == null) {
                    newChannelDataArray[i] = parent.createChild();
                    childrenCreated[i] = true;
                } else {
                    childrenCreated[i] = false;
                }
            } else {
                // Non connector channel found in runtime. Need to pick the right one.
                // Weed chain data list down to those that include this channel as the
                // i-th channel
                boolean foundChild = false;
                String inputChannelName = parentChannelData[i].getName();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found non connector channel, " + inputChannelName);
                }
                while (chainDataList.size() > 0) {
                    runningChainData = (ChainDataImpl) chainDataList.get(0);
                    // Weed out outbound chains using this parent channel config.
                    if (runningChainData.getType().equals(FlowType.OUTBOUND)) {
                        // Can't consider this chain since it is outbound.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Removing chain that is outbound, " + runningChainData.getName());
                        }
                        chainDataList.remove(runningChainData);
                        continue;
                    }
                    // Extract the i-th channel from the current running chain data
                    if (i + 1 > runningChainData.getChannelList().length) {
                        // Chain doesn't have any more channels. Nothing at i-th position.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Removing chain that is too short, " + runningChainData.getName());
                        }
                        chainDataList.remove(runningChainData);
                        continue;
                    }
                    child = (ChildChannelDataImpl) runningChainData.getChannelList()[i];
                    // Check if the runtime channel is a child of the input parent.
                    if (!inputChannelName.equals(child.getExternalName())) {
                        // Child did not come from input parent. Remove divergent chain.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Removing divergent chain, " + runningChainData.getName());
                        }
                        chainDataList.remove(runningChainData);
                    } else {
                        // Found a match. Use it and break.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Found reusable channel from chain, " + runningChainData.getName());
                        }
                        newChannelDataArray[i] = child;
                        childrenCreated[i] = false;
                        foundChild = true;
                        break;
                    }
                }
                // At this point, chainDataList only includes the runtime chains that
                // match
                // up to the i-th channel of the input chain data.
                // Example: if i=1 and we have ABX, the list may contain ABC and ABD.
                // If the list is empty, create a new child.
                if (!foundChild) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Channel not in runtime so create it, " + parent.getName());
                    }
                    // Channel not in runtime yet. Create it.
                    newChannelDataArray[i] = parent.createChild();
                    childrenCreated[i] = true;
                    // All next channels will either converge or need to be created.
                    for (int j = i + 1; j < newChannelDataArray.length; j++) {
                        // Get the parent channel data object.
                        parent = (ChannelDataImpl) channelDataMap.get(parentChannelData[j].getName());
                        // Create the child for use in this chain.
                        newChannelDataArray[j] = parent.createChild();
                        childrenCreated[j] = true;
                    }
                    // Children have all been created. Break out of for loop.
                    break;
                }
            }
        } // end for loop creating new channel array of children

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "generateChildDataArray");
        }
        return newChannelDataArray;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#startChain(com.ibm.websphere.channelfw
     * .ChainData)
     */
    @Override
    public synchronized void startChain(ChainData chain) throws ChannelException, ChainException {
        String chainName = (null != chain) ? chain.getName() : null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startChain: " + chainName);
        }
        if (null == chain) {
            throw new InvalidChainNameException("Null chain");
        }

        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chain.getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        startChainInternal(chain);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startChain");
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#startChain(String)
     */
    @Override
    public synchronized void startChain(String chainName) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startChain: " + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Null chain name");
        }

        // Get the chain configuration
        ChainData chainData = this.chainDataMap.get(chainName);
        if (null == chainData) {
            InvalidChainNameException e = new InvalidChainNameException("Nonexistent chain configuration");
            throw e;
        }

        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chainData.getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        startChainInternal(chainData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startChain");
        }
    }

    /**
     * This method starts both inbound and outbound chains.
     *
     * @param chainData
     * @throws ChannelException
     * @throws ChainException
     */
    synchronized void startChainInternal(ChainData chainData) throws ChannelException, ChainException {
        startChainInternal(chainData, ChainStartMode.FAIL_EACH_SILENT);
    }

    /**
     * This method starts both inbound and outbound chains.
     *
     * @param targetChainData
     * @param startMode
     *                            - indicate how to handle failure conditions
     * @throws ChannelException
     * @throws ChainException
     */
    public synchronized void startChainInternal(ChainData targetChainData, ChainStartMode startMode) throws ChannelException, ChainException {
        String chainName = targetChainData.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startChainInternal: " + chainName);
        }
        if (!targetChainData.isEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Chain " + chainName + " is disabled");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "startChainInternal");
            }
            return;
        }
        ChainData chainData;
        // Find the chain in the framework configuration.
        Chain chain = getRunningChain(chainName);
        if (null == chain) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Chain not found running.  Double check it is configured.");
            }
            // Get the chain configuration
            chainData = chainDataMap.get(chainName);
            if (null == chainData) {
                // Did not find the chain in the config. This method must have been
                // called from something other than startChain (which does this check).
                // As of 7/20/04 the only other location is the ChainStartAlarmListener.
                // This is now a case where the framework must have been shut
                // down while an alarm was set to start the chain later. The alarm will
                // handle this exception.
                throw new InvalidChainNameException("Unable to start unknown chain, " + chainName);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Chain exists, but not in runtime yet.  Initialize it.");
            }
            initChainInternal(chainData);
            chain = getRunningChain(chainName);
            if (null == chain) {
                // This shouldn't happen.
                InvalidChainNameException e = new InvalidChainNameException("Unable to start unknown chain, " + chainName);
                throw e;
            }
        }

        // Update the chainData to include what's in the runtime (children channel data, not parent)
        // Note that the input came from a user who only knows about parent channel data.
        chainData = chain.getChainData();

        List<Channel> chainsDone = new ArrayList<Channel>();
        try {
            RuntimeState chainState = chain.getState();
            if (RuntimeState.INITIALIZED.equals(chainState)) {
                // Check for inbound vs outbound. Channels are started in different
                // orders based on this.
                if (chainData.getType().equals(FlowType.INBOUND)) {
                    // Inbound chain. Ensure the disc process of each channel is
                    // initialized.
                    ((InboundChain) chain).setupDiscProcess();
                    Channel[] chainChannels = chain.getChannels();
                    ChannelData[] channelData = chain.getChannelsData();
                    // Loop through the channels starting from the app channel and down to
                    // the dev channel.
                    for (int i = chainChannels.length - 1; i >= 0; --i) {
                        if (startChannelInChain(chainChannels[i], chain)) {
                            chainsDone.add(chainChannels[i]);
                            // Only take the next step if the current channel is not the
                            // device
                            // side channel.
                            if (i != 0) {
                                // Start the disc process between
                                ((InboundChain) chain).startDiscProcessBetweenChannels((InboundChannel) chainChannels[i], (InboundChannel) chainChannels[i - 1],
                                                                                       channelData[i].getDiscriminatorWeight());
                            }
                        } else {
                            // The channel was not started. I must already be running. No more
                            // work to do.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Channel was already started; " + chainChannels[i].getName());
                            }
                            break;
                        }
                    }
                } else {
                    // Outbound chain.
                    Channel[] chainChannels = chain.getChannels();
                    // Loop through the channel starts from the app side to the dev side.
                    for (int i = 0; i < chainChannels.length; ++i) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Start channel in chain: " + chainChannels[i].getName());
                        }
                        // Start the channels.
                        if (startChannelInChain(chainChannels[i], chain)) {
                            chainsDone.add(chainChannels[i]);
                        }
                    }
                }
            } else if (!RuntimeState.STARTED.equals(chainState)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cannot start chain " + chainData.getName() + ", state: " + chainState.ordinal);
                }
                InvalidRuntimeStateException e = new InvalidRuntimeStateException("Cannot start chain " + chainData.getName());
                throw e;
            }

        } catch (RetryableChannelException e) {
            // Don't ffdc or log error messages in cases where a try will take place
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught RetryableException" + e.getMessage());
            }
            // Handle partially created chain. Undo any starts that have been done
            // thus far.
            for (int i = 0; i < chainsDone.size(); i++) {
                try {
                    // Note, the former channel state may have been quiesced. Stopping
                    // anyhow.
                    stopChannel(chainsDone.get(i));
                } catch (Exception e1) {
                    FFDCFilter.processException(e, getClass().getName() + ".startChainInternal", "2855", this, new Object[] { chainsDone.get(i) });
                }
            }
            // Throw the exception up to the caller.
            throw e;

        } catch (ChannelException e) {
            // Don't ffdc or log error messages in cases where a try will take place
            if (startMode != ChainStartMode.RETRY_EACH_ON_FAIL) {
                FFDCFilter.processException(e, getClass().getName() + ".startChainInternal", "2577", this, new Object[] { chainData });
                ((ChainDataImpl) chainData).chainStartFailed(1, 0);
                Tr.error(tc, "chain.start.error", new Object[] { chain.getName(), e.toString() });
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "(no ffdc) Caught ChannelException: " + e);
            }
            // Handle partially created chain. Undo any starts that have been done
            // thus far.
            for (int i = 0; i < chainsDone.size(); i++) {
                try {
                    // Note, the former channel state may have been quiesced. Stopping
                    // anyhow.
                    stopChannel(chainsDone.get(i));
                } catch (Exception e1) {
                    FFDCFilter.processException(e, getClass().getName() + ".startChainInternal", "2589", this, new Object[] { chainsDone.get(i) });
                }
            }
            // Throw the exception up to the caller.
            throw e;

        } catch (ChainException e) {
            FFDCFilter.processException(e, getClass().getName() + ".startChainInternal", "2595", this, new Object[] { chainData });
            Tr.error(tc, "chain.start.error", new Object[] { chain.getName(), e.toString() });
            // Handle partially created chain. Undo any starts that have been done
            // thus far.
            for (int i = 0; i < chainsDone.size(); i++) {
                try {
                    stopChannel(chainsDone.get(i));
                } catch (Exception e1) {
                    FFDCFilter.processException(e, getClass().getName() + ".startChainInternal", "2602", this, new Object[] { chainsDone.get(i) });
                }
            }
            // Throw the exception up to the caller.
            throw e;
        }

        chain.start();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startChainInternal");
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#stopChain(com.ibm.websphere.channelfw
     * .ChainData, long)
     */
    @Override
    public synchronized void stopChain(ChainData chaindata, long millisec) throws ChannelException, ChainException {
        String chainName = (null != chaindata) ? chaindata.getName() : null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stopChain: " + chainName + " time=" + millisec);
        }
        if (null == chaindata) {
            throw new InvalidChainNameException("Null chain");
        }
        if (millisec < 0L) {
            throw new ChainTimerException("Invalid time length give to stopChain, " + millisec);
        }

        // Verify the chain is found in the runtime.
        Chain chain = getRunningChain(chainName);
        if (null == chain) {
            // Due to timing windows (i.e. multiple channel providers going away), there is
            // a race to stop chains. This is not a notable condition: the chain can only be stopped
            // once.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "stopChain " + chainName, "chain is not running");
            }
            return;
        }

        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chaindata.getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        stopChainInternal(chain, millisec);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stopChain " + chainName);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#stopChain(String, long)
     */
    @Override
    public synchronized void stopChain(String chainName, long millisec) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stopChain: " + chainName + " time=" + millisec);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Null chain name");
        }
        if (millisec < 0) {
            throw new ChainTimerException("Invalid time length give to stopChain, " + millisec);
        }

        // Verify the chain is found in the runtime.
        Chain chain = getRunningChain(chainName);
        if (null == chain) {
            // Due to timing windows (i.e. multiple channel providers going away), there is
            // a race to stop chains. This is not a notable condition: the chain can only be stopped
            // once.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "stopChain " + chainName, "chain is not running");
            }
            return;
        }

        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chain.getChainData().getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        stopChainInternal(chain, millisec);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stopChain");
        }
    }

    /**
     * This method stops both inbound and outbound chains.
     *
     * @param chain
     * @param millisec
     * @throws ChannelException
     * @throws ChainException
     */
    synchronized void stopChainInternal(Chain chain, long millisec) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stopChainInternal: " + chain.getName() + ", time=" + millisec);
        }

        RuntimeState chainState = chain.getState();
        Channel[] chainChannels = chain.getChannels();
        if ((RuntimeState.STARTED == chainState) || (RuntimeState.QUIESCED == chainState)) {
            // Check if this is just a quiesce
            if (millisec > 0L) {
                if (RuntimeState.QUIESCED == chainState) {
                    // Due to timing windows (i.e. multiple channel providers going away), there is
                    // a race to stop chains. This is not a notable condition: the chain can only be stopped
                    // once.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "stopChain " + chain.getName(), "chain already quiesced");
                    }
                    return;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Quiescing chain: " + chain.getName());
                }

                int i = 0;
                try {
                    // In start state. Just a quiesce. Iterate the channels and send a
                    // stop warning
                    for (i = 0; i < chainChannels.length; ++i) {
                        // Only alert channels that are active only for this chain.
                        // Otherwise a future stop will never take affect anyhow
                        if (getNumStartedChainsUsingChannel(chainChannels[i].getName()) == 1) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Quiescing channel: " + chainChannels[i].getName());
                            }
                            chainChannels[i].stop(millisec);
                            // Update the state of the channel.
                            setChannelState(chainChannels[i].getName(), RuntimeState.QUIESCED);
                        }
                    }
                    // Start the timer for when the stop will be asserted.
                    StopChainTask task = new StopChainTask(chain.getName(), this);
                    chain.setStopTask(task);
                    // Update the state of the chain.
                    chain.quiesce();
                    this.stopTimer.schedule(task, millisec);
                } catch (ChannelException e) {
                    FFDCFilter.processException(e, getClass().getName() + ".stopChainInternal", "2711", this, new Object[] { chain, this });
                    Tr.error(tc, "chain.stop.error", new Object[] { chain.getName(), e.toString() });
                    // Need to undo channels stopped so far.
                    for (int j = 0; j < i; j++) {
                        chainChannels[j].start();
                        // Update the state of the channel.
                        setChannelState(chainChannels[j].getName(), RuntimeState.STARTED);
                    }
                    throw e;
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Stopping chain: " + chain.getName());
                }

                if (RuntimeState.QUIESCED == chainState) {
                    // A stop is being requested while a stop task is in place. Need to
                    // cancel it first.
                    StopChainTask task = chain.getStopTask();
                    if (task != null) {
                        task.cancel();
                        chain.setStopTask(null);
                    }
                }
                // Disable the channels from the device side up to the application side.
                List<Channel> channelsToStop = new ArrayList<Channel>(chainChannels.length);
                for (int i = 0; i < chainChannels.length; ++i) {
                    // Only stop a channel if it is in use by just this chain.
                    if (getNumStartedChainsUsingChannel(chainChannels[i].getName()) <= 1) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Placing channel in list to stop: " + chainChannels[i].getName());
                        }
                        if (chain.getChainData().getType().equals(FlowType.OUTBOUND)) {
                            channelsToStop.add(chainChannels[i]);
                        } else if (disableChannelInChain(chainChannels[i], chain)) {
                            channelsToStop.add(chainChannels[i]);
                        }
                    }
                }
                int i = 0;
                try {
                    // Stop the channels from the application side to the device side.
                    for (i = channelsToStop.size() - 1; i >= 0; i--) {
                        stopChannel(channelsToStop.get(i));
                    }
                    chain.stop();
                } catch (ChannelException e) {
                    FFDCFilter.processException(e, getClass().getName() + ".stopChainInternal", "2763", this, new Object[] { chain, this });
                    Tr.error(tc, "chain.stop.error", new Object[] { chain.getName(), e.toString() });
                    // Need to restart channels that were stopped thus far.
                    if (FlowType.INBOUND.equals(chain.getChainData().getType())) {
                        // Re-enable the channel
                        ((InboundChain) chain).setupDiscProcess();
                    }
                    for (int j = 0; j < i; j++) {
                        startChannelInChain(channelsToStop.get(j), chain);
                    }
                    // Alert the caller.
                    throw e;
                }
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // Due to timing windows (i.e. multiple channel providers going away), there is
            // a race to stop chains. This is not a notable condition: the chain can only be stopped
            // once.
            Tr.debug(tc, "stopChainInternal " + chain.getName(), "chain is not running");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stopChainInternal");
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#destroyChain(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public synchronized void destroyChain(ChainData chaindata) throws ChannelException, ChainException {
        String chainName = (null != chaindata) ? chaindata.getName() : null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "destroyChain: " + chainName);
        }

        if (null == chaindata) {
            throw new InvalidChainNameException("Null chain");
        }
        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chaindata.getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        // Verify the chain is in the runtime.
        Chain chain = getRunningChain(chainName);
        if (null == chain) {
            // Due to timing windows (i.e. multiple channel providers going away), there is
            // a race to destroy chains. This is not a notable condition: the chain can only be destroyed
            // once.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "destroyChain: " + chainName + " does not exist -- may already have been destroyed");
            }
            return;
        }

        destroyChainInternal(chain);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#destroyChain(String)
     */
    @Override
    public synchronized void destroyChain(String chainName) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "destroyChain: " + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Null chain name");
        }

        // Verify the chain is in the runtime.
        Chain chain = getRunningChain(chainName);
        if (null == chain) {
            // Due to timing windows (i.e. multiple channel providers going away), there is
            // a race to destroy chains. This is not a notable condition: the chain can only be destroyed
            // once.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "destroyChain: " + chainName + " does not exist -- may already have been destroyed");
            }
            return;
        }

        // Ensure we have an inbound chain.
        if (FlowType.OUTBOUND.equals(chain.getChainData().getType())) {
            throw new InvalidChainNameException("Outbound chain cannot use this interface.");
        }

        destroyChainInternal(chain);
    }

    /**
     * This method destroys both inbound and outbound chains..
     *
     * @param chain
     * @throws ChannelException
     * @throws ChainException
     */
    public synchronized void destroyChainInternal(Chain chain) throws ChannelException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "destroyChainInternal: " + chain.getName());
        }
        if (RuntimeState.INITIALIZED.equals(chain.getState())) {
            Channel[] chainChannels = chain.getChannels();
            ChannelData[] channelData = chain.getChannelsData();
            for (int i = 0; i < chainChannels.length; i++) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Destroy channel in chain: " + chainChannels[i].getName());
                }
                try {
                    // Don't let one channel's exception mess up the whole chain.
                    destroyChannelInChain(chainChannels[i], chain, channelData[i]);
                } catch (Exception e) {
                    FFDCFilter.processException(e, getClass().getName() + ".destroyChainInternal", "2865", this, new Object[] { chain, channelData[i] });
                    Tr.error(tc, "chain.destroy.error", new Object[] { chain.getName(), e.toString() });
                }
            }
            chain.destroy();
            // Remove the chain from the framework.
            this.chainRunningMap.remove(chain.getName());
            // Clear out the outbound VCF if this is outbound - called internally
            // only.
            if (FlowType.OUTBOUND.equals(chain.getChainData().getType())) {
                this.outboundVCFactories.remove(chain.getName());
            }
        } else {
            InvalidRuntimeStateException e = new InvalidRuntimeStateException("Unable to destroy chain: " + chain.getName() + ", state: " + chain.getState().ordinal);
            //Race condition can occur where chain is stopped before we register the listener, no need to FFDC
            //FFDCFilter.processException(e, getClass().getName() + ".destroyChainInternal", "2861", this, new Object[] { chain, this });
            throw e;
        }
    }

    /*
     * @seecom.ibm.wsspi.channelfw.ChannelFramework#addChainEventListener(
     * ChainEventListener, String)
     */
    @Override
    public synchronized void addChainEventListener(ChainEventListener cel, String chainName) throws InvalidChainNameException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addChainEventListener: chain=" + chainName + " listener=" + cel);
        }
        ChainDataImpl chainData = null;

        if (null == chainName) {
            InvalidChainNameException e = new InvalidChainNameException("Unable to register a listener for a chain with null name");
            //Race condition can occur where chain is stopped before we register the listener, no need to FFDC
            //FFDCFilter.processException(e, getClass().getName() + ".addChainEventListener", "2910", this, new Object[] { chainName, this });
            throw e;
        }

        // Handle a listener registering for events from all chains.
        if (chainName.equals(ChainEventListener.ALL_CHAINS)) {
            for (ChainData chain : this.chainDataMap.values()) {
                ((ChainDataImpl) chain).addChainEventListener(cel);
            }
            this.globalChainEventListeners.add(cel);
        } else {
            // Extract the chain config from the framework.
            chainData = this.chainDataMap.get(chainName);
            // Verify the chain config is in the framework
            if (null == chainData) {
                InvalidChainNameException e = new InvalidChainNameException("Unable to register listener for unknown chain config, " + chainName);
                //Race condition can occur where chain is stopped before we register the listener, no need to FFDC
                //FFDCFilter.processException(e, getClass().getName() + ".addChainEventListener", "2910", this, new Object[] { chainName, this });
                throw e;
            }
            chainData.addChainEventListener(cel);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#removeChainEventListener(com.ibm
     * .wsspi.channelfw.ChainEventListener, java.lang.String)
     */
    @Override
    public synchronized void removeChainEventListener(ChainEventListener cel, String chainName) throws InvalidChainNameException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeChainEventListener: chainName=" + chainName + " listener=" + cel);
        }
        ChainDataImpl chainData = null;
        if (null == chainName) {
            InvalidChainNameException e = new InvalidChainNameException("Unregister listener for null chain name");
            //Race condition can occur where chain is stopped before we register the listener, no need to FFDC
            //FFDCFilter.processException(e, getClass().getName() + ".addChainEventListener", "2910", this, new Object[] { chainName, this });
            throw e;
        }

        // Handle a listener unregistering for events from all chains.
        if (chainName.equals(ChainEventListener.ALL_CHAINS)) {
            for (ChainData chain : this.chainDataMap.values()) {
                ((ChainDataImpl) chain).removeChainEventListener(cel);
            }
            this.globalChainEventListeners.remove(cel);
        } else {
            // Extract the chain config from the framework.
            chainData = this.chainDataMap.get(chainName);
            // Verify the chain config is in the framework.
            if (null == chainData) {
                InvalidChainNameException e = new InvalidChainNameException("Unable to unregister listener for unknown chain config, " + chainName);
                FFDCFilter.processException(e, getClass().getName() + ".removeChainEventListener", "2948", this, new Object[] { chainName, this });
                throw e;
            } else if (globalChainEventListeners.contains(cel)) {
                // Can't remove a global listener from individual chains
                InvalidChainNameException e = new InvalidChainNameException("Can't remove a global listener from individual chains, " + chainName);
                FFDCFilter.processException(e, getClass().getName() + ".removeChainEventListener", "2953", this, new Object[] { chainName, this });
                throw e;
            }
            chainData.removeChainEventListener(cel);
        }
    }

    /*
     * @seecom.ibm.wsspi.channelfw.ChannelFramework#addGroupEventListener(
     * ChainEventListener,String)
     */
    @Override
    public synchronized void addGroupEventListener(ChainEventListener cel, String groupName) throws ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addGroupEventListener");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "groupName=" + groupName);
            Tr.debug(tc, "Listener=" + cel);
        }

        // Find the chain group.
        ChainGroupDataImpl groupData = (ChainGroupDataImpl) chainGroups.get(groupName);

        // Verify the chain group is in the framework.
        if (null == groupData) {
            ChainGroupException e = new ChainGroupException("Unable to register listener for unknown group, " + groupName);
            FFDCFilter.processException(e, getClass().getName() + ".registerGroupEventListener", "2982", this, new Object[] { groupName });
            throw e;
        }

        groupData.addChainEventListener(cel);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addGroupEventListener");
        }
    }

    /*
     * @seecom.ibm.wsspi.channelfw.ChannelFramework#removeGroupEventListener(
     * ChainEventListener, String)
     */
    @Override
    public synchronized void removeGroupEventListener(ChainEventListener cel, String groupName) throws ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeGroupEventListener");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "groupName=" + groupName);
            Tr.debug(tc, "Listener=" + cel);
        }

        // Find the chain group.
        ChainGroupDataImpl groupData = (ChainGroupDataImpl) chainGroups.get(groupName);

        // Verify the group is in the framework.
        if (null == groupData) {
            ChainGroupException e = new ChainGroupException("Unable to unregister listener for unknown group, " + groupName);
            FFDCFilter.processException(e, getClass().getName() + ".removeGroupEventListener", "3011", this, new Object[] { groupName });
            throw e;
        }

        groupData.removeChainEventListener(cel);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "removeGroupEventListener");
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#addChainGroup(java.lang.String,
     * java.lang.String[])
     */
    @Override
    public synchronized ChainGroupData addChainGroup(String groupName, String[] chainNames) throws InvalidChainNameException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addChainGroup: " + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null group name");
        }
        if ((null == chainNames) || (0 == chainNames.length)) {
            throw new InvalidChainNameException("Null or empty chain name list");
        }

        // Create a new group
        ChainData chainDataArray[] = new ChainData[chainNames.length];
        // Verify the chain configurations already exist.
        ChainData chainData = null;
        for (int i = 0; i < chainNames.length; i++) {
            chainData = chainDataMap.get(chainNames[i]);
            if (null != chainData) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found chain for group, " + chainNames[i]);
                }
                // Note, now supporting inbound and outbound chains.
                chainDataArray[i] = chainData;
            } else {
                InvalidChainNameException e = new InvalidChainNameException("Missing chain config during add: " + chainNames[i]);
                FFDCFilter.processException(e, getClass().getName() + ".addChainGroup", "3071", this, new Object[] { chainNames[i] });
                throw e;
            }
        }
        // At this point we have verified that all the chain configs exist.
        ChainGroupData groupData = createChainGroupData(groupName, chainDataArray);
        this.chainGroups.put(groupName, groupData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addChainGroup");
        }
        return groupData;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#removeChainGroup(java.lang.String)
     */
    @Override
    public synchronized ChainGroupData removeChainGroup(String groupName) throws ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing chain group, " + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Input group name is null");
        }

        ChainGroupData groupData = this.chainGroups.remove(groupName);
        if (null == groupData) {
            throw new ChainGroupException("Null group name");
        }
        return groupData;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#updateChainGroup(java.lang.String,
     * java.lang.String[])
     */
    @Override
    public synchronized ChainGroupData updateChainGroup(String groupName, String[] chainNames) throws ChainGroupException, InvalidChainNameException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Updating chain group, " + groupName);
        }

        // Input parameters validated internally
        return addChainGroup(groupName, chainNames);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getChainGroup(java.lang.String)
     */
    @Override
    public synchronized ChainGroupData getChainGroup(String groupName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getChainGroup: " + groupName);
        }
        if (null == groupName) {
            return null;
        }
        return this.chainGroups.get(groupName);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#addChainToGroup(java.lang.String,
     * java.lang.String)
     */
    @Override
    public synchronized ChainGroupData addChainToGroup(String groupName, String chainName) throws ChainGroupException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addChainToGroup chainName=" + chainName + ", groupName=" + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null chain group");
        }
        if (null == chainName) {
            throw new ChainException("Null chain name");
        }

        // Ensure the chain exists.
        ChainData chain = this.chainDataMap.get(chainName);
        if (null == chain) {
            throw new ChainException("Unable to find chain: " + chainName);
        }

        // Ensure the group exists.
        ChainGroupDataImpl group = (ChainGroupDataImpl) this.chainGroups.get(groupName);
        if (null == group) {
            throw new ChainGroupException("Unable to find group: " + groupName);
        }

        // Add the chain to the group.
        group.addChain(chain);

        return group;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#removeChainFromGroup(java.lang
     * .String, java.lang.String)
     */
    @Override
    public synchronized ChainGroupData removeChainFromGroup(String groupName, String chainName) throws ChainGroupException, ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeChainFromGroup chainName=" + chainName + ", groupName=" + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null chain group");
        }
        if (null == chainName) {
            throw new ChainException("Null chain name");
        }

        // Ensure the chain exists.
        ChainData chain = this.chainDataMap.get(chainName);
        if (null == chain) {
            throw new ChainException("Unable to find chain: " + chainName);
        }

        // Ensure the group exists.
        ChainGroupDataImpl group = (ChainGroupDataImpl) this.chainGroups.get(groupName);
        if (null == group) {
            throw new ChainGroupException("Unable to find group: " + groupName);
        }

        // Remove the chain from the group.
        group.removeChain(chain);
        return group;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getAllChainGroups()
     */
    @Override
    public synchronized ChainGroupData[] getAllChainGroups() {
        return this.chainGroups.values().toArray(new ChainGroupData[this.chainGroups.size()]);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getAllChainGroups(java.lang.String
     * )
     */
    @Override
    public synchronized ChainGroupData[] getAllChainGroups(String chainName) throws ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getAllChainGroups chainName=" + chainName);
        }
        if (null == chainName) {
            throw new InvalidChainNameException("Null chain name");
        }

        // Ensure the chain exists.
        ChainData chain = this.chainDataMap.get(chainName);
        if (null == chain) {
            throw new ChainException("Unable to find chain: " + chainName);
        }

        int index = 0;
        int numGroups = getNumGroupsUsingChain(chainName);
        ChainGroupData[] groupArray = new ChainGroupData[numGroups];
        for (ChainGroupData group : this.chainGroups.values()) {
            if (group.containsChain(chainName)) {
                groupArray[index++] = group;
            }
        }
        return groupArray;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#initChainGroup(java.lang.String)
     */
    @Override
    public synchronized ChainData[] initChainGroup(String groupName) throws ChannelException, ChainException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "initChainGroup: " + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null chain group");
        }

        // Verify the group exists in the framework.
        ChainGroupData groupData = this.chainGroups.get(groupName);
        if (null == groupData) {
            throw new ChainGroupException("Unable to find group: " + groupName);
        }

        List<ChainData> changedChains = new ArrayList<ChainData>();
        ChainData[] chainDataArray = groupData.getChains();
        StringBuilder sbErrors = new StringBuilder();
        boolean errorOccurred = false;

        String chainName = null;
        Chain chain = null;
        // Loop through all the chain names in the list and init them.
        for (int i = 0; i < chainDataArray.length; i++) {
            chainName = chainDataArray[i].getName();
            if (chainDataArray[i].getType().equals(FlowType.OUTBOUND)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Chain " + chainName + " is outbound so no action being taken.");
                }
                continue;
            }
            try {
                // See if the chain is already in the runtime.
                chain = getRunningChain(chainName);
                if (null == chain) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Initialize the chain, " + chainName);
                    }
                    initChainInternal(chainDataArray[i]);
                    changedChains.add(chainDataArray[i]);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Chain already in initialized state, " + chainName);
                    }
                }
            } catch (Exception e) {
                String errorString = "Error initializing chain " + chainName + " in group " + groupName + ", exception=" + e;
                // No FFDC Needed.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, errorString);
                }
                // Note the error so an exception can be thrown later with a list of
                // chains that couldn't starte.
                errorOccurred = true;
                FFDCFilter.processException(e, getClass().getName() + ".initChainGroup", "3327", this, new Object[] { chainName, groupName });
                sbErrors.append("\r\n");
                sbErrors.append(errorString);
                // Get through as many chains as possible as opposed to fail fast.
                continue;
            }
        }

        // Handle any errors if they occurred.
        if (errorOccurred) {
            throw new ChainGroupException(sbErrors.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "initChainGroup");
        }
        return changedChains.toArray(new ChainData[changedChains.size()]);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#startChainGroup(java.lang.String)
     */
    @Override
    public synchronized ChainData[] startChainGroup(String groupName) throws ChannelException, ChainException, ChainGroupException {
        return startChainGroup(groupName, ChainStartMode.FAIL_EACH_SILENT);
    }

    /**
     * This method is very similar to the method of the same name in the
     * ChannelFrameworkInterface
     * with one difference. It takes and extra parameter indicating how to handle
     * conditions when
     * one of the chains throws and special exception when starting indiating that
     * a retry could
     * cause it to start correctly.
     *
     * @param groupName
     * @param startMode
     * @return ChainData[]
     * @throws ChannelException
     * @throws ChainException
     * @throws ChainGroupException
     */
    public ChainData[] startChainGroup(String groupName, ChainStartMode startMode) throws ChannelException, ChainException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startChainGroup: name=" + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null chain group");
        }

        // Verify the group exists in the framework.
        ChainGroupData groupData = this.chainGroups.get(groupName);
        if (null == groupData) {
            throw new ChainGroupException("Unable to find group: " + groupName);
        }

        ChainData[] chainDataArray = groupData.getChains();
        List<ChainData> changedChains = new ArrayList<ChainData>();
        String chainName = null;
        Chain chain = null;
        RuntimeState chainState = null;
        StringBuilder sbErrors = new StringBuilder();
        boolean errorOccurred = false;
        // Loop through all the chain names in the list and start them.
        for (int i = 0; i < chainDataArray.length; i++) {
            chainName = chainDataArray[i].getName();
            if (chainDataArray[i].getType().equals(FlowType.OUTBOUND)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping outbound chain " + chainName);
                }
                continue;
            }
            if (!chainDataArray[i].isEnabled()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping disabled chain " + chainName);
                }
                continue;
            }
            try {
                chain = getRunningChain(chainName);
                if (null == chain) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Chain not found so build it.");
                    }
                    initChainInternal(chainDataArray[i]);
                    chain = getRunningChain(chainName);
                    if (null == chain) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Error starting chain " + chainName + " in group " + groupName);
                        }
                        // Get through as many chains as possible as opposed
                        // to fail fast.
                        continue;
                    }
                }
                // Determine if the chain can be started.
                chainState = chain.getState();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Consider starting chain: " + chainName + ", state: " + chainState.ordinal);
                }
                // This check also exists in the startChain method, but is
                // needed here to know if the changedChains should be
                // populated. Note, no return type on startChain.
                if (RuntimeState.INITIALIZED.equals(chainState) || RuntimeState.QUIESCED.equals(chainState)) {
                    startChainInternal(chainDataArray[i], startMode);
                    changedChains.add(chainDataArray[i]);
                }
            } catch (Exception e) {
                if ((e instanceof RetryableChannelException) && (startMode == ChainStartMode.RETRY_EACH_ON_FAIL)) {
                    retryChainStart(chainDataArray[i], e);
                    continue;
                }
                String errorString = "Error starting chain " + chainName + " in group " + groupName + ", exception=" + e;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, errorString);
                }
                // Note the error so an exception can be thrown later with
                // a list of chains that couldn't start.
                FFDCFilter.processException(e, getClass().getName() + ".startChainGroup", "3436", this, new Object[] { chainName, groupName });
                errorOccurred = true;
                sbErrors.append("\r\n");
                sbErrors.append(errorString);
                // Get through as many chains as possible as opposed to
                // fail fast.
                continue;
            }
        }

        // Handle any errors if they occurred.
        if (errorOccurred) {
            throw new ChainGroupException(sbErrors.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startChainGroup");
        }
        return changedChains.toArray(new ChainData[changedChains.size()]);
    }

    /**
     * This method is called when an initial attempt to start a chain has
     * failed due to a particular type of exception, RetryableChannelException,
     * indicating that a retry may enable the chain to be started. This could
     * result from something like a device side channel having bind problems
     * since a socket from a previous chain stop is still wrapping up.
     *
     * @param chainData
     *                      specification of chain to be started
     * @param e
     *                      Exception that occured which caused this retry attempt to take
     *                      place
     */
    protected void retryChainStart(ChainData chainData, Exception e) {
        // Do nothing in the core implementation. This method is overloaded in the
        // service.
        // 7/16/04 CAL - this prevents a dependency on the Alarm Manager and
        // channelfw.service
        FFDCFilter.processException(e, getClass().getName() + ".retryChainStart", "3470", this, new Object[] { chainData });
        Tr.error(tc, "chain.retrystart.error", new Object[] { chainData.getName(), Integer.valueOf(1) });
        ((ChainDataImpl) chainData).chainStartFailed(1, 0);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#stopChainGroup(String, long)
     */
    @Override
    public synchronized ChainData[] stopChainGroup(String groupName, long millisec) throws ChannelException, ChainException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stopChainGroup: " + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null chain group");
        }

        // Verify the group exists in the framework
        ChainGroupData groupData = this.chainGroups.get(groupName);
        if (null == groupData) {
            throw new ChainGroupException("Unable to find group: " + groupName);
        }

        // Verify the input number of millisec is valid.
        if (millisec < 0) {
            throw new ChainTimerException("Invalid time length give to stopChain, " + millisec);
        }

        List<ChainData> changedChains = new ArrayList<ChainData>();
        ChainData[] chainDataArray = groupData.getChains();
        Chain chain = null;
        String chainName = null;
        StringBuilder sbErrors = new StringBuilder();
        boolean errorOccurred = false;
        RuntimeState chainState = null;
        // Loop through all the chain names.
        for (int i = 0; i < chainDataArray.length; i++) {
            chainName = chainDataArray[i].getName();
            if (chainDataArray[i].getType().equals(FlowType.OUTBOUND)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Chain " + chainName + " is outbound so no action being taken.");
                }
                continue;
            }
            try {
                // Check if the state is in intialized
                chain = getRunningChain(chainName);
                if (null != chain) {
                    // Determine if the chain can be stopped.
                    chainState = chain.getState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Consider stopping chain: " + chainName + ", state: " + chainState.ordinal);
                    }
                    // This check also exists in the stopChain method, but is needed here
                    // to know
                    // if the changedChains should be populated. Note, no return type on
                    // stopChain.
                    if (RuntimeState.STARTED.equals(chainState) || RuntimeState.QUIESCED.equals(chainState)) {
                        if (RuntimeState.QUIESCED.equals(chainState) && millisec > 0) {
                            // Redundant quiesce.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Stop notification already given for chain: " + chainName);
                            }
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Stop the chain, " + chainName);
                            }
                            stopChainInternal(chain, millisec);
                            changedChains.add(chainDataArray[i]);
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Chain doesn't exist, " + chainName);
                    }
                }
            } catch (Exception e) {
                String errorString = "Error stopping chain " + chainName + " in group " + groupName + ", exception=" + e;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, errorString);
                }
                // Note the error so an exception can be thrown later with a list of
                // chains that couldn't starte.
                errorOccurred = true;
                FFDCFilter.processException(e, getClass().getName() + ".stopChainGroup", "3558", this, new Object[] { chainName, groupName });
                sbErrors.append("\r\n");
                sbErrors.append(errorString);
                // Get through as many chains as possible as opposed to fail fast.
                continue;
            }
        }

        // Handle any errors if they occurred.
        if (errorOccurred) {
            throw new ChainGroupException(sbErrors.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stopChainGroup");
        }
        return changedChains.toArray(new ChainData[changedChains.size()]);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#destroyChainGroup(java.lang.String
     * )
     */
    @Override
    public synchronized ChainData[] destroyChainGroup(String groupName) throws ChannelException, ChainException, ChainGroupException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroyChainGroup: " + groupName);
        }
        if (null == groupName) {
            throw new ChainGroupException("Null chain group");
        }

        // Verify the group exists in the framework.
        ChainGroupData groupData = this.chainGroups.get(groupName);
        if (null == groupData) {
            throw new ChainGroupException("Unable to find group: " + groupName);
        }

        List<ChainData> changedChains = new ArrayList<ChainData>();
        ChainData[] chainDataArray = groupData.getChains();
        Chain chain = null;
        String chainName = null;
        StringBuilder sbErrors = new StringBuilder();
        boolean errorOccurred = false;
        RuntimeState chainState = null;
        // Loop through all the chain names.
        for (int i = 0; i < chainDataArray.length; i++) {
            chainName = chainDataArray[i].getName();
            if (chainDataArray[i].getType().equals(FlowType.OUTBOUND)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Chain " + chainName + " is outbound so no action being taken.");
                }
                continue;
            }
            try {
                chain = getRunningChain(chainName);
                if (null != chain) {
                    // Determine if the chain can be destroyed.
                    chainState = chain.getState();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Consider destroying chain: " + chainName + ", state: " + chainState.ordinal);
                    }
                    // This check also exists in the destroyChain method, but is needed
                    // here to know
                    // if the changedChains should be populated. Note, no return type on
                    // destroyChain.
                    if (RuntimeState.INITIALIZED == chainState) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Destroy the chain, " + chainName);
                        }
                        destroyChainInternal(chain);
                        changedChains.add(chainDataArray[i]);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Chain doesn't exist, " + chainName);
                    }
                }
            } catch (Exception e) {
                String errorString = "Error destroying chain " + chainName + " in group " + groupName + ", exception=" + e;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, errorString);
                }
                // Note the error so an exception can be thrown later with a list of
                // chains that couldn't destroy.
                errorOccurred = true;
                FFDCFilter.processException(e, getClass().getName() + ".destroyChainGroup", "3647", this, new Object[] { chainName, groupName });
                sbErrors.append("\r\n");
                sbErrors.append(errorString);
                // Get through as many chains as possible as opposed to fail fast.
                continue;
            }
        }

        // Handle any errors if they occurred.
        if (errorOccurred) {
            throw new ChainGroupException(sbErrors.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroyChainGroup");
        }
        return changedChains.toArray(new ChainData[changedChains.size()]);
    }

    /**
     * Extract a list of runtime chain data objects (including exclusively
     * child channel data lists) that are using the input parent channel.
     *
     * @param parent
     *                   The parent channel data object
     * @return an arraylist of the runtime chains that include the channel
     */
    public synchronized List<ChainData> getRunningChains(ChannelDataImpl parent) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getRunningChains");
        }
        ChannelContainer channelContainer = null;
        List<ChainData> chainDataList = new ArrayList<ChainData>();

        // Iterate the children - they are in the runtime.
        Iterator<ChildChannelDataImpl> children = parent.children();
        while (children.hasNext()) {
            // Get the runtime channel container for this child.
            channelContainer = this.channelRunningMap.get(children.next().getName());
            for (Chain chain : channelContainer.getChainMap().values()) {
                chainDataList.add(chain.getChainData());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getRunningChains: " + chainDataList.size());
        }
        return chainDataList;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#isChainRunning(java.lang.String)
     */
    @Override
    public synchronized boolean isChainRunning(String chainName) {
        boolean rc = false;
        if (null != chainName) {
            Chain c = this.chainRunningMap.get(chainName);
            if (null != c) {
                rc = c.getState().equals(RuntimeState.STARTED) || c.getState().equals(RuntimeState.QUIESCED);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "isChainRunning: " + chainName + " " + rc);
        }
        return rc;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#isChainRunning(com.ibm.websphere
     * .channelfw.ChainData)
     */
    @Override
    public synchronized boolean isChainRunning(ChainData chain) {
        if (null == chain) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "isChainRunning(null): false");
            }
            return false;
        }
        return isChainRunning(chain.getName());
    }

    /**
     * Fetch the input chain from the runtime.
     *
     * @param chainName
     * @return chain requested, or null if not found.
     */
    public synchronized Chain getRunningChain(String chainName) {
        Chain chain = null;
        if (null != chainName) {
            chain = this.chainRunningMap.get(chainName);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRunningChain: " + chainName + " found=" + (null != chain));
        }
        return chain;
    }

    /**
     * Fetch the input channel from the runtime.
     *
     * @param inputChannelName
     *                             of the (parent) channel requested.
     * @param chain
     *                             in which channel is running.
     * @return Channel requested, or null if not found.
     */
    public synchronized Channel getRunningChannel(String inputChannelName, Chain chain) {
        if (inputChannelName == null || chain == null) {
            return null;
        }

        Channel channel = null;
        // Ensure the chain is running.
        if (null != this.chainRunningMap.get(chain.getName())) {
            ChannelData[] channels = chain.getChannelsData();
            for (int index = 0; index < channels.length; index++) {
                if (channels[index].getExternalName().equals(inputChannelName)) {
                    channel = chain.getChannels()[index];
                    break;
                }
            }
        }
        return channel;
    }

    /**
     * Return the state of the input runtime channel.
     *
     * @param channelName
     * @param chain
     *                        that includes this channel
     * @return state of channel, or -1 if the channel cannot be found.
     */
    public synchronized RuntimeState getChannelState(String channelName, Chain chain) {
        RuntimeState state = null;
        Channel channel = getRunningChannel(channelName, chain);
        if (channel != null) {
            ChannelContainer channelContainer = this.channelRunningMap.get(channel.getName());
            if (null != channelContainer) {
                state = channelContainer.getState();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getChannelState: " + channelName + "=" + state);
        }
        return state;
    }

    /**
     * Set the channel state of a given channel.
     *
     * @param channelName
     * @param state
     */
    private synchronized void setChannelState(String channelName, RuntimeState state) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setChannelState channelName=" + channelName + ", state=" + state.ordinal);
        }
        if (null != channelName) {
            ChannelContainer channelContainer = this.channelRunningMap.get(channelName);
            if (null != channelContainer) {
                channelContainer.setState(state);
            }
        }
    }

    /**
     * Determine if a channel references a chain in the runtime config.
     *
     * @param channelName
     * @param chainName
     * @return boolean true if channel is referenced by chain, false otherwise
     */
    public synchronized boolean doesChannelReferenceChain(String channelName, String chainName) {
        boolean foundRef = false;
        Chain chain = this.chainRunningMap.get(chainName);
        if (chain != null) {
            ChannelData channelsData[] = chain.getChannelsData();
            ChildChannelDataImpl childChannelData = null;
            for (int i = 0; i < channelsData.length; i++) {
                childChannelData = (ChildChannelDataImpl) channelsData[i];
                if (childChannelData.getExternalName().equals(channelName)) {
                    foundRef = true;
                    break;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "doesChannelReferenceChain: channel=" + channelName + ", chain=" + chainName + ", rc=" + foundRef);
        }
        return foundRef;
    }

    /**
     * Get the number of chains that are currently using this channel in the
     * runtime
     * which are in the STARTED state.
     *
     * @param channelName
     * @return number of chains in STARTED state
     */
    public synchronized int getNumStartedChainsUsingChannel(String channelName) {
        int numStartedChains = 0;
        ChannelContainer channelContainer = this.channelRunningMap.get(channelName);
        if (null != channelContainer) {
            for (Chain chain : channelContainer.getChainMap().values()) {
                if (chain.getState() == RuntimeState.STARTED) {
                    numStartedChains++;
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getNumStartedChainsUsingChannel: " + channelName + "=" + numStartedChains);
        }
        return numStartedChains;
    }

    /**
     * @return number of runtime channels in the framework.
     */
    public synchronized int getNumRunningChannels() {
        return this.channelRunningMap.size();
    }

    /**
     * @return number of channel data objects in the framework.
     */
    public synchronized int getNumChannels() {
        return this.channelDataMap.size();
    }

    /**
     * @return number of channel factories in the framework.
     */
    public synchronized int getNumChannelFactories() {
        return this.channelFactories.size();
    }

    /**
     * @return number of runtime chains in the framework.
     */
    public synchronized int getNumRunningChains() {
        return this.chainRunningMap.size();
    }

    /**
     * @return number of chain configs in the framework.
     */
    public synchronized int getNumChains() {
        return this.chainDataMap.size();
    }

    /**
     * @return number of chain groups in the framework.
     */
    public synchronized int getNumChainGroups() {
        return this.chainGroups.size();
    }

    /**
     * @param chainName
     * @return number of groups that hold a reference to the named chain.
     */
    public synchronized int getNumGroupsUsingChain(String chainName) {
        int numGroups = 0;
        for (ChainGroupData chaingroup : this.chainGroups.values()) {
            if (chaingroup.containsChain(chainName)) {
                numGroups++;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getNumGroupsUsingChain: " + chainName + "=" + numGroups);
        }
        return numGroups;
    }

    /**
     * @return number of outbound virtual connection factories in the framework.
     */
    public synchronized int getNumOutboundVCFs() {
        return this.outboundVCFactories.size();
    }

    /**
     * Query the amount of delay before warning the user about missing
     * configuration items.
     *
     * @return long
     */
    public long getMissingConfigDelay() {
        return this.missingConfigWarning;
    }

    /**
     * Return the maximum number of attempts that will be made to
     * restart a chain when it fails to start.
     *
     * @return long
     */
    public long getChainStartRetryAttempts() {
        return this.chainStartRetryAttempts;
    }

    /**
     * Return the length of time which will be waiting between restarts
     * of a chain after it fails to start.
     *
     * @return long
     */
    public long getChainStartRetryInterval() {
        return this.chainStartRetryInterval;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getDefaultChainQuiesceTimeout()
     */
    @Override
    public long getDefaultChainQuiesceTimeout() {
        return this.chainQuiesceTimeout;
    }

    /**
     * Create a new ChannelData Object.
     *
     * @param name
     * @param factoryClass
     * @param properties
     * @param weight
     * @return ChannelData
     */
    protected ChannelData createChannelData(String name, Class<?> factoryClass, Map<Object, Object> properties, int weight) {

        return new ChannelDataImpl(name, factoryClass, properties, weight, this);
    }

    /**
     * Create a new ChainData object.
     *
     * @param name
     * @param type
     * @param channels
     * @return ChainData
     * @throws IncoherentChainException
     */
    protected ChainData createChainData(String name, FlowType type, ChannelData[] channels, Map<Object, Object> properties) throws IncoherentChainException {

        return new ChainDataImpl(name, type, channels, properties);
    }

    /**
     * Create a new ChainGroupData object.
     *
     * @param name
     * @param chains
     * @return ChainGroupData
     */
    protected ChainGroupData createChainGroupData(String name, ChainData[] chains) {

        return new ChainGroupDataImpl(name, chains, this);
    }

    /**
     * Create a new VirtualConnectionFactory object.
     *
     * @param chainData
     * @return OutboundVirtualConnectionFactoryImpl
     * @throws ChannelException
     * @throws ChainException
     */
    protected OutboundVirtualConnectionFactoryImpl createVirtualConnectionFactory(ChainData chainData) throws ChannelException, ChainException {
        return new OutboundVirtualConnectionFactoryImpl(chainData, this);
    }

    /*
     * @see Object#toString()
     */
    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**    Outbound Conn Factories   **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<OutboundVirtualConnectionFactoryImpl> outFactoryIterator = outboundVCFactories.values().iterator();
        while (outFactoryIterator.hasNext()) {
            sb.append(outFactoryIterator.next()).append(nl);
        }
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**       Channel Factories      **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<ChannelFactoryDataImpl> channelFactoryIterator = channelFactories.values().iterator();
        while (channelFactoryIterator.hasNext()) {
            sb.append(channelFactoryIterator.next()).append("\r\n");
        }
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**   Channel Configurations     **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<ChannelData> channelDataIterator = channelDataMap.values().iterator();
        while (channelDataIterator.hasNext()) {
            sb.append(channelDataIterator.next().toString());
        }
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**    Chain Configurations      **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<ChainDataImpl> chainDataIterator = chainDataMap.values().iterator();
        while (chainDataIterator.hasNext()) {
            sb.append(chainDataIterator.next().toString());
        }
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**          Chain Groups        **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<ChainGroupData> values = chainGroups.values().iterator();
        while (values.hasNext()) {
            ChainGroupData groupData = values.next();
            sb.append("Group: ").append(groupData.getName()).append("\r\n");
            ChainData[] chainDataArray = groupData.getChains();
            for (int j = 0; j < chainDataArray.length; j++) {
                sb.append("\tchain: ").append(chainDataArray[j]).append("\r\n");
            }
        }
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**      Runtime Channels        **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<String> runtimeChannels = channelRunningMap.keySet().iterator();
        while (runtimeChannels.hasNext()) {
            ChannelContainer channelContainer = channelRunningMap.get(runtimeChannels.next());
            sb.append("Channel: ").append(channelContainer.getChannel().getName()).append("\r\n");
            sb.append("\tState: ").append(channelContainer.getState().ordinal).append("\r\n");
            sb.append("\tReferenced Chains:\r\n");
            Iterator<String> chainNames = channelContainer.getChainMap().keySet().iterator();
            while (chainNames.hasNext()) {
                sb.append("\t\t").append(chainNames.next()).append("\r\n");
            }
        }
        sb.append(nl);
        sb.append("**********************************").append(nl);
        sb.append("**       Runtime Chains         **").append(nl);
        sb.append("**********************************").append(nl);
        Iterator<Chain> runtimeChains = chainRunningMap.values().iterator();
        while (runtimeChains.hasNext()) {
            Chain chain = runtimeChains.next();
            sb.append(chain).append("\r\n");
            ChannelData channelsData[] = chain.getChannelsData();
            sb.append("\tReferenced Channels:\r\n");
            for (int i = 0; i < channelsData.length; i++) {
                sb.append("\t\t").append(channelsData[i].getName()).append("\r\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { this.toString() };
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#registerService(java.lang.Class,
     * java.lang.Object)
     */
    @Override
    public void registerService(Class<?> clazz, Object service) {
        this.services.put(clazz, service);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#deregisterService(java.lang.Class)
     */
    @Override
    public Object deregisterService(Class<?> clazz) {
        return this.services.remove(clazz);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#lookupService(java.lang.Class)
     */
    @Override
    public Object lookupService(Class<?> clazz) {
        return this.services.get(clazz);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#deregisterFactory(java.lang.String
     * )
     */
    @Override
    public void deregisterFactory(String name) {
        final boolean bTrace = TraceComponent.isAnyTracingEnabled();
        if (bTrace && tc.isDebugEnabled()) {
            Tr.debug(tc, "Removing factory registration: " + name);
        }
        Class<?> factory = null;
        synchronized (this.factories) {
            factory = this.factories.remove(name);
        }

        if (null != factory) {
            final String factoryName = factory.getName();
            final List<String> chains = new ArrayList<String>();

            synchronized (this) {
                for (ChainData chainData : this.chainDataMap.values()) {
                    // Look at each channel associated with this chain.
                    for (ChannelData channel : chainData.getChannelList()) {
                        if (channel.getFactoryType().getName().equals(factoryName)) {
                            chains.add(chainData.getName());
                            break; // out of channel loop
                        }
                    }
                }
            }

            // create a post-action for cleaning up the chains whose factory has been removed.
            Runnable cleanup = new Runnable() {
                @Override
                public void run() {
                    for (String chainName : chains) {
                        cleanupChain(chainName);
                    }
                }
            };

            // Stop the chain.. the cleanup will happen once the quiesce is complete.
            ChannelUtils.stopChains(chains, -1L, cleanup);
        }
    }

    protected synchronized void cleanupChain(String chainName) {
        List<ChannelData> channels = new LinkedList<ChannelData>();
        ChainData cd = chainDataMap.get(chainName);
        if (null == cd) {
            // some other thread got to this chain first...
            return;
        }
        try {
            if (FlowType.OUTBOUND.equals(cd.getType())) {
                // TODO what?
                // this.outboundVCFactories.get(cd.getName());
            } else {
                for (ChannelData channel : cd.getChannelList()) {
                    if (!channels.contains(channel)) {
                        channels.add(channel);
                    }
                }
                if (cd.isEnabled()) {
                    destroyChain(cd);
                }
                removeChain(cd);
            }
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error removing chain based on factory; " + cd.getName() + " " + t);
            }
        }

        for (ChannelData channel : channels) {
            channelDataMap.remove(channel.getName());
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#lookupFactory(java.lang.String)
     */
    @Override
    public Class<? extends ChannelFactory> lookupFactory(String name) {
        Class<? extends ChannelFactory> clazz = null;
        synchronized (this.factories) {
            clazz = this.factories.get(name);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "lookupFactory: " + name + ": " + clazz);
        }
        if (null != clazz) {
            // if we found a registration and a provider, let it know
            ChannelFactoryProvider provider = this.providers.get(name);
            if (null != provider && !activatedProviders.contains(provider)) {
                this.activatedProviders.add(provider);
                provider.init();
            }
        }
        return clazz;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#registerFactory(java.lang.String,
     * java.lang.Class)
     */
    @Override
    public void registerFactory(String name, Class<? extends ChannelFactory> factory) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "registerFactory: " + name + "; " + factory);
        }
        synchronized (this.factories) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Class<? extends ChannelFactory> prevFactory = this.factories.get(name);
                if (null != prevFactory && factory != prevFactory) {
                    Tr.event(tc, "WARNING: overlaying existing factory: " + prevFactory);
                }
            }
            this.factories.put(name, factory);
        } // end-sync

        // now that we have a new factory type, tell ChannelUtils to process
        // any delayed config that might be waiting for it
        ChannelUtils.loadConfigDelay();

    }

    /**
     * Set a factory provider.
     *
     * @param provider
     */
    @Override
    public void registerFactories(ChannelFactoryProvider provider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Register factory provider; " + provider.getClass().getName());
        }
        synchronized (this.factories) {
            for (Entry<String, Class<? extends ChannelFactory>> entry : provider.getTypes().entrySet()) {
                this.providers.put(entry.getKey(), provider);
                Class<? extends ChannelFactory> newFactory = entry.getValue();
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Class<? extends ChannelFactory> prevFactory = this.factories.get(entry.getKey());
                    if (null != prevFactory && newFactory != prevFactory) {
                        Tr.event(tc, "WARNING: overlaying existing factory: " + prevFactory);
                    }
                }
                this.factories.put(entry.getKey(), newFactory);
            }
        } // end-sync

        // now that we have a new factory type, tell ChannelUtils to process
        // any delayed config that might be waiting for it
        ChannelUtils.loadConfigDelay();
    }

    /**
     * Remove a factory provider.
     *
     * @param provider
     */
    @Override
    public void deregisterFactories(ChannelFactoryProvider provider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Removing factory provider; " + provider.getClass().getName());
        }
        for (Entry<String, Class<? extends ChannelFactory>> entry : provider.getTypes().entrySet()) {
            this.providers.remove(entry.getKey());
            deregisterFactory(entry.getKey());
        }
        this.activatedProviders.remove(provider);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#determineBestEndPoint(com.ibm.
     * websphere.channelfw.CFEndPoint[],
     * com.ibm.websphere.channelfw.CFEndPointCriteria)
     */
    @Override
    public CFEndPoint determineBestEndPoint(CFEndPoint[] endPointList, CFEndPointCriteria criteria) {
        if (null == endPointList) {
            return null;
        }
        CFEndPoint chosenEndPoint = null;
        CFEndPoint[] matchedEps = commonGetEndPoints(endPointList, criteria, true);
        if (null != matchedEps) {
            chosenEndPoint = matchedEps[matchedEps.length - 1];
        }
        return chosenEndPoint;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFramework#getEndPoint(java.lang.String)
     */
    @Override
    public CFEndPoint getEndPoint(String chainName) throws ChannelFrameworkException {
        CFEndPoint rc = null;
        if (null != chainName) {
            synchronized (this) {
                ChainDataImpl chain = this.chainDataMap.get(chainName);
                if (null != chain) {
                    rc = chain.getEndPoint();
                }
            }
        }
        return rc;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFramework#getEndPoints(com.ibm.websphere
     * .channelfw.CFEndPoint[], com.ibm.websphere.channelfw.CFEndPointCriteria)
     */
    @Override
    public CFEndPoint[] getEndPoints(CFEndPoint[] endPointList, CFEndPointCriteria criteria) {
        return commonGetEndPoints(endPointList, criteria, false);
    }

    /**
     * Use the criteria to narrow down the provided list of endpoints to
     * a proper subset.
     *
     * @param endPointList
     * @param criteria
     * @param getBestOnly
     * @return CFEndPoint[]
     */
    private CFEndPoint[] commonGetEndPoints(CFEndPoint[] endPointList, CFEndPointCriteria criteria, boolean getBestOnly) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "commonGetEndPoints");
        }
        if (null == endPointList || 0 == endPointList.length || null == criteria) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "commonGetEndPoints", null);
            }
            return null;
        }
        String chainName = criteria.getChainName();
        List<CFEndPoint> chosenEndPoints = new LinkedList<CFEndPoint>();
        if (null != chainName) {
            for (int i = 0; i < endPointList.length; i++) {
                if (null == endPointList[i]) {
                    continue;
                }
                if (chainName.equals(endPointList[i].getName())) {
                    chosenEndPoints.add(endPointList[i]);
                    if (getBestOnly) {
                        break;
                    }
                }
            }
        } else {
            // Chain name was not provided in criteria.
            // Take special steps if the vhost is specified in the criteria.
            String vhost = criteria.getVirtualHost();
            if (null != vhost) {
                // If a match is found with an endpoint, then chosenEndPoints should
                // only include
                // a list of endPoints with matching vhost. If no matches are found,
                // then filter
                // out all endPoints that have the unmatching vhost ... leaving all with
                // null vhosts.
                //
                // Special Note:
                // The case of not finding a match should only happen if no vhost was
                // found to match
                // the inbound chain's host and port that lead to the creation of the
                // CFEndPoint. This
                // could result from a customer changing the port number on the server,
                // but not
                // putting the port into an appropriate virtual host. If precise
                // selection of a
                // CFEndPoint is needed, then that extra step is required by the
                // customer.
                List<CFEndPoint> vhostMatchingEndPoints = new ArrayList<CFEndPoint>();
                List<CFEndPoint> vhostNullEndPoints = new ArrayList<CFEndPoint>();
                for (CFEndPoint tempEndPoint : endPointList) {
                    if (tempEndPoint == null) {
                        continue;
                    }
                    List<String> vhostList = tempEndPoint.getVirtualHosts();
                    if (vhostList.isEmpty()) {
                        vhostNullEndPoints.add(tempEndPoint);
                    } else {
                        for (String value : vhostList) {
                            if (vhost.equalsIgnoreCase(value)) {
                                // Found a match. Add it to the matching array list.
                                vhostMatchingEndPoints.add(tempEndPoint);
                                break;
                            }
                        }
                    }
                }
                // Check to see if any matches were found for the vhost.
                if (0 != vhostMatchingEndPoints.size()) {
                    // A match was found. The endPointList should only include these
                    // endPoints.
                    // Update the array to the correct length. Then fill it with the new
                    // contents.
                    endPointList = new CFEndPoint[vhostMatchingEndPoints.size()];
                    vhostMatchingEndPoints.toArray(endPointList);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found matching vhost in " + vhostMatchingEndPoints.size() + " CFEndPoints.");
                    }
                } else {
                    // No match was found. The endPointList should only include endPoints
                    // with no vhost.
                    // Update the array to the correct length. Then fill it with the new
                    // contents.
                    endPointList = new CFEndPoint[vhostNullEndPoints.size()];
                    vhostNullEndPoints.toArray(endPointList);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "No matching vhost found.  New CFEndPoint list size " + vhostNullEndPoints.size());
                    }
                }
            }

            // Decision may be based on isSSLRequired and isLocal
            boolean sslRequired = criteria.isSSLRequired();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Criteria specification if SSL is required: " + sslRequired);
            }

            // Decision may be based on a list of required channel factories.
            Class<?>[] requiredFactories = criteria.getOptionalChannelFactories();

            // Iterate the endPoints and compare them to the criteria.
            for (CFEndPoint tempEndPoint : endPointList) {
                if (tempEndPoint == null) {
                    continue;
                }
                // Double check that the channel accessor is not null.
                if (tempEndPoint.getChannelAccessor() == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Skipping over end point with null channel accessor, " + tempEndPoint.getName());
                    }
                    continue;
                }

                // Check for inclusion of the required factories.
                if (requiredFactories != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found required factories to match");
                    }
                    boolean foundFactory = true;
                    Class<?> ocdFactory = null;
                    List<OutboundChannelDefinition> ocdList = tempEndPoint.getOutboundChannelDefs();
                    // Iterate the list of required factories to ensure each is in the end
                    // point.
                    for (Class<?> requiredFactory : requiredFactories) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "RequiredFactory: " + requiredFactory);
                        }
                        // Note that we haven't found a match yet.
                        foundFactory = false;
                        // Iterate the list of outbound channel definitions in search for
                        // the required factory class.
                        for (OutboundChannelDefinition def : ocdList) {
                            ocdFactory = def.getOutboundFactory();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "OCD Factory: " + ocdFactory);
                            }
                            // Try to match the required factory class to this outbound
                            // channel definition.
                            if (requiredFactory.isAssignableFrom(ocdFactory) || ocdFactory.isAssignableFrom(requiredFactory)) {
                                // Found a match. This endpoint is good so far.
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Found a match");
                                }
                                foundFactory = true;
                                break;
                            }
                        }
                        // Verify that the required factory was in the endpoint.
                        if (!foundFactory) {
                            // Did NOT find a match. This endpoint is no good. Break out of
                            // this loop.
                            break;
                        }
                    }
                    if (!foundFactory) {
                        // Did NOT find a match for a required factory. This endpoint is no
                        // good. Move on to next endpoint.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Skipping over endpoint with missing required factory.");
                        }
                        continue;
                    }
                }

                // Check that the channel accessor matches the endPoint.
                if (tempEndPoint.getChannelAccessor().equals(criteria.getChannelAccessor())) {
                    // Found matching channel accessor. Check if SSL is required in the
                    // criteria
                    if (sslRequired) {
                        // The criteria requires an SSL enabled endpoint.
                        if (tempEndPoint.isSSLEnabled()) {
                            // Check to see if an endpoint was already found.
                            if (chosenEndPoints.size() == 0) {
                                // No endpoint found yet. Found a matching endpoint.
                                chosenEndPoints.add(tempEndPoint);
                                // Searching can stop if the endpoint is local
                                if (tempEndPoint.isLocal()) {
                                    // The matching endpoint is local and getBestOnly. Stop
                                    // search.
                                    if (getBestOnly) {
                                        break;
                                    }
                                }
                            } else {
                                if (getBestOnly) {
                                    // Endpoint was already found. We are searching for a better
                                    // local one.
                                    if (tempEndPoint.isLocal()) {
                                        // This endpoint is local and the former endPoint wasn't and
                                        // getBestOnly. Stop search.
                                        chosenEndPoints.add(tempEndPoint);
                                        break;
                                    }
                                } else {
                                    chosenEndPoints.add(tempEndPoint);
                                }
                            }
                        }
                    } else {
                        // The criteria requires a non SSL enabled endpoint.
                        if (!tempEndPoint.isSSLEnabled()) {
                            // Found a matching non SSL enabled endpoint.
                            chosenEndPoints.add(tempEndPoint);
                            // Still need to get a local one if available.
                            if (tempEndPoint.isLocal()) {
                                // Found a matching local endpoint. Stop search.
                                if (getBestOnly) {
                                    break;
                                }
                            }
                        }
                    } // end check sslRequired
                } // end check channelAccessor
            } // end loop through end points
        } // end check chainName

        // convert the list to array or null if no match is found.
        int rtnSize = chosenEndPoints.size();
        CFEndPoint rtnEPs[] = null;
        if (rtnSize > 0) {
            rtnEPs = new CFEndPoint[rtnSize];
            chosenEndPoints.toArray(rtnEPs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "commonGetEndPoints " + rtnSize);
        }
        return rtnEPs;
    }

    /**
     * Find the appropriate virtual host list for the provided
     * address and port target.
     *
     * @param address
     * @param port
     * @return List<String>, never null but might be empty
     */
    public List<String> getVhost(String address, String port) {
        if (null == address || null == port) {
            return null;
        }
        int portnum = Integer.parseInt(port);
        List<EndPointInfo> eps = EndPointMgrImpl.getRef().getEndPoints(address, portnum);
        List<String> rc = new ArrayList<String>(eps.size());
        for (EndPointInfo ep : eps) {
            rc.add(ep.getName());
        }
        return rc;
    }

    /**
     * Create a new outbound chain based on the provided endpoint definition.
     *
     * @param endpoint
     * @return ChainData
     * @throws ChannelFrameworkException
     */
    public ChainData createOutboundChain(CFEndPoint endpoint) throws ChannelFrameworkException {
        List<OutboundChannelDefinition> defs = endpoint.getOutboundChannelDefs();
        String namelist[] = new String[defs.size()];
        int i = 0;
        for (OutboundChannelDefinition def : defs) {
            namelist[i] = "channel_" + channelNameCounter.getAndIncrement();
            addChannel(namelist[i], def.getOutboundFactory(), def.getOutboundChannelProperties());
            i++;
        }
        return addChain("chain_" + chainNameCounter.getAndIncrement(), FlowType.OUTBOUND, namelist);
    }

    /**
     * This method is called to fetch a virtual connection factory which
     * represents an outbound chain.
     *
     * @param channelDefs
     * @return VirtualConnectionFactory
     * @throws ChannelFrameworkException
     */
    private synchronized VirtualConnectionFactory getOutboundVCFactory(List<OutboundChannelDefinition> channelDefs) throws ChannelFrameworkException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getOutboundVCFactory");

        ChainDataImpl chainData = null;
        boolean found = false;
        int hashKey = 0;

        // Build a hash key based on names of channel factories.
        for (OutboundChannelDefinition def : channelDefs) {
            hashKey += def.getOutboundFactory().hashCode();
        }

        // See if the key already exists in the current set of outbound chains.
        OutboundVirtualConnectionFactoryImpl vcf = null;
        Iterator<OutboundVirtualConnectionFactoryImpl> vcfs = this.outboundVCFactories.values().iterator();
        while (vcfs.hasNext()) {
            vcf = vcfs.next();
            chainData = (ChainDataImpl) vcf.getChain().getChainData();
            if (hashKey == chainData.getChannelFactoryHash()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found matching hash keys");
                }
                // Handle tiny case where two chains hash to same value.
                // Verify the channel factories, factory props, and channel
                // props are the same.
                ChannelData channelDataList[] = chainData.getChannelList();
                if (channelDataList.length == channelDefs.size()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found matching number of channels");
                    }
                    // Assume a match.
                    found = true;
                    ChannelData existingDef;
                    ChannelFactoryData existingFactoryData;
                    OutboundChannelDefinition inputDef;
                    // Loop through all the channels in the chain data.
                    for (int i = 0; i < channelDataList.length; i++) {
                        // Extract the current/existing channel def.
                        existingDef = channelDataList[i];
                        existingFactoryData = getChannelFactory(existingDef.getFactoryType());
                        // Extract the new channel def.
                        inputDef = channelDefs.get(i);
                        // Compare the existing and new channel defs.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Comparing existing def " + existingDef.getFactoryType() + "to new def " + inputDef.getOutboundFactory());
                        }
                        boolean sameFactoryType = (existingDef.getFactoryType() == inputDef.getOutboundFactory());
                        boolean sameChannelProps = propertiesIncluded(inputDef.getOutboundChannelProperties(), existingDef.getPropertyBag());
                        boolean sameFactoryProps = propertiesIncluded(inputDef.getOutboundFactoryProperties(), existingFactoryData.getProperties());

                        if (!sameFactoryType || !sameChannelProps || !sameFactoryProps) {
                            // Found a mismatch.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Keys matched, but factories didn't");
                            }
                            found = false;
                            // Break out of this inner loop.
                            break;
                        }
                    }
                    if (found) {
                        // Break out of outer loop.
                        break;
                    }
                }
            }
        } // factory loop

        if (!found) {
            // Couldn't find a match. Need to create a one.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No existing VCF, create one.");
            }
            // We may be able to reuse existing chain or channel data objects.
            // However, the hit in searching probably isn't worth it.
            // For now, create them all from scratch.
            String channelNameList[] = new String[channelDefs.size()];
            String channelName = null;
            int i = 0;
            for (OutboundChannelDefinition def : channelDefs) {
                channelName = "channel_" + channelNameCounter.getAndIncrement();
                channelNameList[i++] = channelName;
                addChannel(channelName, def.getOutboundFactory(), def.getOutboundChannelProperties());
            }
            String chainName = "chain_" + chainNameCounter.getAndIncrement();
            addChain(chainName, FlowType.OUTBOUND, channelNameList);
            vcf = (OutboundVirtualConnectionFactoryImpl) getOutboundVCFactory(chainName);
        } else if (null != vcf) {
            vcf.incrementRefCount();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found an existing vcf " + vcf.hashCode());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getOutboundVCFactory");
        }
        return vcf;
    }

    /**
     * This method returns whether all the properties in the first Map are in
     * the second Map.
     *
     * @param inputMap
     *                        of properties to search for.
     * @param existingMap
     *                        of properties to search in.
     * @return true if all properties of first Map are found in second Map
     */
    private boolean propertiesIncluded(Map<Object, Object> inputMap, Map<Object, Object> existingMap) {
        if (inputMap == null) {
            // If no properties are specified, then success.
            return true;
        } else if (existingMap == null || inputMap.size() > existingMap.size()) {
            // If properties are specified, but none exist in the current map,
            // then fail.
            return false;
        }

        // Loop through input list and search in the search list.
        Object existingValue;
        for (Entry<Object, Object> entry : inputMap.entrySet()) {
            existingValue = existingMap.get(entry.getKey());
            if (existingValue == null || !existingValue.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prepare the factory and the chain for the provided endpoint.
     *
     * @param endpoint
     * @throws ChannelFrameworkException
     */
    public void prepareEndPoint(CFEndPointImpl endpoint) throws ChannelFrameworkException {
        if (null == endpoint.getOutboundChainData()) {
            VirtualConnectionFactory vcf = getOutboundVCFactory(endpoint.getOutboundChannelDefs());
            endpoint.setOutboundVCFactory(vcf);
            endpoint.setOutboundChainData(getChain(vcf.getName()));
        }
    }

    /**
     * Save AsyncIOHelper reference for use by Async IO (AsyncLibrary).
     *
     * @param AsyncIOHelper
     * @throws
     */
    public void setAsyncIOHelper(AsyncIOHelper asyncIOHelper) {
        this.asyncIOHelper = asyncIOHelper;
    }

    /**
     * Retrieve AsyncIOHelper reference for use by Async IO (AsyncLibrary).
     *
     * @return AsyncIOHelper
     * @throws
     */
    public AsyncIOHelper getAsyncIOHelper() {
        return asyncIOHelper;
    }

    /**
     * Check if AsyncIO is enabled
     *
     * @return
     */
    public boolean getAsyncIOEnabled() {
        AsyncIOHelper asyncIOHelper = this.asyncIOHelper;
        return asyncIOHelper != null && asyncIOHelper.enableAsyncIO();
    }
}
