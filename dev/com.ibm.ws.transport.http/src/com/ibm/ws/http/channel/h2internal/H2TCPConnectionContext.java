/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.net.InetAddress;

import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 *
 */
public class H2TCPConnectionContext implements TCPConnectionContext {

    H2TCPReadRequestContext h2ReadRequestContext = null;
    H2TCPWriteRequestContext h2WriteRequestContext = null;
    Integer streamID = null;
    H2InboundLink muxLink = null;
    VirtualConnection vc = null;

    public H2TCPConnectionContext(Integer id, H2InboundLink link, VirtualConnection v) {
        streamID = id;
        muxLink = link;
        vc = v;
        h2ReadRequestContext = new H2TCPReadRequestContext(streamID, link, this);
        h2WriteRequestContext = new H2TCPWriteRequestContext(streamID, link, this);
    }

    public VirtualConnection getVC() {
        return vc;
    }

    @Override
    public TCPReadRequestContext getReadInterface() {
        return h2ReadRequestContext;
    }

    @Override
    public TCPWriteRequestContext getWriteInterface() {
        return h2WriteRequestContext;
    }

    @Override
    public InetAddress getRemoteAddress() {
        return null;
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public InetAddress getLocalAddress() {
        return null;
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public SSLConnectionContext getSSLContext() {
        return null;
    }

}
