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
package com.ibm.wsspi.channelfw;

/**
 * This is the ConnectionLink specifically for Outbound (client side)
 * Channels. Since the outbound connections are initiated by the application
 * channel, this interface adds connect methods (one asynchronous and one not)
 * to make new connections.
 */
public interface OutboundConnectionLink extends ConnectionLink {

    /**
     * Connect to the provided address asynchronously. The ready
     * methods will be called when the connection is established or fails.
     * <p>
     * Failures will come via a destroy call on the ConnectionReadyCallback. In
     * this failure scenario, the virtual connection will not be reusable for a
     * new connect.
     * 
     * @param address
     *            The address to connect to.
     */
    void connectAsynch(Object address);

    /**
     * Connect to the provided address synchronously. If a failure occurs,
     * an exception will be thrown. In this failure scenario, the virtual
     * connection will not be reusable for a new connect.
     * 
     * @param address
     *            The address to connect to.
     * @exception Exception
     *                This exception thrown if connect fails. Often on network
     *                channel implementations this will be an IOException.
     */
    void connect(Object address) throws Exception;
}
