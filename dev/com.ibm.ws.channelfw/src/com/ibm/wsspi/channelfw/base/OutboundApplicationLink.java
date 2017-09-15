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

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.OutboundConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Helper implementation for Outbound Application channel links.
 */
public abstract class OutboundApplicationLink implements OutboundConnectionLink {

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

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getVirtualConnection()
     */
    public VirtualConnection getVirtualConnection() {
        return this.vc;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ConnectionLink#setDeviceLink(com.ibm.wsspi.channelfw
     * .ConnectionLink)
     */
    public void setDeviceLink(ConnectionLink next) {
        this.linkOnDeviceSide = next;
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ConnectionLink#setApplicationCallback(com.ibm.wsspi
     * .channelfw.ConnectionReadyCallback)
     */
    public void setApplicationCallback(ConnectionReadyCallback next) {
        this.linkOnApplicationSide = next;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getDeviceLink()
     */
    public ConnectionLink getDeviceLink() {
        return this.linkOnDeviceSide;
    }

    /*
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

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#close(VirtualConnection,
     * Exception)
     */
    public void close(VirtualConnection conn, Exception e) {
        getDeviceLink().close(conn, e);
    }

    /*
     * @see com.ibm.wsspi.channelfw.ConnectionLink#getChannelAccessor()
     */
    public final Object getChannelAccessor() {
        throw new IllegalStateException("Not implemented and should not be used");
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundConnectionLink#connect(Object)
     */
    public void connect(Object address) throws Exception {
        ((OutboundConnectionLink) getDeviceLink()).connect(address);
        postConnectProcessing(getVirtualConnection());
    }

    /*
     * @see com.ibm.wsspi.channelfw.OutboundConnectionLink#connectAsynch(Object)
     */
    public void connectAsynch(Object address) {
        ((OutboundConnectionLink) getDeviceLink()).connectAsynch(address);
    }

    /**
     * Method called when the outbound connection has finished and this particular
     * connection
     * layer is next to take action.
     * 
     * @param conn
     */
    public void ready(VirtualConnection conn) {
        postConnectProcessing(conn);
        getApplicationCallback().ready(conn);
    }

    /**
     * Post connect processing.
     * 
     * @param conn
     */
    abstract protected void postConnectProcessing(VirtualConnection conn);

    /*
     * @see
     * com.ibm.wsspi.channelfw.ConnectionReadyCallback#destroy(java.lang.Exception
     * )
     */
    public void destroy(Exception e) {
        destroy();
    }
}
