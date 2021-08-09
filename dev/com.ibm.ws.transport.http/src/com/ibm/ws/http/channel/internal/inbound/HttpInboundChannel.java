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
package com.ibm.ws.http.channel.internal.inbound;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.CallbackIDs;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.DiscriminationProcess;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.InboundChannel;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.MalformedMessageException;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * This class handles inbound HTTP requests from a remote client. It
 * handles parsing the request and using discrimination to find an
 * application channel above that will handle the request and send
 * a response.
 * 
 */
public class HttpInboundChannel implements InboundChannel, Discriminator {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpInboundChannel.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Idle, fully stopped state for this channel */
    private static final int STATE_STOPPED = 0;
    /** A delayed stop has started but not fully finished */
    private static final int STATE_STOPPING = 1;
    /** Standard fully running state for the channel */
    private static final int STATE_RUNNING = 2;

    /** Factory to create HTTP related objects */
    private HttpObjectFactory myObjectFactory = null;
    /** Channel factory that owns this channel */
    private HttpInboundChannelFactory channelFactory = null;
    /** Configuration of this channel */
    private HttpChannelConfig myConfig = null;
    /** State flag on whether we're running or not */
    private int myState = STATE_STOPPED;
    /** Channel configuration object */
    private ChannelData config = null;
    /** DiscriminationProcess */
    private DiscriminationProcess discriminationProcess = null;

