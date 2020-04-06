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
package com.ibm.ws.channelfw.internal.chains;

import java.util.Map;

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.ChildChannelDataImpl;
import com.ibm.ws.channelfw.internal.RuntimeState;
import com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup;
import com.ibm.ws.channelfw.internal.discrim.DiscriminationProcessImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.exception.ChainException;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;
import com.ibm.wsspi.channelfw.exception.IncoherentChainException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelNameException;

/**
 * Inbound chain instance.
 */
public class InboundChain extends Chain {

    /** Trace service */
    private static final TraceComponent tc = Tr.register(InboundChain.class,
                                                         ChannelFrameworkConstants.BASE_TRACE_NAME,
                                                         ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Creates an inbound chain from a chain config object.
     *
     * @param config
     * @param framework
     * @throws ChannelException
     * @throws IncoherentChainException
     */
    public InboundChain(ChainData config, ChannelFrameworkImpl framework) throws ChannelException, IncoherentChainException {
        super(config);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor");
        }
        ChannelFactory channelFactory = null;
        channels = new InboundChannel[channelDataArray.length];
        Channel prevChannel = null;
        // Fill in the array.
        for (int i = 0; i < channelDataArray.length; ++i) {
            // Get the factory used to create each channel in the chain.
            channelFactory = framework.getChannelFactoryInternal(channelDataArray[i].getFactoryType(), true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "factory: " + channelFactory);
                Tr.debug(tc, "findOrCreateChannel: " + channelDataArray[i].getName());
            }
            // Assign the appropriate device interface class names to the channel data objects.
            if (i != 0) {
                // Only do this for non device side channels.
                ((ChildChannelDataImpl) channelDataArray[i]).setDeviceInterface(
                                                                                channels[i - 1].getApplicationInterface());
            }
            // Inform the child channel data that it is being used inbound.
            ((ChildChannelDataImpl) channelDataArray[i]).setIsInbound(true);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "putting into PropertyBag chain name of: " + chainData.getName());
            }
            Map propertyBag = channelDataArray[i].getPropertyBag();
            propertyBag.put(ChannelFrameworkConstants.CHAIN_NAME_KEY, chainData.getName());

            // Get an instance of each channel.
            channels[i] = channelFactory.findOrCreateChannel(channelDataArray[i]);
            if (null == channels[i]) {
                InvalidChannelNameException e = new InvalidChannelNameException("Chain cannot be created because of channel, " + channelDataArray[i].getName());
                FFDCFilter.processException(e, getClass().getName() + ".constructor",
                                            "89", this, new Object[] { channelDataArray[i] });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "constructor");
                }
                throw e;
            } else if (null != prevChannel) {
                // Chain coherency checking.
                Class<?> prevChannelDiscType = ((InboundChannel) prevChannel).getDiscriminatoryType();
                Class<?> nextChannelDiscDataType = ((InboundChannel) channels[i]).getDiscriminator().getDiscriminatoryDataType();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "prev.AppSideClass: " + prevChannel.getApplicationInterface());
                    Tr.debug(tc, "this.DevSideClass: " + channels[i].getDeviceInterface());
                    Tr.debug(tc, "prev.DiscType: " + prevChannelDiscType);
                    Tr.debug(tc, "this.DiscType: " + nextChannelDiscDataType);
                }
                if (prevChannelDiscType != nextChannelDiscDataType) {
                    IncoherentChainException e = new IncoherentChainException("Unmatching channel disc types: " + prevChannel.getName()
                                                                              + ", " + prevChannelDiscType
                                                                              + " " + channels[i].getName()
                                                                              + ", " + nextChannelDiscDataType);
                    FFDCFilter.processException(e, getClass().getName() + ".constructor",
                                                "122", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "constructor");
                    }
                    throw e;
                }
                if (prevChannel.getApplicationInterface() != channels[i].getDeviceInterface()) {
                    IncoherentChainException e = new IncoherentChainException("Unmatching channel interfaces: "
                                                                              + prevChannel.getName() + ", " + channels[i].getName());
                    FFDCFilter.processException(e, getClass().getName() + ".constructor",
                                                "136", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "constructor");
                    }
                    throw e;
                }
            }
            // Don't build a discrimination process for the last channel in the inbound chain.
            if (channelDataArray.length != i + 1) {
                DiscriminationProcess dp = ((InboundChannel) channels[i]).getDiscriminationProcess();
                if (null == dp) {
                    // put a DiscriminationProcess in the channel
                    Class<?> discriminatoryType = ((InboundChannel) channels[i]).getDiscriminatoryType();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "discriminatoryType: " + discriminatoryType);
                    }
                    dp = new DiscriminationProcessImpl(discriminatoryType, channelDataArray[i].getName());
                    ((InboundChannel) channels[i]).setDiscriminationProcess(dp);
                }
            }
            // Save this channel for use in the next iteration of this loop.
            prevChannel = channels[i];
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "constructor");
        }
    }

    /**
     * This method is called from the channel framework right before the start method is called.
     * In between, it starts up the channels in the chain.
     */
    public void setupDiscProcess() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "setupDiscProcess");
        }
        ChannelData list[] = chainData.getChannelList();
        Class<?> discriminatoryType = null;
        DiscriminationProcessImpl dp = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Get the channels started");
        }
        // Set up discrimination process for framework and start the channels.
        for (int i = 0; i < list.length; i++) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Set up disc process for channel, " + list[i].getName());
            }
            // Don't set up a disc process for the last channel in the chain.
            if (list.length != i + 1) {
                dp = (DiscriminationProcessImpl) ((InboundChannel) channels[i]).getDiscriminationProcess();
                if (dp == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Create a discrimination process for channel, "
                                     + channels[i].getName());
                    }
                    // Create a discrimination process for the framework
                    discriminatoryType = ((InboundChannel) channels[i]).getDiscriminatoryType();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Discriminatory type: " + discriminatoryType);
                    }
                    dp = new DiscriminationProcessImpl(discriminatoryType, list[i].getName());
                    ((InboundChannel) channels[i]).setDiscriminationProcess(dp);
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found DP: " + dp);
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Last channel in chain, " + list[i].getName());
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "setupDiscProcess");
        }
    }

    /**
     * This method is called from ChannelFrameworkImpl during chain startup to start up the discrimination
     * process between each set of adjacent channels in the chain.
     *
     * @param appChannel the channel on the application side of the two provided
     * @param devChannel the channel on the device side of the two provided
     * @param discWeight
     * @throws ChainException in case an error occurs, potentially related to setting up discrimination.
     */
    public void startDiscProcessBetweenChannels(InboundChannel appChannel, InboundChannel devChannel,
                                                int discWeight) throws ChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startDiscProcessBetweenChannels");
        }
        // Get the discriminator for the app channel. Protect from misc exceptions being thrown.
        Discriminator d = null;
        Exception discException = null;
        try {
            d = appChannel.getDiscriminator();
            if (d == null) {
                discException = new Exception("Null discriminator extracted from channel " + appChannel.getName());
            }
        } catch (Exception e) {
            // No FFDC needed. Done a couple lines down.
            discException = e;
        }
        if (null != discException) {
            // Even though rethrowing exception, must FFDC now to capture stack trace.
            FFDCFilter.processException(discException,
                                        getClass().getName() + ".startDiscProcessBetweenChannels",
                                        "234", this, new Object[] { appChannel, devChannel, Integer.valueOf(discWeight) });
            throw new ChainException("Unable to get discriminator from " + appChannel.getName(), discException);
        }

        // Get the discrimination group from the former channel in the chain.
        DiscriminationGroup prevDg = (DiscriminationGroup) devChannel.getDiscriminationProcess();
        // Check to see if the disc group already includes the discriminator.
        if (!((DiscriminationProcessImpl) prevDg).containsDiscriminator(d)) {
            // Add this discriminator or create a new one?
            if (!(prevDg.getDiscriminators().isEmpty())) {
                // Add a new DiscriminationProcess to a started channel
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Create new dp for channel, " + devChannel.getName());
                }
                DiscriminationGroup newDg = new DiscriminationProcessImpl(devChannel.getDiscriminatoryType(), prevDg);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Add in discriminator from channel, " + appChannel.getName());
                }
                newDg.addDiscriminator(d, discWeight);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Set dp into channel, " + devChannel.getName());
                }
                newDg.start();
                devChannel.setDiscriminationProcess(newDg);
            } else {
                // Enable the previous channel in the chain to communication with this one.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "Add in disc from channel, "
                                 + appChannel.getName()
                                 + " into dp of channel, "
                                 + devChannel.getName());
                }
                prevDg.addDiscriminator(d, discWeight);
                prevDg.start();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found discriminator in dp, " + appChannel.getName());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startDiscProcessBetweenChannels");
        }
    }

    /**
     * Disable the input channel.
     *
     * @param inputChannel
     * @throws InvalidChannelNameException
     * @throws DiscriminationProcessException
     */
    public void disableChannel(Channel inputChannel) throws InvalidChannelNameException, DiscriminationProcessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "disableChannel: " + inputChannel.getName());
        }
        synchronized (state) {
            if (RuntimeState.STARTED.equals(state) || RuntimeState.QUIESCED.equals(state)) {
                String targetName = inputChannel.getName();
                int index = 0;
                InboundChannel prevChannel = null;
                // Find the index of the input channel in this chain.
                for (; index < channels.length; index++) {
                    if (channels[index].getName().equals(targetName)) {
                        break;
                    }
                    prevChannel = (InboundChannel) channels[index];
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Index of channel, " + index);
                }

                if (channels.length == index) {
                    // Never found the channel. Log an error.
                    InvalidChannelNameException e = new InvalidChannelNameException("ERROR: can't unlink unknown channel, " + targetName);
                    FFDCFilter.processException(e, getClass().getName() + ".disableChannel",
                                                "319", this, new Object[] { inputChannel });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "disableChannel");
                    }
                    throw e;
                } else if (null != prevChannel) {
                    // Note: do nothing if the index was zero, meaning input channel is a connector.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Previous channel, " + prevChannel.getName());
                    }
                    // Discriminator was only added after the chain was started.
                    Class<?> discriminatoryType = prevChannel.getDiscriminatoryType();
                    DiscriminationProcessImpl newDp = new DiscriminationProcessImpl(discriminatoryType, (DiscriminationGroup) prevChannel.getDiscriminationProcess());
                    newDp.removeDiscriminator(((InboundChannel) inputChannel).getDiscriminator());
                    prevChannel.setDiscriminationProcess(newDp);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "RuntimeState invalid to disable channel: "
                                 + inputChannel.getName() + ", chain state: " + state.ordinal);
                }
            }
        } // End synchronize
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "disableChannel");
        }
    }

    /**
     * This method analyzes the interfaces between the input array of
     * inbound channels. If the interfaces between them match, implying they
     * have the ability to form a chain, the method returns without exception.
     * Otherwise, an IncoherentChainException is thrown describing the channels
     * that were incoherent.
     *
     * @param chainData
     * @throws IncoherentChainException
     */
    public static void verifyChainCoherency(ChainData chainData) throws IncoherentChainException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "verifyChainCoherency");
        }
        ChannelData[] channelDataArray = chainData.getChannelList();
        // Verify there are multiple channels in this chain.
        if (channelDataArray.length > 1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found channel list greater than 1");
            }
            ChannelFrameworkImpl fw = (ChannelFrameworkImpl) ChannelFrameworkFactory.getChannelFramework();
            ChannelFactoryData current = null;
            ChannelFactoryData next = null;
            Class<?> currentAppClass = null;
            Class<?>[] nextDevClasses = null;
            try {
                current = fw.findOrCreateChannelFactoryData(channelDataArray[0].getFactoryType());
            } catch (ChannelFactoryException e) {
                // No FFDC Needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found invalid channel factory of type "
                                 + channelDataArray[0].getFactoryType().getName());
                }
                throw new IncoherentChainException("Invalid channel factory");
            }
            // Iterate the channels and verify coherency between them.
            for (int i = 1; i < channelDataArray.length; i++) {
                currentAppClass = current.getApplicationInterface();
                try {
                    next = fw.findOrCreateChannelFactoryData(channelDataArray[i].getFactoryType());
                } catch (ChannelFactoryException e) {
                    // No FFDC Needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found invalid channel factory of type "
                                     + channelDataArray[i].getFactoryType().getName());
                    }
                    throw new IncoherentChainException("Invalid channel factory");
                }
                nextDevClasses = next.getDeviceInterface();
                // Check for nulls or incompatibility.
                if ((null == currentAppClass) || (null == nextDevClasses)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found null interface classes between two channel factories: "
                                     + current.getFactory().getName() + ", "
                                     + next.getFactory().getName());
                    }
                    throw new IncoherentChainException("Found null interface classes between two channel factories: "
                                                       + current.getFactory().getName() + ", "
                                                       + next.getFactory().getName());
                }
                // Handle polymorphism
                boolean foundMatch = false;
                for (int j = 0; j < nextDevClasses.length; j++) {
                    if (currentAppClass.isAssignableFrom(nextDevClasses[j])) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Found compatible class; " + currentAppClass);
                        }
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found incoherency between two channel factories: " + current + ", " + next);
                    }
                    throw new IncoherentChainException("Found incoherency between two channel factories: " + current + ", " + next);
                }
                current = next;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "verifyChainCoherency");
        }
    }

}
