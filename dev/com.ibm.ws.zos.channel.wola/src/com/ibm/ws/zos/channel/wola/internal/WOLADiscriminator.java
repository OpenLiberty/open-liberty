/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import com.ibm.ws.zos.channel.local.LocalCommDiscriminationData;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * WOLA Discriminator implementation that allows channels lower in the chain to
 * connect to this channel if specific conditions are met.
 */
public class WOLADiscriminator implements Discriminator {

    /**
     * WOLA channel reference.
     */
    final WOLAChannel wolaChannel;

    /**
     * Constructor.
     */
    public WOLADiscriminator(WOLAChannel wolaChannel) {
        this.wolaChannel = wolaChannel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int discriminate(VirtualConnection vc, Object discrimData) {
        // If the discrimination data is not valid return NO.
        if (discrimData == null) {
            return Discriminator.NO;
        }

        // Check the protocol. If it is a WOLA protocol, return YES.
        LocalCommDiscriminationData data = (LocalCommDiscriminationData) discrimData;
        if (data.getProtocol() == LocalCommDiscriminationData.WOLA_PROTOCOL) {
            return Discriminator.YES;
        }

        return Discriminator.NO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanUpState(VirtualConnection vc) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getDiscriminatoryDataType() {
        return LocalCommDiscriminationData.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel getChannel() {
        return wolaChannel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWeight() {
        return wolaChannel.getChannelConfigData().getDiscriminatorWeight();
    }

}