    /**
     * Constructor.
     * 
     * @param cc
     *            - Channel configuration object
     * @param cf
     *            - Owning channel factory
     * @param of
     *            - Object factory to use in channel
     */
    public HttpInboundChannel(ChannelData cc, HttpInboundChannelFactory cf, HttpObjectFactory of) {
        this.channelFactory = cf;
        this.myObjectFactory = of;
        update(cc);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Created: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminator()
     */
    public final Discriminator getDiscriminator() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getApplicationInterface()
     */
    public final Class<?> getApplicationInterface() {
        return HttpInboundServiceContext.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#getDeviceInterface()
     */
    public final Class<?> getDeviceInterface() {
        return TCPConnectionContext.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getChannel()
     */
    public final Channel getChannel() {
        return this;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getDiscriminatoryDataType()
     */
    public final Class<?> getDiscriminatoryDataType() {
        return WsByteBuffer.class;
    }

    /*
     * @see com.ibm.wsspi.channelfw.InboundChannel#getDiscriminatoryType()
     */
    public final Class<?> getDiscriminatoryType() {
        return HttpRequestMessage.class;
    }

    /**
     * Query the HTTP channel configuration for this channel.
     * 
     * @return HttpChannelConfig
     */
    public final HttpChannelConfig getHttpConfig() {
        return this.myConfig;
    }

    /**
     * Set the HTTP channel configuration for this channel to the input object.
     * 
     * @param hcc
     */
    final public void setHttpConfig(HttpChannelConfig hcc) {
        this.myConfig = hcc;
    }

    /**
     * Provide access to the factory that created this channel instance.
     * 
     * @return HttpInboundChannelFactory
     */
    final public HttpInboundChannelFactory getFactory() {
        return this.channelFactory;
    }

    /**
     * Query the Http object factory link.
     * 
     * @return HttpObjectFactory
     */
    public final HttpObjectFactory getObjectFactory() {
        return this.myObjectFactory;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Discriminator#getWeight()
     */
    public final int getWeight() {
        return this.config.getDiscriminatorWeight();
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.Channel#getConnectionLink(com.ibm.wsspi.channelfw
     * .VirtualConnection)
     */
    public ConnectionLink getConnectionLink(VirtualConnection vc) {
        HttpInboundLink link = (HttpInboundLink) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
        if (null == link) {
            link = new HttpInboundLink(this, vc);
        }
        return link;
    }

    /**
     * Query whether or not this channel is actively running or not.
     * 
     * @return boolean
     */
    public boolean isRunning() {
        // PK12235, more specific states
        return STATE_RUNNING == this.myState;
    }

    /**
     * Query whether this channel is in the process of stopping or is already
     * stopped.
     * 
     * @return boolean
     */
    public boolean isStopping() {
        // PK12235, more specific states
        return STATE_RUNNING != this.myState;
    }

    /**
     * Query whether this channel is completely stopped.
     * 
     * @return boolean
     */
    public boolean isStopped() {
        // PK12235, more specific states
        return STATE_STOPPED == this.myState;
    }

    /**
     * Set the channel state flag to the input value.
     * 
     * @param state
     */
    private void setState(int state) {
        // PK12235, more specific states
        this.myState = state;
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#start()
     */
    public void start() {
        setState(STATE_RUNNING);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Start: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#stop(long)
     */
    public void stop(long millisec) {
        // PK12235, more specific states
        if (0 == millisec) {
            // immediate hard stop
            setState(STATE_STOPPED);
        } else {
            setState(STATE_STOPPING);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stop: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#init()
     */
    public void init() {
        // there are no extra steps beyond the constructor as there are
        // no channel objects that start or stop
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "init: " + this);
        }
    }

    /*
     * @see com.ibm.wsspi.channelfw.Channel#destroy()
     */
    public void destroy() {
        setState(STATE_STOPPED);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "destroy: " + this);
        }
        this.channelFactory.removeChannel(getName());
        this.channelFactory = null;
        this.myConfig = null;
        this.myObjectFactory = null;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.Discriminator#discriminate(com.ibm.wsspi.channelfw
     * .VirtualConnection, java.lang.Object)
     */
    public int discriminate(VirtualConnection vc, Object discrimData) {

        // PK12235, check for a full stop only
        if (isStopped()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel stopped, ignoring discriminate on: " + vc);
            }
            return Discriminator.NO;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Discriminating against " + vc + " with " + discrimData);
        }
        // we take the WsByteBuffer class but the TCP channel actually
        // gives us an array of those
        WsByteBuffer[] buffs = (WsByteBuffer[]) discrimData;

        // if there's no data to read, we can't continue now but we can't
        // rule it out that it might be ours
        if (null == buffs || 0 == buffs.length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No data provided, returning MAYBE");
            }
            return Discriminator.MAYBE;
        }
        HttpInboundLink link = (HttpInboundLink) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
        if (null == link) {
            // brand new connection
            link = new HttpInboundLink(this, vc);
        } else {
            link.init(vc, this);
        }
        // either way, pull the SC from the link object now
        HttpInboundServiceContextImpl mySvcCtxt = (HttpInboundServiceContextImpl) link.getChannelAccessor();
        boolean retVal = false;

        // check through each buffer provided. Stop when we know that
        // either this is ours, not ours, or we've run out of data

        for (int i = 0; i < buffs.length && null != buffs[i]; i++) {

            // pull off the next buffer to parse through
            WsByteBuffer buff = buffs[i];
            int limit = buff.limit();
            int position = buff.position();
            // have the SC reset this read buffer depending on how many times
            // we've been through discrimination with it.
            mySvcCtxt.enableBufferModification();
            mySvcCtxt.configurePostReadBuffer(buff);

            try {
                // parse the first line only to determine if this is
                // an HTTP message
                retVal = mySvcCtxt.getRequestImpl().parseLineDiscrim(buff);
            } catch (MalformedMessageException mme) {
                // no FFDC required
                // if we received this, then the section throwing the exception
                // will have logged in debug what was wrong, or what was not
                // HTTP about this request
                buff.limit(limit);
                buff.position(position);
                link.destroy(null);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Discriminate returning NO for non-HTTP msg");
                }
                return Discriminator.NO;
            } catch (Exception e) {
                // no FFDC required
                // any other exception indicates an error in the code when
                // trying to parse...
                buff.limit(limit);
                buff.position(position);
                link.destroy(null);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unexpected exception [" + e + "]");
                    Tr.debug(tc, "Discriminate returning NO");
                }
                return Discriminator.NO;
            }
            // save where we stopped (for MAYBE and YES both)
            mySvcCtxt.setOldLimit(buff.limit());
            if (retVal) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Discrimination returning YES");
                }
                // save this link object in the VC
                vc.getStateMap().put(CallbackIDs.CALLBACK_HTTPICL, link);
                return Discriminator.YES;
            }
            // need more data case, reset the buffer
            buff.limit(limit);
            buff.position(position);
        }

        // save this link object in the VC in case it does come back to us
        vc.getStateMap().put(CallbackIDs.CALLBACK_HTTPICL, link);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Returning MAYBE for " + vc);
        }
        return Discriminator.MAYBE;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.Discriminator#cleanUpState(com.ibm.wsspi.channelfw
     * .VirtualConnection)
     */
    public void cleanUpState(VirtualConnection vc) {
        // if discrimination returned MAYBE, then we stored a link object that
        // now needs to be cleaned up
        HttpInboundLink link = (HttpInboundLink) vc.getStateMap().get(CallbackIDs.CALLBACK_HTTPICL);
        if (null != link) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Performing cleanup after discrim MAYBE");
            }
            link.destroy(null);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.Channel#update(com.ibm.websphere.channelfw.ChannelData
     * )
     */
    public void update(ChannelData cc) {
        setHttpConfig(new HttpChannelConfig(cc));
        this.config = cc;
    }

    /*
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.config.getName() + " state=" + this.myState;
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
     * @see
     * com.ibm.wsspi.channelfw.InboundChannel#setDiscriminationProcess(com.ibm
     * .wsspi.channelfw.DiscriminationProcess)
     */
    public final void setDiscriminationProcess(DiscriminationProcess dp) {
        this.discriminationProcess = dp;
    }

}
