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
package com.ibm.ws.channelfw.testsuite.channels.protocol;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Dummy protocol-type channel for testing.
 */
@SuppressWarnings("unused")
public class ProtocolDummyChannel implements InboundChannel, Discriminator {
    private ProtocolDummyFactory myFactory;
    /** Channel configuration object */
    private ChannelData config = null;
    /** DiscriminationProcess */
    private DiscriminationProcess discriminationProcess = null;

    /**
     * Constructor.
     * 
     * @param config
     * @param factory
     */
    public ProtocolDummyChannel(ChannelData config, ProtocolDummyFactory factory) {
        this.myFactory = factory;
        update(config);
    }

    public Discriminator getDiscriminator() {
        return this;
    }

    public Class<?> getDiscriminatoryType() {
        return ProtocolDummyContext.class;
    }

    public void destroy() throws ChannelException {
        this.myFactory.removeChannel(getName());
    }

    public Class<?> getApplicationInterface() {
        return ProtocolDummyContext.class;
    }

    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        ProtocolDummyLink link = (ProtocolDummyLink) vc.getStateMap().get("PROTODUMMYLINK");
        if (null == link) {
            link = new ProtocolDummyLink();
        }
        return link;
    }

    public Class<?> getDeviceInterface() {
        return TCPConnectionContext.class;
    }

    public void init() throws ChannelException {
        //
    }

    public void start() throws ChannelException {
        //
    }

    public void stop(long millisec) throws ChannelException {
        //
    }

    public void update(ChannelData cc) {
        this.config = cc;
    }

    // Discriminator methods

    public void cleanUpState(VirtualConnection vc) {
        vc.getStateMap().remove("PROTODUMMYLINK");
    }

    public int discriminate(VirtualConnection vc, Object discrimData) {
        return Discriminator.YES;
    }

    public Channel getChannel() {
        return this;
    }

    public Class<?> getDiscriminatoryDataType() {
        return WsByteBuffer.class;
    }

    public int getWeight() {
        return 0;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getName()
     */
    public String getName() {
        return this.config.getName();
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
     */
    public final DiscriminationProcess getDiscriminationProcess() {
        return this.discriminationProcess;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(com.ibm.wsspi.channelfw.DiscriminationProcess)
     */
    public final void setDiscriminationProcess(DiscriminationProcess dp) {
        this.discriminationProcess = dp;
    }

}
