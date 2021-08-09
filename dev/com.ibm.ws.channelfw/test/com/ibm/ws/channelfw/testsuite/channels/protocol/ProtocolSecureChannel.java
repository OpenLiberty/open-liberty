/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
 * Secure protocol (mid-chain) channel.
 */
@SuppressWarnings("unused")
public class ProtocolSecureChannel implements InboundChannel, Discriminator {
    private ChannelData config;
    private DiscriminationProcess discriminationProcess = null;

    /**
     * Constructor.
     * 
     * @param data
     */
    public ProtocolSecureChannel(ChannelData data) {
        this.config = data;
    }

    @Override
    public void destroy() throws ChannelException {
        // nothing
    }

    @Override
    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        return new PassThruLink();
    }

    @Override
    public Class<?> getDeviceInterface() {
        return TCPConnectionContext.class;
    }

    @Override
    public String getName() {
        return this.config.getName();
    }

    @Override
    public void init() throws ChannelException {
        // nothing
    }

    @Override
    public void start() throws ChannelException {
        // nothing
    }

    @Override
    public void stop(long millisec) throws ChannelException {
        // nothing
    }

    @Override
    public void update(ChannelData cc) {
        // nothing
    }

    @Override
    public DiscriminationProcess getDiscriminationProcess() {
        return this.discriminationProcess;
    }

    @Override
    public Discriminator getDiscriminator() {
        return this;
    }

    @Override
    public Class<?> getDiscriminatoryType() {
        return WsByteBuffer.class;
    }

    @Override
    public void setDiscriminationProcess(DiscriminationProcess dp) {
        this.discriminationProcess = dp;
    }

    @Override
    public void cleanUpState(VirtualConnection vc) {
        // nothing
    }

    @Override
    public int discriminate(VirtualConnection vc, Object discrimData) {
        return Discriminator.YES;
    }

    @Override
    public Channel getChannel() {
        return this;
    }

    @Override
    public Class<?> getDiscriminatoryDataType() {
        return WsByteBuffer.class;
    }

    @Override
    public int getWeight() {
        return 0;
    }

}
