/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api;

import java.net.InetSocketAddress;

import com.ibm.wsspi.http.channel.outbound.HttpAddress;

/**
 * Address class for outbound test connections.
 */
public class TargetAddress implements HttpAddress {

    /** Target address */
    private InetSocketAddress addr = null;

    /**
     * Create a TargetAddress with the input information
     * 
     * @param name
     * @param port
     */
    public TargetAddress(String name, int port) {
        this.addr = new InetSocketAddress(name, port);
    }

    /**
     * Hostname to pass into the Host header of the request.
     * 
     * @return String
     */
    public String getHostname() {
        return getRemoteAddress().getHostName();
    }

    /**
     * Query whether the target in the address is a forward proxy. If this
     * is true, then the request message will send out the full URL (scheme
     * plus hostname plus URI, etc), otherwise the request will only send out
     * the URI ([GET /index.html HTTP/1.1] for example).
     * 
     * @return boolean
     */
    public boolean isForwardProxy() {
        return false;
    }

    /**
     * Load address to bind this socket to. Can return null in which case
     * the operating system dependent behaviour of binding to the next free
     * local address is assumed.
     * 
     * @return InetSocketAddress
     */
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    /**
     * Remote address to bind socket to. This must return a valid value.
     * 
     * @return InetSocketAddress
     */
    public InetSocketAddress getRemoteAddress() {
        return this.addr;
    }

    public int getConnectTimeout() {
        return 10000;
    }

}
