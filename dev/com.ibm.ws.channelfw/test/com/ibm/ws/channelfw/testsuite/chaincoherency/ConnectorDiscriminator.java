/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.chaincoherency;

import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Discriminator instance for the test channel.
 */
@SuppressWarnings("unused")
public class ConnectorDiscriminator implements Discriminator {
    private Class<?> discDataType = null;
    private InboundChannel channel = null;

    /**
     * Constructor.
     * 
     * @param inputChannel
     * @param inputDiscDataType
     */
    public ConnectorDiscriminator(InboundChannel inputChannel, Class<?> inputDiscDataType) {
        discDataType = inputDiscDataType;
        channel = inputChannel;
    }

    /**
     * @see Discriminator#getDiscriminatoryDataType()
     */
    public Class<?> getDiscriminatoryDataType() {
        return discDataType;
    }

    /**
     * Examines a piece of data and determines if it should be processed
     * by the channel associated with this discriminator.
     * 
     * @see Discriminator#discriminate(VirtualConnection, Object)
     */
    public int discriminate(VirtualConnection vc, Object discrimData) {
        return Discriminator.YES;
    }

    /**
     * Returns the channel that this discriminator will discriminate on
     * behalf of.
     * 
     * @see Discriminator#getChannel()
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * @see Discriminator#getWeight()
     */
    public int getWeight() {
        return 1;
    }

    /**
     * @see Discriminator#cleanUpState(VirtualConnection)
     */
    public void cleanUpState(VirtualConnection vc) {
        // Nothing to clean up.
    }
}
