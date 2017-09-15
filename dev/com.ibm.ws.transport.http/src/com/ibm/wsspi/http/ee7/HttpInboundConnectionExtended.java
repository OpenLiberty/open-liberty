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
package com.ibm.wsspi.http.ee7;

import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 *
 */
public interface HttpInboundConnectionExtended extends HttpInboundConnection {

    // todo add javadoc to these three methods when the code has been finalized.  This is in the SPI, so would be better to minimize additions here

    /**
     * This API will return the TCPConnectionContext.
     * 
     * @return
     */
    TCPConnectionContext getTCPConnectionContext();

    /**
     * This API will return the VirtualConnection.
     * 
     * @return
     */
    VirtualConnection getVC();

    /**
     * This API will return the device link.
     * 
     * @return
     */
    ConnectionLink getHttpInboundDeviceLink();

    /**
     * This API will return the application link.
     * 
     * @return
     */
    ConnectionLink getHttpInboundLink();

    /**
     * This API will return the HttpDispatcherLink.
     * 
     * @return
     */
    ConnectionLink getHttpDispatcherLink();
}
