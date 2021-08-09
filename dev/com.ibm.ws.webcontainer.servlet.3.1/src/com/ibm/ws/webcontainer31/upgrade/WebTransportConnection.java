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
package com.ibm.ws.webcontainer31.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 *
 */
public class WebTransportConnection implements TransportConnectionAccess {


    private final static TraceComponent tc = Tr.register(WebTransportConnection.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private TCPConnectionContext tcpConn; 
    private VirtualConnection    virtualConn;
    private ConnectionLink deviceConnLink;
    private HttpUpgradeHandlerWrapper handler;

    public WebTransportConnection(HttpUpgradeHandlerWrapper upgradeHandler) {
        this.handler = upgradeHandler;
    }


    public TCPConnectionContext getTCPConnectionContext() {

        return tcpConn;
    }

    public void setTCPConnectionContext(TCPConnectionContext input) {

        tcpConn = input;
    }

    public ConnectionLink getDeviceConnLink() {

        return deviceConnLink;
    }

    public void setDeviceConnLink(ConnectionLink input) {

        deviceConnLink = input;
    }


    public VirtualConnection getVirtualConnection() {

        return virtualConn;
    }

    public void setVirtualConnection(VirtualConnection input) {

        virtualConn = input;
    }

    public void close() throws Exception {
        if (handler!=null) {
            handler.destroy();
        }
    }

}
