/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.udpchannel;

import java.net.SocketAddress;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A context object encapsulating all of the data related to a UDPChannel
 * write data request. This context can be obtained via the getWriteInterface
 * method on the UDPContext.
 */
public interface UDPWriteRequestContext {

    /**
     * Set the write buffer for the next write call.
     * 
     * @param buf
     */
    void setBuffer(WsByteBuffer buf);

    /**
     * Performs write on the connection. If the write can be done immediately then the VirtualConnection
     * object is passed back, after the data has been written. If the data can not be written immediately,
     * then null is returned, the request is queued, and when the data has been written the UDP Channel
     * will call the registered UDPWriteCompletedCallback.
     * The callback may or may not be invoked, if invoked it will be invoked on a different thread.
     * If the data is written immediately, then the VirtualConnection that is returned will be the
     * same VirtualConnection that applied to this UDPWriteRequestContext before the write was
     * called, it is returned as a convienence for the calling code to invoke the callback complete
     * method in the same way the UDP Channel would have invoked it.
     * 
     * @param address - address to send the packet too
     * @param callback - an implementation of the UDPWriteCompletedCallback class
     * @param forceQueue - force request to be queued and callback called from another thread
     * @return VirtualConnection - if all bytes were written immediately, null if the write
     *         has gone asynchronous and the provided callback will be used later
     */
    VirtualConnection write(SocketAddress address, UDPWriteCompletedCallback callback, boolean forceQueue);

}
