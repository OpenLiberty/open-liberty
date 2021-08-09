/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.outbound;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpFactoryConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.OutboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.channel.outbound.HttpAddress;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;
import com.ibm.wsspi.tcpchannel.TCPConnectRequestContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * This represents an HTTP channel specific to outbound requests for a remote
 * server/site. An application above will initiate the outbound
 * request through this channel and this will parse the response from
 * the remote target and provide that to the caller.
 * 
 */
public class HttpOutboundChannel implements OutboundChannel {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpOutboundChannel.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Idle, fully stopped state for this channel */
    private static final int STATE_STOPPED = 0;
    /** A delayed stop has started but not fully finished */
    private static final int STATE_STOPPING = 1;
    /** Standard fully running state for the channel */
    private static final int STATE_RUNNING = 2;

    /** Application side interface class */
    private Class<?> appInterface = null;
    /** Device side interface class */
    private Class<?> devInterface = null;
    /** Device side address interface */
    private Class<?> devAddrInterface = null;
    /** Application side address interfaces */
    private Class<?>[] appAddrInterfaces = new Class[1];
    /** HTTP factory for objects */
    private HttpObjectFactory myObjectFactory = null;
    /** Channel factory that owns this channel */
    private HttpOutboundChannelFactory channelFactory = null;
    /** Channel config link */
    private HttpChannelConfig myConfig = null;
    /** Flag on whether this channel is running or not */
    private int myState = STATE_STOPPED;
    /** CHFW configuration object */
    private ChannelData chfwConfig = null;

    /**
     * Constructor
     * 
     * @param cc
     *            - Channel configuration object
     * @param cf
     *            - Owning channel factory
     * @param of
     *            - Object factory to use in channel
     */
    public HttpOutboundChannel(ChannelData cc, HttpOutboundChannelFactory cf, HttpObjectFactory of) {
        this.channelFactory = cf;
        this.appInterface = HttpOutboundServiceContext.class;
        this.devInterface = TCPConnectionContext.class;
        this.devAddrInterface = TCPConnectRequestContext.class;
        this.appAddrInterfaces[0] = HttpAddress.class;
        this.myObjectFactory = of;
        update(cc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created: " + this);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw
     * .VirtualConnection)
     */
    public final ConnectionLink getConnectionLink(VirtualConnection vc) {
        return new HttpOutboundLink(this, vc);
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getApplicationInterface()
     */
    public final Class<?> getApplicationInterface() {
        return this.appInterface;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
     */
    public final Class<?> getDeviceInterface() {
        return this.devInterface;
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundChannel#getDeviceAddress()
     */
    public final Class<?> getDeviceAddress() {
        return this.devAddrInterface;
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundChannel#getApplicationAddress()
     */
    public final Class<?>[] getApplicationAddress() {
        return this.appAddrInterfaces;
    }

    /**
     * Query the HTTP channel configuration for this channel
     * 
     * @return HttpChannelConfig
     */
    public final HttpChannelConfig getHttpConfig() {
        return this.myConfig;
    }

    /**
     * Set the HTTP channel configuration to the input object
     * 
     * @param hcc
     */
    public final void setHttpConfig(HttpChannelConfig hcc) {
        this.myConfig = hcc;
    }

    /**
     * Get access to the factory configuration that created this channel.
     * 
     * @return HttpFactoryConfig
     */
    final public HttpFactoryConfig getFactoryConfig() {
        return this.channelFactory.getConfig();
    }

    /**
     * Query the HTTP object factory
     * 
     * @return HttpObjectFactory
     */
    public final HttpObjectFactory getObjectFactory() {
        return this.myObjectFactory;
    }

    /**
     * Query whether or not this channel is actively running or not.
     * 
     * @return boolean
     */
    public synchronized boolean isRunning() {
        // PK12235, more specific states
        return STATE_RUNNING == this.myState;
    }

    /**
     * Query whether this channel is in the process of stopping or is already
     * stopped.
     * 
     * @return boolean
     */
    public synchronized boolean isStopping() {
        // PK12235, more specific states
        return STATE_RUNNING != this.myState;
    }

    /**
     * Query whether this channel is completely stopped.
     * 
     * @return boolean
     */
    public synchronized boolean isStopped() {
        // PK12235, more specific states
        return STATE_STOPPED == this.myState;
    }

    /**
     * Set the channel state flag to the input value.
     * 
     * @param state
     */
    private synchronized void setState(int state) {
        this.myState = state;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    public void start() {
        setState(STATE_RUNNING);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Starting: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#stop(long)
     */
    public void stop(long millisec) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Stopping (" + millisec + ") : " + this);
        }
        if (0 == millisec) {
            setState(STATE_STOPPED);
        } else {
            setState(STATE_STOPPING);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    public void init() {
        // there are no extra steps beyond the constructor as there are
        // no channel objects that start or stop
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Initializing: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#destroy()
     */
    public void destroy() {
        setState(STATE_STOPPED);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying: " + this);
        }
        this.channelFactory.removeChannel(getName());
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.Channel#update(com.ibm.websphere.channelfw.ChannelData
     * )
     */
    public void update(ChannelData cc) {
        this.chfwConfig = cc;
        setHttpConfig(new HttpChannelConfig(cc));
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getName()
     */
    public String getName() {
        return this.chfwConfig.getName();
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.chfwConfig.getName() + " state=" + this.myState;
    }
}
