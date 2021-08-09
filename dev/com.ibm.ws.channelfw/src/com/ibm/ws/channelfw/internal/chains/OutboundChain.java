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

import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.ChannelFrameworkImpl;
import com.ibm.ws.channelfw.internal.ChildChannelDataImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.OutboundChannel;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.IncoherentChainException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelNameException;

/**
 * OutboundChain implementation.
 */
public class OutboundChain extends Chain {

    // Trace service
    private static final TraceComponent tc =
                    Tr.register(OutboundChain.class,
                                ChannelFrameworkConstants.BASE_TRACE_NAME,
                                ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Creates an outbound chain from a chain config object.
     * 
     * @param config
     * @param framework
     * @throws ChannelException
     * @throws IncoherentChainException
     */
    public OutboundChain(ChainData config, ChannelFrameworkImpl framework) throws ChannelException, IncoherentChainException {
        super(config);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor");
        }
        ChannelFactory channelFactory = null;
        OutboundChannel prevChannel = null;
        // Create the array to manage the channels in this chain.
        channels = new OutboundChannel[channelDataArray.length];

        // Fill in the array, from the device side.
        for (int i = channelDataArray.length - 1; i >= 0; --i) {
            // Get the factory used to create each channel in the chain.
            channelFactory = framework.getChannelFactoryInternal(channelDataArray[i].getFactoryType(), true);
            // Get an instance of each channel.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Create channel, " + channelDataArray[i].getName());
            }
            // Assign the appropriate device interface class names to the channel data
            // objects.
            if (i != channelDataArray.length - 1) {
                // Only set the device interface for non device side channels.
                ((ChildChannelDataImpl) channelDataArray[i]).setDeviceInterface(channels[i + 1].getApplicationInterface());
            }
            // Inform the child channel data that it is being used inbound.
            ((ChildChannelDataImpl) channelDataArray[i]).setIsInbound(false);

            channels[i] = channelFactory.findOrCreateChannel(channelDataArray[i]);
            if (null == channels[i]) {
                InvalidChannelNameException e = new InvalidChannelNameException("Chain cannot be created because of channel, " + channelDataArray[i].getName());
                FFDCFilter.processException(e, getClass().getName() + ".constructor", "79", this, new Object[] { channelDataArray[i] });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "constructor");
                }
                throw e;
            } else if (null != prevChannel) {
                // Chain coherency checking.

                // Coherency checking for connect address types
                int numAppAddressTypes = prevChannel.getApplicationAddress().length;
                String prevType = ((OutboundChannel) channels[i]).getDeviceAddress().toString();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "DevAddress: " + prevType);
                    for (int j = 0; j < numAppAddressTypes; j++) {
                        Tr.debug(tc, "AppAddress[" + j + "]: " + prevChannel.getApplicationAddress()[j].toString());
                    }
                }
                boolean bMatchedAddress = false;
                String nextType = null;
                for (int j = 0; j < numAppAddressTypes; j++) {
                    nextType = prevChannel.getApplicationAddress()[j].toString();
                    if (prevType.equals(nextType)) {
                        bMatchedAddress = true;
                        break;
                    }
                }
                if (!bMatchedAddress) {
                    IncoherentChainException e = new IncoherentChainException("Unmatching addresses between channels: " + prevChannel.getName() + ", " + channels[i].getName());
                    FFDCFilter.processException(e, getClass().getName() + ".constructor", "109", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "constructor");
                    }
                    throw e;
                }

                // Coherency checking for interfaces
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "prev.AppInterface: " + prevChannel.getApplicationInterface());
                    Tr.debug(tc, "next.DevInterface: " + channels[i].getDeviceInterface());
                }
                if (prevChannel.getApplicationInterface() != channels[i].getDeviceInterface()) {
                    IncoherentChainException e = new IncoherentChainException("Unmatching channels: " + prevChannel.getName() + ", " + channels[i].getName());
                    FFDCFilter.processException(e, getClass().getName() + ".constructor", "124", this, new Object[] { prevChannel, channels[i] });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "constructor");
                    }
                    throw e;
                }
            }
            prevChannel = (OutboundChannel) channels[i];
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "constructor");
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
            ChannelFrameworkImpl fw = (ChannelFrameworkImpl) ChannelFrameworkFactory.getChannelFramework();
            ChannelFactoryData current = null;
            ChannelFactoryData next = null;
            Class<?>[] currentDevClasses = null;
            Class<?> nextAppClass = null;
            try {
                current = fw.findOrCreateChannelFactoryData(channelDataArray[0].getFactoryType());
            } catch (ChannelFactoryException e) {
                // No FFDC Needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found invalid channel factory of type " + channelDataArray[0].getFactoryType().getName());
                }
                throw new IncoherentChainException("Invalid channel factory");
            }
            // Iterate the channels and verify coherency between them.
            for (int i = 1; i < channelDataArray.length; i++) {
                currentDevClasses = current.getDeviceInterface();
                try {
                    next = fw.findOrCreateChannelFactoryData(channelDataArray[i].getFactoryType());
                } catch (ChannelFactoryException e) {
                    // No FFDC Needed
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found invalid channel factory of type " + channelDataArray[i].getFactoryType().getName());
                    }
                    throw new IncoherentChainException("Invalid channel factory");
                }
                nextAppClass = next.getApplicationInterface();
                // Check for nulls or incompatibility.
                if ((null == currentDevClasses) || (null == nextAppClass)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found null interface classes between two channel factories: " + current.getFactory().getName() + ", " + next.getFactory().getName());
                    }
                    throw new IncoherentChainException("Found null interface classes between two channel factories: " + current.getFactory().getName() + ", "
                                                       + next.getFactory().getName());
                }
                // Handle polymorphism
                boolean foundMatch = false;
                for (int j = 0; j < currentDevClasses.length; j++) {
                    if (nextAppClass.isAssignableFrom(currentDevClasses[j])) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Found compatible class: " + nextAppClass + " " + currentDevClasses[j]);
                        }
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found incoherency between two channel factories: " + current.getFactory().getName() + ", " + next.getFactory().getName());
                    }
                    throw new IncoherentChainException("Found incoherency between two channel factories: " + current.getFactory().getName() + ", " + next.getFactory().getName());
                }
                current = next;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "verifyChainCoherency");
        }
    }
}
