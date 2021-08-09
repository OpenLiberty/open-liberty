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
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Helper implementation for Outbound Connector channel links.
 */
public abstract class OutboundConnectorLink implements OutboundConnectionLink {

    /**
     * TraceComponent
     */
    private static final TraceComponent tc = Tr.register(OutboundConnectorLink.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

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
        if (e != null) {
            // if this exception does not exist, it is assumed that this closed
            // without error.
            logClosedException(e);
        }
        destroy(e);
        conn.destroy();
    }

    /**
     * @see com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(Exception)
     */
    public void destroy(Exception e) {
        ConnectionReadyCallback appside = getApplicationCallback();
        if (appside != null) {
            appside.destroy(e);
        }
        destroy();
    }

    /**
     * This logs the error if the connection was closed by a higher level channel
     * with an error.
     * 
     * @param e
     */
    public void logClosedException(Exception e) {
        // Note: this may be a normal occurance so don't log error, just debug.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Connection closed with exception: " + e.getMessage());
        }
    }

}
