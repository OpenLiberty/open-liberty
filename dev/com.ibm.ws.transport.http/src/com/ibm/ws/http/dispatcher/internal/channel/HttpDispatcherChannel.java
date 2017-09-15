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
package com.ibm.ws.http.dispatcher.internal.channel;

import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * Application channel that sits atop an HTTP protocol chain to handle the
 * routing of incoming HTTP requests.
 */
public class HttpDispatcherChannel implements InboundChannel, Discriminator {
    /** trace variable */
    private static final TraceComponent tc = Tr.register(HttpDispatcherChannel.class);

    /** Factory that created this channel instance */
    private HttpDispatcherFactory myFactory;
    /** Configuration information */
    private HttpDispatcherConfig myConfig;
    /** Channel configuration object */
    private ChannelData chfwConfig = null;
    /** Active connection counter */
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    /** Flag on whether this channel is currently quiescing or not */
    private boolean quiescing = false;

    /** Flag on whether stop with no quiese has been called after the last start call */
    private volatile boolean stop0Called = false;

    /**
     * Constructor.
     * 
     * @param config
     * @param factory
     */
    public HttpDispatcherChannel(ChannelData config, HttpDispatcherFactory factory) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Created channel: " + this);
        }
        this.myFactory = factory;
        update(config);
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminator()
     */
    @Override
    @Trivial
    public Discriminator getDiscriminator() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#destroy()
     */
    @Override
    public void destroy() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Destroy channel: " + this);
        }
        if (null != this.myFactory) {
            this.myFactory.removeChannel(getName());
            this.myFactory = null;
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        HttpDispatcherLink link = (HttpDispatcherLink) vc.getStateMap().get(HttpDispatcherLink.LINK_ID);
        if (null == link) {
            link = new HttpDispatcherLink();
            link.init(vc, this);
        }
        return link;
    }

    /**
     * Increase the number of active connections currently being processed inside
     * the HTTP dispatcher.
     */
    protected void incrementActiveConns() {
        int count = this.activeConnections.incrementAndGet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Increment active, current=" + count);
        }
    }

    /**
     * Decrement the number of active connections being processed by the dispatcher.
     */
    protected void decrementActiveConns() {
        int count = this.activeConnections.decrementAndGet();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Decrement active, current=" + count);
        }
        if (0 == count && this.quiescing) {
            signalNoConnections();
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
     */
    @Override
    @Trivial
    public Class<?> getDeviceInterface() {
        return HttpInboundServiceContext.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    @Override
    @Trivial
    public void init() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Init channel: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    @Override
    @Trivial
    public void start() throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Start channel: " + this);
        }
        stop0Called = false;
    }

    /**
     * Send an event to the channel framework that there are no more active
     * connections on this quiesced channel instance. This will allow an early
     * final chain stop instead of waiting the full quiesce timeout length.
     */
    private void signalNoConnections() {
        EventEngine events = HttpDispatcher.getEventService();
        if (null == events) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unable to send event, missing service");
            }
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "No active connections, sending stop chain event");
        }
        Event event = events.createEvent(ChannelFramework.EVENT_STOPCHAIN);
        event.setProperty(ChannelFramework.EVENT_CHANNELNAME, this.chfwConfig.getExternalName());
        events.postEvent(event);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#stop(long)
     */
    @Override
    public void stop(long millisec) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stop channel: " + this + " time=" + millisec);
        }
        if (0L < millisec) {
            this.quiescing = true;
            if (0 == this.activeConnections.get()) {
                // no current connections, notify the final stop can happen now
                signalNoConnections();
            }
        } else {
            this.quiescing = false;
            stop0Called = true;
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#update(com.ibm.websphere.channelfw.ChannelData)
     */
    @Override
    public void update(ChannelData cc) {
        this.chfwConfig = cc;
        this.myConfig = new HttpDispatcherConfig(cc.getPropertyBag());
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#cleanUpState(com.ibm.wsspi.channelfw.VirtualConnection)
     */
    @Override
    @Trivial
    public void cleanUpState(VirtualConnection vc) {
        vc.getStateMap().remove(HttpDispatcherLink.LINK_ID);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#discriminate(com.ibm.wsspi.channelfw.VirtualConnection, java.lang.Object)
     */
    @Override
    @Trivial
    public int discriminate(VirtualConnection vc, Object discrimData) {
        return Discriminator.YES;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getChannel()
     */
    @Override
    @Trivial
    public Channel getChannel() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getDiscriminatoryDataType()
     */
    @Override
    @Trivial
    public Class<?> getDiscriminatoryDataType() {
        return HttpRequestMessage.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getWeight()
     */
    @Override
    @Trivial
    public int getWeight() {
        return this.chfwConfig.getDiscriminatorWeight();
    }

    /**
     * Access the Dispatcher configuration information.
     * 
     * @return HttpDispatcherConfig
     */
    @Trivial
    protected HttpDispatcherConfig getDispConfig() {
        return this.myConfig;
    }

    /*
     * @see com.ibm.wsspi.channelfw.base.InboundApplicationChannel#getName()
     */
    @Override
    @Trivial
    public String getName() {
        return this.chfwConfig.getName();
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminationProcess()
     */
    @Override
    @Trivial
    public DiscriminationProcess getDiscriminationProcess() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(DiscriminationProcess)
     */
    @Override
    @Trivial
    public void setDiscriminationProcess(DiscriminationProcess dp) {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getApplicationInterface()
     */
    @Override
    @Trivial
    public Class<?> getApplicationInterface() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminatoryType()
     */
    @Override
    @Trivial
    public Class<?> getDiscriminatoryType() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /**
     * @return pid for the endpoint
     */
    public String getEndpointPid() {
        return myConfig.getEndpointPid();
    }

    /**
     * @return stop 0 quiesce state of the channel
     */
    public boolean getStop0Called() {
        return stop0Called;
    }

}
