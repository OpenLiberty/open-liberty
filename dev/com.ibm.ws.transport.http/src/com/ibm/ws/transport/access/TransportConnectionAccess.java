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
package com.ibm.ws.transport.access;

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

//import java.io.InputStream;
//import java.io.OutputStream;

/**
 *
 */
public interface TransportConnectionAccess {

    public TCPConnectionContext getTCPConnectionContext();

    public void setTCPConnectionContext(TCPConnectionContext x);

    public ConnectionLink getDeviceConnLink();

    public void setDeviceConnLink(ConnectionLink x);

    public VirtualConnection getVirtualConnection();

    public void setVirtualConnection(VirtualConnection x);

    public void close() throws Exception;

}
