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

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.OutboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.InvalidChannelFactoryException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Test channel instance.
 */
@SuppressWarnings("unused")
public class ConnectorChannel implements InboundChannel, OutboundChannel {
    protected Discriminator discriminatorInstance = null;
    private Class<?> devSideClass = TCPConnectionContext.class;
    private Class<?> appSideClass = TCPConnectionContext.class;
    protected Class<?> devAddress = null;
    protected Class<?> appAddresses[] = null;
    private Class<?> discType = null;
    /** Channel configuration object */
    private ChannelData config = null;
    /** DiscriminationProcess */
    private DiscriminationProcess discriminationProcess = null;

    /**
     * Constructor.
     * 
     * @param cc
     * @param inputDiscType
     * @param inputDiscDataType
     * @param inputDevAddr
     * @param inputAppAddrs
     * @throws InvalidChannelFactoryException
     */
    public ConnectorChannel(ChannelData cc, Class<?> inputDiscType, Class<?> inputDiscDataType,
                            Class<?> inputDevAddr, Class<?>[] inputAppAddrs) throws InvalidChannelFactoryException {
        discType = inputDiscType;
        discriminatorInstance = new ConnectorDiscriminator(this, inputDiscDataType);
        devAddress = inputDevAddr;
        appAddresses = inputAppAddrs;
        update(cc);
    }

    public Class<?> getDiscriminatoryType() {
        return discType;
    }

    public Class<?> getDeviceAddress() {
        return devAddress;
    }

    public Class<?>[] getApplicationAddress() {
        return appAddresses;
    }

    /**
     * @see com.ibm.wsspi.channel.Channel#bind()
     */
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        return new ConnectorChannelLink(vc, this);
    }

    /**
     * @see com.ibm.wsspi.channel.Channel#getDiscriminator()
     */
    public Discriminator getDiscriminator() {
        return discriminatorInstance;
    }

    /**
     * @see Channel#getDevSideInterfaceClass()
     */
    public Class<?> getDeviceInterface() {
        return devSideClass;
    }

    /**
     * @see Channel#getAppSideInterfaceClass()
     */
    public Class<?> getApplicationInterface() {
        return appSideClass;
    }

    /**
     * @see Channel#updateConfig(ChannelData)
     */
    public void update(ChannelData cc) {
        this.config = cc;
    }

    /**
     * @see Channel#start()
     */
    public void start() throws ChannelException {
        // nothing to do
    }

    /**
     * @see Channel#stop(int)
     */
    public void stop(long millisec) throws ChannelException {
        // nothing to do
    }

    /**
     * @see Channel#init()
     */
    public void init() throws ChannelException {
        // nothing to do
    }

    /**
     * @see Channel#destroy()
     */
    public void destroy() throws ChannelException {
        // nothing to do
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
