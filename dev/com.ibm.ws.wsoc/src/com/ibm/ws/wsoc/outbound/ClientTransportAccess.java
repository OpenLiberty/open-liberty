package com.ibm.ws.wsoc.outbound;

/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 *
 */
public class ClientTransportAccess implements TransportConnectionAccess {

    private static final TraceComponent tc = Tr.register(ClientTransportAccess.class);

    private TCPConnectionContext tcpConn;
    private VirtualConnection virtualConn;
    private ConnectionLink deviceConnLink;

    public ClientTransportAccess() {

    }

    @Override
    public TCPConnectionContext getTCPConnectionContext() {

        return tcpConn;
    }

    @Override
    public void setTCPConnectionContext(TCPConnectionContext input) {

        tcpConn = input;
    }

    @Override
    public ConnectionLink getDeviceConnLink() {

        return deviceConnLink;
    }

    @Override
    public void setDeviceConnLink(ConnectionLink input) {

        deviceConnLink = input;
    }

    @Override
    public VirtualConnection getVirtualConnection() {

        return virtualConn;
    }

    @Override
    public void setVirtualConnection(VirtualConnection input) {

        virtualConn = input;
    }

    @Override
    public void close() throws Exception {

    }

}
