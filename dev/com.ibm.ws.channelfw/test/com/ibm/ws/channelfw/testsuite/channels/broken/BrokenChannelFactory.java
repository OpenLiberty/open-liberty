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
package com.ibm.ws.channelfw.testsuite.channels.broken;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.ws.channelfw.testsuite.channels.protocol.ProtocolDummyFactory;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;

/**
 * The following channel and channel factory will be used to verify that when a channel of
 * this type is included in a chain in a group, the group can still be
 * inited/started/stopped/destroyed. Specifically, all other chains will be handled
 * and the lifecycle methods will not stop / fail fast.
 */
public class BrokenChannelFactory extends ProtocolDummyFactory {
    /**
     * Constructor.
     * 
     * @throws InvalidChannelFactoryException
     */
    public BrokenChannelFactory() throws InvalidChannelFactoryException {
        super();
    }

    protected Channel createChannel(ChannelData config) {
        return new BrokenChannel(config, this);
    }
}