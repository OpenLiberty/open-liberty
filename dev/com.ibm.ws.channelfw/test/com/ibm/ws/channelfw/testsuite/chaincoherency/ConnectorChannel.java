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
    private final Class<?> devSideClass = TCPConnectionContext.class;
    private final Class<?> appSideClass = TCPConnectionContext.class;
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

    @Override
    public Class<?> getDiscriminatoryType() {
        return discType;
    }

    @Override
    public Class<?> getDeviceAddress() {
        return devAddress;
    }

    @Override
    public Class<?>[] getApplicationAddress() {
        return appAddresses;
    }

    /**
     * @see Channel#bind()
     */
    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        return new ConnectorChannelLink(vc, this);
    }

    /**
     * @see Channel#getDiscriminator()
     */
    @Override
    public Discriminator getDiscriminator() {
        return discriminatorInstance;
    }

    /**
     * @see Channel#getDevSideInterfaceClass()
     */
    @Override
    public Class<?> getDeviceInterface() {
        return devSideClass;
    }

    /**
     * @see Channel#getAppSideInterfaceClass()
     */
    @Override
    public Class<?> getApplicationInterface() {
        return appSideClass;
    }

    /**
     * @see Channel#updateConfig(ChannelData)
     */
    @Override
    public void update(ChannelData cc) {
        this.config = cc;
    }

    /**
     * @see Channel#start()
     */
    @Override
    public void start() throws ChannelException {
        // nothing to do
    }

    /**
     * @see Channel#stop(int)
     */
    @Override
    public void stop(long millisec) throws ChannelException {
        // nothing to do
    }

    /**
     * @see Channel#init()
     */
    @Override
    public void init() throws ChannelException {
        // nothing to do
    }

    /**
     * @see Channel#destroy()
     */
    @Override
    public void destroy() throws ChannelException {
        // nothing to do
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getName()
     */
    @Override
    public String getName() {
        return this.config.getName();
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
     */
    @Override
    public final DiscriminationProcess getDiscriminationProcess() {
        return this.discriminationProcess;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(com.ibm.wsspi.channelfw.DiscriminationProcess)
     */
    @Override
    public final void setDiscriminationProcess(DiscriminationProcess dp) {
        this.discriminationProcess = dp;
    }

}
