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
package com.ibm.wsspi.tcpchannel;

import java.net.InetAddress;

/**
 * A context object encapsulating data related to a TCPChannel.
 * 
 * @ibm-spi
 */
public interface TCPConnectionContext {
    /**
     * Constant key used in the Virtual Connection statemap to indicate that SSL
     * tunneling is on if the property key exists in the statemap, its value is
     * implied to betrue if the property key does not exist its value is implied
     * to be false
     */
    String FORWARD_PROXY_CONNECT = "FORWARD_PROXY_CONNECT";

    /**
     * Get the Read Object for this interface
     * 
     * @return TCPReadRequestContext
     */
    TCPReadRequestContext getReadInterface();

    /**
     * Get the Write Object for this interface
     * 
     * @return TCPWriteRequestContext
     */
    TCPWriteRequestContext getWriteInterface();

    /**
     * Returns the remote address (on the other end of the socket) Under some
     * circumstances (for example a failed outbound connection attempt) the
     * value returned is the address <em>attempted</em> to have been connected
     * to.
     * 
     * @return InetAddress
     */
    InetAddress getRemoteAddress();

    /**
     * Returns the remote port (on the other end of the socket)
     * 
     * @return int
     */
    int getRemotePort();

    /**
     * Returns the local host address of the socket
     * 
     * @return InetAddress
     */
    InetAddress getLocalAddress();

    /**
     * Returns the local port number of the socket
     * 
     * @return int
     */
    int getLocalPort();

    /**
     * Get the SSL context of the connection
     * 
     * @return SSL connection context interface or null if no SSL is in use.
     */
    SSLConnectionContext getSSLContext();
}
