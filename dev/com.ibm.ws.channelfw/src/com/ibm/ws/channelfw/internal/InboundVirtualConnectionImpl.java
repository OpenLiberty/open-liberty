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

import java.util.Iterator;

import com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup;
import com.ibm.wsspi.channelfw.Discriminator;

/**
 * VirtualInboundConnection implementation. This is the implementation for
 * Inbound VirtualConnections, used
 * only for the internals of the ChannelFramework.
 */
public class InboundVirtualConnectionImpl extends VirtualConnectionImpl implements InboundVirtualConnection {
    /**
     * discriminator status (remembered for an additional discriminator.
     */
    private int[] discStatus;

    /**
     * DiscriminationProcessImpl for validating discriminator state.
     */
    private DiscriminationGroup dp = null;

    /**
     * Constructor for the VirtualInboundConnectionImpl
     */
    protected InboundVirtualConnectionImpl() {
        // Nothing needed here at this time.
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.InboundVirtualConnection#getDiscriminatorStatus
     * ()
     */
    public int[] getDiscriminatorStatus() {
        return this.discStatus;
    }

    /*
     * @see com.ibm.ws.channelfw.internal.VirtualConnectionImpl#destroy()
     */
    public void destroy() {
        this.discStatus = null;
        // Clean up any state stored by discriminators.
        cleanUpAllDiscriminatorState();
        super.destroy();
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.InboundVirtualConnection#setDiscriminatorStatus
     * (int[])
     */
    public void setDiscriminatorStatus(int[] status) {
        this.discStatus = status;
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.InboundVirtualConnection#setDiscriminationGroup
     * (com.ibm.ws.channelfw.internal.discrim.DiscriminationGroup)
     */
    public void setDiscriminationGroup(DiscriminationGroup dp) {
        this.dp = dp;
    }

    /*
     * @see
     * com.ibm.ws.channelfw.internal.InboundVirtualConnection#getDiscriminationGroup
     * ()
     */
    public DiscriminationGroup getDiscriminationGroup() {
        return this.dp;
    }

    /**
     * Clean up potential state information left in this VC from any
     * of the discriminators in the group which resulted in MAYBE during
     * the discrimination process.
     */
    public void cleanUpMaybeDiscriminatorState() {
        if (this.dp != null) {
            Discriminator d;
            Iterator<Discriminator> it = this.dp.getDiscriminators().iterator();
            int i = 0;
            while (it.hasNext()) {
                d = it.next();
                if (Discriminator.MAYBE == this.discStatus[i++]) {
                    d.cleanUpState(this);
                }
            }
        }
    }

    /**
     * Clean up potential state information left in this VC from any
     * of the discriminators in the group.
     */
    public void cleanUpAllDiscriminatorState() {
        if (this.dp != null) {
            Iterator<Discriminator> it = this.dp.getDiscriminators().iterator();
            while (it.hasNext()) {
                it.next().cleanUpState(this);
            }
        }
    }

}
