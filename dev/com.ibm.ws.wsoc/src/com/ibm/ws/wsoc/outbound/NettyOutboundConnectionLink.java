/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.outbound;

import io.netty.channel.Channel;

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;


/**
 * An extension of ConnectionLink to wrap Netty channel operations necessary for
 * Websocket outbound logic. Specifically the only method needed is to close the
 * connections whenever a close is triggered.
 */
public class NettyOutboundConnectionLink implements ConnectionLink {

    private Channel connection;

    protected NettyOutboundConnectionLink(Channel connection) {
        this.connection = connection;
    }

    @Override
    public void ready(VirtualConnection vc) {
        // Nothing to do here
    }

    @Override
    public void destroy(Exception e) {
        // Nothing to do here
    }

    @Override
    public void setDeviceLink(ConnectionLink next) {
        // Nothing to do here
    }

    @Override
    public void setApplicationCallback(ConnectionReadyCallback next) {
        // Nothing to do here
    }

    @Override
    public VirtualConnection getVirtualConnection() {
        // Nothing to return here
        return null;
    }

    @Override
    public ConnectionLink getDeviceLink() {
        // Nothing to return here
        return null;
    }

    @Override
    public Object getChannelAccessor() {
        // Nothing to return here
        return null;
    }

    @Override
    public ConnectionReadyCallback getApplicationCallback() {
        // Nothing to return here
        return null;
    }

    @Override
    public void close(VirtualConnection vc, Exception e) {
        connection.close();
    }


}
