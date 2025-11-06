/*******************************************************************************
 * Copyright (c) 2014, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
 * This package is not SPI even though it is in wsspi.
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

    /**
     * This API will return the remote protocol as defined by a X-Forwarded-Proto or Forwarded:proto header
     *
     * @return
     */
    String getRemoteProto();

    /**
     * This API will return whether the channel is configured to use X-Forwarded-* or Forwarded headers
     *
     * @return
     */
    boolean useRemoteIpOptions();

    /**
     * Since Servlet 6.0 : support jakarta.servlet.ServletRequest#getProtocolRequestId()
     *
     * @return (int) Stream ID; -1 otherwise
     *
     */
    public int getStreamId();

    /**
     * Since Servlet 6.0 : support jakarta.servlet.ServletConnection#getConnectionId()
     *
     * @return (int) connection dispatcher link for the servlet request
     *
     */
    public int getConnectionId();
}
