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

import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A context object encapsulating all of the data related to a UDPChannel
 * read data request. This context can be obtained via the getReadInterface
 * method on the UDPContext.
 */
public interface UDPReadRequestContext {

    /**
     * Performs reads on the connection. If the read can be done immediately then the VirtualConnection
     * object is passed back, and the data is ready for use by the application. if the data can not be
     * read immediately, then null is returned, the request will be queued, and data will be available
     * when the UDP Channel calls the registered UDPReadCompletedCallback.
     * The callback may or may not be invoked, if invoked it will be invoked on a different thread.
     * If the data is read immediately, then the VirtualConnection that is returned will be the
     * same VirtualConnection that applied to this UDPReadRequestContext before the read was
     * called, it is returned as a convenience for the calling code to invoke the callback complete
     * method in the same way the UDP Channel would have invoked it.
     * 
     * @param callback - an implementation of the UDPReadCompletedCallback class
     * @param forceQueue - force request to be queued and callback called from another thread
     * @return VirtualConnection - if all bytes were read immediately, null if the read
     *         has gone asynchronous and the provided callback will be used later
     */
    VirtualConnection read(UDPReadCompletedCallback callback, boolean forceQueue);

    /**
     * Request that reads continually run in the background without direct requests
     * by the channel above. Each read completion will use the provided callback.
     * This can be turned on or off based on the provided flag.
     * 
     * @param callback - an implementation of the UDPReadCompletedCallback class
     * @param enable - turns the reading on/off.
     */
    void readAlways(UDPReadCompletedCallbackThreaded callback, boolean enable);

    /**
     * Returns the read UDPBuffer associated with this request.
     * 
     * @return UDPBuffer
     */
    UDPBuffer getUDPBuffer();

}
