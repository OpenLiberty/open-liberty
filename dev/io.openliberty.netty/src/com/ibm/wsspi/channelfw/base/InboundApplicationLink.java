/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw.base;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Helper implementation for Inbound Application channel links.
 */
public abstract class InboundApplicationLink implements ConnectionLink {

    /**
     * trace component for this factory
     */
    private static final TraceComponent tc = Tr.register(InboundApplicationLink.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Link below this one on the chain.
     */
    private ConnectionLink linkOnDeviceSide = null;

    /**
     * Connection to the channel above this one on the chain.
     */
    private ConnectionReadyCallback linkOnApplicationSide = null;

    /**
     * Virtual connection associated with this connection.
     */
    protected VirtualConnection vc = null;

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getVirtualConnection()
     */
    public VirtualConnection getVirtualConnection() {
        return this.vc;
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#setDeviceLink(com.ibm.wsspi.channelfw.ConnectionLink)
     */
    public void setDeviceLink(ConnectionLink next) {
        this.linkOnDeviceSide = next;
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#setApplicationCallback(com.ibm.wsspi.channelfw.ConnectionReadyCallback)
     */
    public void setApplicationCallback(ConnectionReadyCallback next) {
        this.linkOnApplicationSide = next;
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getDeviceLink()
     */
    public ConnectionLink getDeviceLink() {
        return this.linkOnDeviceSide;
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getApplicationCallback()
     */
    public ConnectionReadyCallback getApplicationCallback() {
        return this.linkOnApplicationSide;
    }

    /**
     * Destroy resources held by this object.
     */
    protected void destroy() {
        this.vc = null;
        this.linkOnApplicationSide = null;
        this.linkOnDeviceSide = null;
    }

    /**
     * Initialize this link.
     * 
     * @param connection
     */
    public void init(VirtualConnection connection) {
        this.vc = connection;
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#close(VirtualConnection, Exception)
     */
    public void close(VirtualConnection conn, Exception e) {
        ConnectionLink deviceLink = getDeviceLink();
        // Protect from a redundant call to close.
        if (deviceLink != null) {
            deviceLink.close(conn, e);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Questionable/redundant call to close, vc=" + conn.hashCode());
            }
        }
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getChannelAccessor()
     */
    public final Object getChannelAccessor() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

}
