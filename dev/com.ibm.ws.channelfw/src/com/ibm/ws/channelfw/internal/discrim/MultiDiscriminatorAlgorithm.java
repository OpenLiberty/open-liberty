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
package com.ibm.ws.channelfw.internal.discrim;

import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.ws.channelfw.internal.InboundVirtualConnectionImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.exception.DiscriminationProcessException;

/**
 * Discriminator Algorithm for multiple discriminators.
 */
public class MultiDiscriminatorAlgorithm implements DiscriminationAlgorithm {
    /**
     * TraceComponent
     */
    private static final TraceComponent tc = Tr.register(MultiDiscriminatorAlgorithm.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);
    /**
     * Reference to the dp that uses this algorithm
     */
    private DiscriminationGroup discriminationGroup = null;
    /**
     * reference to the discriminators
     */
    private List<Discriminator> discriminators = null;
    /**
     * number of discriminators
     */
    private int numDiscriminators = 0;

    /**
     * Constructor.
     * 
     * @param discGroup
     *            Set of discriminators in the group
     */
    MultiDiscriminatorAlgorithm(DiscriminationGroup discGroup) {
        this.discriminationGroup = discGroup;
        this.discriminators = discriminationGroup.getDiscriminators();
        this.numDiscriminators = discriminators.size();
    }

    /**
     * @see com.ibm.ws.channelfw.internal.discrim.DiscriminationAlgorithm#discriminate(InboundVirtualConnection, Object, ConnectionLink)
     */
    public int discriminate(InboundVirtualConnection vc, Object discrimData, ConnectionLink prevChannelLink) throws DiscriminationProcessException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "discriminate: " + vc);
        }
        int state = DiscriminationProcess.AGAIN;
        boolean newStatus = false;
        int maybeCount = 0, maybeIndex = 0;
        int[] status = vc.getDiscriminatorStatus();
        boolean groupChanged = false;
        // Perform "real" discrimination.
        // CONN_RUNTIME: This does the discrimination if there is multiple
        // discriminators. only a new int[] created during this.
        if (status == null || (groupChanged = (0 != discriminationGroup.compareTo(vc.getDiscriminationGroup())))) {
            // Ensure all discriminator state is clear from the VC.
            if (groupChanged) {
                ((InboundVirtualConnectionImpl) vc).cleanUpAllDiscriminatorState();
            }
            // Create a new status array.
            status = new int[numDiscriminators];
            newStatus = true;
        }
        Discriminator d = null;
        for (int i = 0; (i < numDiscriminators) && (state == DiscriminationProcess.AGAIN); ++i) {
            // Only include this discriminator in the decision if it has
            // not previously disqualified itself
            if (!newStatus && status[i] == Discriminator.NO) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Skipping a discriminator that has a \"NO\" status");
                }
                continue;
            }
            d = discriminators.get(i);
            // check to see if this is the last guy and we have all "nos"
            if (maybeCount == 0 && i == (numDiscriminators - 1)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Discrimination has all no's and one last guy");
                }
                state = DiscriminationProcess.SUCCESS;
                break;
            }
            // CONN_RUNTIME: This part does call out to channel's discriminators
            switch (d.discriminate(vc, discrimData)) {
                case (Discriminator.YES):
                    state = DiscriminationProcess.SUCCESS;
                    status[i] = Discriminator.YES;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "discriminator " + d + " reports success");
                    }
                    break;
                case (Discriminator.NO):
                    // This discriminator cannot ever claim the data
                    status[i] = Discriminator.NO;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "discriminator " + d + " reports no");
                    }
                    break;
                case (Discriminator.MAYBE):
                    maybeCount++;
                    maybeIndex = i;
                    status[i] = Discriminator.MAYBE;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "discriminator " + d + " reports maybe");
                    }
                    // This discriminator wants to wait and see...
                    break;
                default:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Discriminator " + d + " returned an incorrect status code");
                    }
                    break;
            }
        }
        // Single Maybe case
        if (state == DiscriminationProcess.AGAIN && maybeCount == 1) {
            d = discriminators.get(maybeIndex);
            state = DiscriminationProcess.SUCCESS;
            // Decrement the maybeCount to save some cycles later when cleaning up
            // disc state.
            maybeCount--;
            // Trigger that this discriminator was chosen so it shouldn't be cleaned
            // up.
            status[maybeIndex] = Discriminator.YES;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Discriminator enhancement: single maybe case");
            }
        }
        // SUCCESS Case
        if ((state == DiscriminationProcess.SUCCESS)) {
            if (d == null) {
                DiscriminationProcessException e = new DiscriminationProcessException("Should not happen!");
                FFDCFilter.processException(e, getClass().getName() + ".discriminate", "150", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found null discriminator");
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "discriminate");
                }
                throw e;
            }
            // This discriminator has claimed the data
            Channel nextChannel = d.getChannel();
            ConnectionLink nextChannelLink = nextChannel.getConnectionLink(vc);
            prevChannelLink.setApplicationCallback(nextChannelLink);
            nextChannelLink.setDeviceLink(prevChannelLink);
            // Clean up any discriminators that returned MAYBE, but were not chosen
            // Note if chosen disc returned MAYBE, status was updated to YES.
            if (maybeCount >= 1) {
                ((InboundVirtualConnectionImpl) vc).cleanUpMaybeDiscriminatorState();
            }
            // clear the status
            vc.setDiscriminatorStatus(null);
            status = null;
        } else if (state == DiscriminationProcess.AGAIN) {
            vc.setDiscriminatorStatus(status);
            vc.setDiscriminationGroup(discriminationGroup);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "discriminate: " + state);
        }
        return state;
    }
}
