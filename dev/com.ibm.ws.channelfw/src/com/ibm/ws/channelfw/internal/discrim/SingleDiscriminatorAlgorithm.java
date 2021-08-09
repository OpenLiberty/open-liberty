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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.channelfw.internal.InboundVirtualConnection;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * This is the single discriminator algorithm that will handle the required
 * steps for discrimination but always assumes success of that call.
 */
public class SingleDiscriminatorAlgorithm implements DiscriminationAlgorithm {
    /**
     * TraceComponent
     */
    private static final TraceComponent tc = Tr.register(SingleDiscriminatorAlgorithm.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);
    /**
     * The discrimination group this algorithm is associated with
     */
    private DiscriminationGroup discriminationGroup = null;
    /**
     * Channel above
     */
    private Channel nextChannel = null;

    /**
     * Constructor.
     * 
     * @param discGroup
     *            Set of discriminators
     */
    SingleDiscriminatorAlgorithm(DiscriminationGroup discGroup) {
        // CONN_RUNTIME: channels with one discriminator attached, this links the
        // channels.
        this.discriminationGroup = discGroup;
        // get the single discriminator and get its Channel
        this.nextChannel = discriminationGroup.getDiscriminators().get(0).getChannel();
    }

    /**
     * Use a VirtualConnection rather than a InboundVirtualConnection for discrimination
     */
    public int discriminate(VirtualConnection vc, Object discrimData, ConnectionLink prevChannelLink) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "discriminate: " + vc);
        }
        ConnectionLink nextChannelLink = nextChannel.getConnectionLink(vc);
        prevChannelLink.setApplicationCallback(nextChannelLink);
        nextChannelLink.setDeviceLink(prevChannelLink);
        return DiscriminationProcess.SUCCESS;
    }

    /**
     * @see com.ibm.ws.channelfw.internal.discrim.DiscriminationAlgorithm#discriminate(InboundVirtualConnection, Object, ConnectionLink)
     */
    @Override
    public int discriminate(InboundVirtualConnection vc, Object discrimData, ConnectionLink prevChannelLink) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "discriminate: " + vc);
        }
        ConnectionLink nextChannelLink = nextChannel.getConnectionLink(vc);
        prevChannelLink.setApplicationCallback(nextChannelLink);
        nextChannelLink.setDeviceLink(prevChannelLink);
        return DiscriminationProcess.SUCCESS;
    }

}
