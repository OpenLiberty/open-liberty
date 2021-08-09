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
/*
 * Created on Sep 3, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.ibm.ws.channelfw.testsuite.channels.connector;

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
 * 
 */
@SuppressWarnings("unused")
public class MainChannel implements InboundChannel, Discriminator {
    private ChannelData mainConfig = null;
    private DiscriminationProcess myDP = null;

    /**
     * Constructor.
     * 
     * @param config
     */
    public MainChannel(ChannelData config) {
        mainConfig = config;
    }

    public Discriminator getDiscriminator() {
        return this;
    }

    public DiscriminationProcess getDiscriminationProcess() {
        return myDP;
    }

    public void setDiscriminationProcess(DiscriminationProcess dp) {
        myDP = dp;
    }

    public Class<?> getDiscriminatoryType() {
        return WsByteBuffer.class;
    }

    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        return null;
    }

    public void start() throws ChannelException {
        // nothing
    }

    public void stop(long millisec) throws ChannelException {
        // nothing
    }

    public void init() throws ChannelException {
        // nothing
    }

    public void destroy() throws ChannelException {
        // nothing
    }

    public String getName() {
        return mainConfig.getName();
    }

    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

    public Class<?> getDeviceInterface() {
        return TCPConnectionContext.class;
    }

    public void update(ChannelData cc) {
        mainConfig = cc;
    }

    public int discriminate(VirtualConnection vc, Object discrimData) {
        return Discriminator.YES;
    }

    public Class<?> getDiscriminatoryDataType() {
        return null;
    }

    public Channel getChannel() {
        return this;
    }

    public int getWeight() {
        return 0;
    }

    public void cleanUpState(VirtualConnection vc) {
        // Nothing to clean up.
    }

}
