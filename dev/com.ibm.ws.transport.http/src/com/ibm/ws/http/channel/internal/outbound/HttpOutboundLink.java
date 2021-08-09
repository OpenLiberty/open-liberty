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

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpObjectFactory;
import com.ibm.wsspi.channelfw.OutboundProtocol;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.base.OutboundProtocolLink;
import com.ibm.wsspi.http.channel.outbound.HttpAddress;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * Implementation of the outbound Http channel connection links. This handles
 * a channel wanting to send an Http request out and receive a response back.
 */
public class HttpOutboundLink extends OutboundProtocolLink implements OutboundProtocol {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(HttpOutboundLink.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Service context associated with this object */
    private HttpOutboundServiceContextImpl myInterface = null;
    /** Channel owning this object */
    private HttpOutboundChannel myChannel = null;
    /** Flag on whether a reconnect is allowed at this point */
    private boolean bAllowReconnect = true;
    /** Flag on whether the reconnect is completely blocked for this conn */
    private boolean bEnableReconnect = true;
    /** Keep track of the target address on the connect */
    private HttpAddress targetAddress = null;
    /** Flag on whether this link object is active or not */
    private boolean bIsActive = false;
    /** flag to tell if we are reconnecting */
    private boolean reconnecting = false;
    /** Original IOException for asynch reconnects */
    private IOException reconnectException = null;
    /** destroyed early by a reconnect */
    private boolean earlyReconnectDestroy = false;

    /**
     * Constructor for an HTTP outbound link object.
     * 
     * @param c
     * @param vc
     */
    public HttpOutboundLink(HttpOutboundChannel c, VirtualConnection vc) {
        init(vc, c);
    }

    /**
     * Initialize this object.
     * 
     * @param inVC
     * @param channel
     */
    public void init(VirtualConnection inVC, HttpOutboundChannel channel) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Init on link: " + this + " " + inVC);
        }
        super.init(inVC);
        this.myChannel = channel;
        this.bIsActive = true;
        setEnableReconnect(this.myChannel.getHttpConfig().allowsRetries());
        setAllowReconnect(true);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.OutboundProtocolLink#destroy(java.lang.Exception
     * )
     */
    public void destroy(Exception e) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Destroying this outbound link: " + this + " " + getVirtualConnection());
        }
        // if this object is not active, then just return out
        if (!this.reconnecting) {
            synchronized (this) {
                if (!this.bIsActive) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Ignoring destroy on an inactive object");
                    }
                    return;
                }
                this.bIsActive = false;
            }
            super.destroy(e);
            if (null != this.myInterface) {
                this.myInterface.destroy();
                this.myInterface = null;
            }
            // 342859 - disconnect from these objects too
            this.targetAddress = null;
            this.myChannel = null;
        } else {
            // this connection is now toast...need to tell that to the
            // channel above.
            this.earlyReconnectDestroy = true;
            IOException ioe = this.reconnectException;
            this.reconnectException = null;
            this.reconnecting = false;
            this.myInterface.callErrorCallback(getVirtualConnection(), ioe);
        }
    }

    /**
     * Get access to the outbound service context for this connection. If it
     * does not exist yet, then create it.
     * 
     * @return HttpOutboundServiceContextImpl
     */
    private HttpOutboundServiceContextImpl getInterface(VirtualConnection inVC) {

        if (null == this.myInterface) {
            this.myInterface = new HttpOutboundServiceContextImpl((TCPConnectionContext) getDeviceLink().getChannelAccessor(), this, inVC, this.myChannel.getHttpConfig());
        }
        return this.myInterface;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getChannelAccessor()
     */
    public Object getChannelAccessor() {
        return getInterface(getVirtualConnection());
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundProtocol#getProtocol()
     */
    public String getProtocol() {
        // 337176 - return the protocol of this connection
        return "HTTP";
    }

    /**
     * Any work required after connecting to the target.
     * 
     * @param inVC
     */
    protected void postConnectProcessing(VirtualConnection inVC) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Finished connecting to target: " + this + " " + inVC);
        }
        getInterface(inVC);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.OutboundProtocolLink#ready(com.ibm.wsspi.channelfw
     * .VirtualConnection)
     */
    public void ready(VirtualConnection inVC) {
        if (!this.reconnecting) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Outbound ready for " + this + " " + inVC);
            }
            super.ready(inVC);
        } else {
            this.reconnecting = false;
            this.reconnectException = null;
            // on the reconnect, inform the outbound SC to re-send the request
            // message that failed.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Outbound reconnect finished for " + this + " " + inVC);
            }
            this.myInterface.nowReconnectedAsync();
        }
    }

    /**
     * Clear any local variables when this object needs to be reset.
     * 
     */
    protected void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Clearing the outbound link: " + this + " " + getVirtualConnection());
        }
        // reset the retry flag based on the config
        setEnableReconnect(this.myChannel.getHttpConfig().allowsRetries());
        setAllowReconnect(true);
        this.reconnecting = false;
        this.reconnectException = null;
        this.earlyReconnectDestroy = false;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.OutboundProtocolLink#close(com.ibm.wsspi.channelfw
     * .VirtualConnection, java.lang.Exception)
     */
    public void close(VirtualConnection inVC, Exception e) {

        // when the channel above calls close() on the outbound connection then
        // we are done... no need to purge anything off the socket like we do
        // on the inbound side (purge request body if they never read it)

        // pass the close down the chain.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Closing this outbound link: " + this + " " + getVirtualConnection());
        }
        setAllowReconnect(false);
        if (!this.earlyReconnectDestroy) {
            super.close(inVC, e);
        } else {
            destroy(e);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.OutboundProtocolLink#connect(java.lang.Object)
     */
    public void connect(Object address) throws Exception {

        this.targetAddress = (HttpAddress) address;
        // if we're connecting on an existing SC, then clear it if need be
        // we do not want to clear when the reconnect flag is false because
        // that means we're in the middle of a reconnect (we called this method
        // and not an external user)
        if (null != this.myInterface && isReconnectAllowed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Clearing existing OSC on connect");
            }
            this.myInterface.clear();
        }
        super.connect(address);
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.base.OutboundProtocolLink#connectAsynch(java.lang
     * .Object)
     */
    public void connectAsynch(Object address) {

        this.targetAddress = (HttpAddress) address;
        // if we're connecting on an existing SC, then clear it if need be
        // we do not want to clear when the reconnect flag is false because
        // that means we're in the middle of a reconnect (we called this method
        // and not an external user)
        if (null != this.myInterface && isReconnectAllowed()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Clearing existing OSC on connectAsynch");
            }
            this.myInterface.clear();
        }
        super.connectAsynch(address);
    }

    /**
     * Attempt to reconnect synchronously to the target server. If an error
     * occurs, then simply throw the original exception that caused this
     * recovery path.
     * 
     * @param originalExcep
     * @throws IOException
     */
    protected void reConnectSync(IOException originalExcep) throws IOException {

        setAllowReconnect(false);
        try {
            connect(getTargetAddress());
        } catch (Exception e) {
            // no FFDC required
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Sync reconnect failed, throwing original exception");
            }
            throw originalExcep;
        }
    }

    /**
     * Attempt to reconnect asynchronously to the target server. If an error
     * occurs, then simply throw the original exception that caused this
     * recovery path.
     * 
     * @param originalExcep
     */
    protected void reConnectAsync(IOException originalExcep) {
        setAllowReconnect(false);
        this.reconnecting = true;
        this.reconnectException = originalExcep;
        connectAsynch(getTargetAddress());
    }

    /**
     * Query whether a reconnect would be allowed. Reconnects can happen once
     * on an write attempt for a request message.
     * 
     * @return boolean
     */
    protected boolean isReconnectAllowed() {
        return this.bAllowReconnect;
    }

    /**
     * Query whether or not reconnects are enabled for this connection.
     * 
     * @return boolean
     */
    private boolean isReconnectEnabled() {
        return this.bEnableReconnect;
    }

    /**
     * Set the controlling flag on whether reconnects are allowed in general.
     * 
     * @param flag
     */
    private void setEnableReconnect(boolean flag) {
        this.bEnableReconnect = flag;
    }

    /**
     * Set the flag on whether to allow another reconnect or not to the input
     * boolean value.
     * 
     * @param flag
     */
    protected void setAllowReconnect(boolean flag) {
        // check both the input flag and the "enabled" option
        this.bAllowReconnect = (flag & isReconnectEnabled());
    }

    /**
     * Application channel wants to disallow reconnect/rewrites of data and
     * have errors always thrown up the chain immediately.
     * 
     * @return boolean
     */
    protected boolean disallowRewrites() {
        setEnableReconnect(false);
        setAllowReconnect(false);
        // it worked if this is now false, so return the reverse value
        return !isReconnectAllowed();
    }

    /**
     * Method used when the service context has been instructed to re-enable
     * the outbound reconnect/rewrites of data during error conditions.
     * 
     * @return boolean
     */
    final protected boolean allowRewrites() {
        // don't allow them to override the config if it is disabled
        // setEnableReconnect(true);
        setAllowReconnect(true);
        return isReconnectAllowed();
    }

    /**
     * Query the target address of this outbound connection.
     * 
     * @return HttpAddress
     */
    public HttpAddress getTargetAddress() {
        return this.targetAddress;
    }

    /**
     * Query the HTTP object factory.
     * 
     * @return HttpObjectFactory
     */
    public HttpObjectFactory getObjectFactory() {
        return (null == this.myChannel) ? null : this.myChannel.getObjectFactory();
    }

    /**
     * Query whether this outbound link is still actively connected to the
     * target host. If this returns true, then the link is expected to be
     * still valid for continued use. If this returns false, then the
     * connection is inactive and the caller must close down the connection
     * and not send/read anymore data, as any attempts to do so will result
     * in errors.
     * 
     * @return boolean
     */
    public boolean isConnected() {

        // if we haven't fully read the incoming message then this
        // connection should still be valid
        if (!this.myInterface.isIncomingMessageFullyRead()) {
            return true;
        }

        try {
            // if the immediate TCP read works then this connection is
            // still connected
            if (null == this.myInterface.getTSC().getReadInterface().getBuffer()) {
                // no read buffer currently exists, use JITAllocate
                this.myInterface.getTSC().getReadInterface().setJITAllocateSize(this.myInterface.getHttpConfig().getIncomingHdrBufferSize());
            }
            this.myInterface.getTSC().getReadInterface().read(0, 0);
            return true;
        } catch (IOException e) {
            // No FFDC required

            // if an exception happens, then this connection is dead
            return false;
        }
    }

}
