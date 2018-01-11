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
package com.ibm.ws.http.channel.test.server;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * A channel which can act as a HTTP server. There is nothing particularly
 * interesting in this code. It is just a simple implementation of the
 * InboundChannel interface.
 */
@SuppressWarnings("unused")
public class HTTPServerChannel implements InboundChannel, Discriminator {

    /** RAS debug reference */
    private static final TraceComponent tc = Tr.register(
                                                         HTTPServerChannel.class,
                                                         "HTTPChannelTest",
                                                         "com.ibm.ws.http.channel.test");

    private Discriminator discriminatorInstance = null;
    private HTTPServerChannelFactory myFactory = null;
    private ChannelData chfwConfig = null;

    /**
     * Create a server instance.
     * 
     * @param cc
     */
    protected HTTPServerChannel(ChannelData cc, HTTPServerChannelFactory factory) {
        this.myFactory = factory;
        this.discriminatorInstance = this;
        update(cc);
        Tr.debug(tc, "Created server channel; " + this);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        return new HTTPServerConnLink(vc);
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminator()
     */
    public Discriminator getDiscriminator() {
        return this.discriminatorInstance;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
     */
    public Class<?> getDeviceInterface() {
        return HttpInboundServiceContext.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#update(com.ibm.websphere.channelfw.ChannelData)
     */
    public void update(ChannelData cc) {
        this.chfwConfig = cc;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    public void start() {
        Tr.debug(tc, "Started server channel; " + this);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#stop(long)
     */
    public void stop(long millisec) {
        Tr.debug(tc, "Stopped server channel; " + this);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    public void init() {
        Tr.debug(tc, "Inited server channel; " + this);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#destroy()
     */
    public void destroy() {
        this.myFactory.removeChannel(getName());
        Tr.debug(tc, "Destroyed server channel; " + this);
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.InboundApplicationChannel#getName()
     */
    public String getName() {
        return this.chfwConfig.getName();
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
     */
    public DiscriminationProcess getDiscriminationProcess() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(DiscriminationProcess)
     */
    public void setDiscriminationProcess(DiscriminationProcess dp) {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getApplicationInterface()
     */
    public Class<?> getApplicationInterface() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminatoryType()
     */
    public Class<?> getDiscriminatoryType() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#cleanUpState(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public void cleanUpState(VirtualConnection vc) {
        // nothing
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#discriminate(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Object)
     */
    @Override
    public int discriminate(VirtualConnection vc, Object discrimData) {
        return Discriminator.YES;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getChannel()
     */
    @Override
    public Channel getChannel() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getDiscriminatoryDataType()
     */
    @Override
    public Class<?> getDiscriminatoryDataType() {
        return HttpRequestMessage.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getWeight()
     */
    @Override
    public int getWeight() {
        return 0;
    }

}
