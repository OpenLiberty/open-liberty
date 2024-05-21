/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;

import io.netty.channel.Channel;

/**
 *
 */
public class NettyConnectionLink implements ConnectionLink {

    Channel nettyChannel;

    public NettyConnectionLink(Channel nettyChannel) {
        // TODO Auto-generated constructor stub
        this.nettyChannel = nettyChannel;
    }

    @Override
    public void ready(VirtualConnection vc) {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy(Exception e) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object getChannelAccessor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VirtualConnection getVirtualConnection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setApplicationCallback(ConnectionReadyCallback next) {
        // TODO Auto-generated method stub

    }

    @Override
    public ConnectionLink getDeviceLink() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ConnectionReadyCallback getApplicationCallback() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close(VirtualConnection vc, Exception e) {
        // TODO Auto-generated method stub
        System.out.println("Closing Netty connection context: " + nettyChannel);
        nettyChannel.close();
    }

    @Override
    public void setDeviceLink(ConnectionLink next) {
        // TODO Auto-generated method stub

    }

}
