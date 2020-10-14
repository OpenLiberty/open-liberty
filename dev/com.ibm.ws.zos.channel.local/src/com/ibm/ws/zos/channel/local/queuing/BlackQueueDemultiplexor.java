/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing;

import java.io.IOException;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.internal.CallbackAlreadyRegisteredException;
import com.ibm.ws.zos.channel.local.queuing.internal.ConnectionClosedException;


/**
 * Interface for registering and de-registering callbacks with the BlackQueueDemultiplexor.
 * 
 * The BlackQueueDemultiplexor receives native local comm work requests ("black queue" requests)
 * and dispatches them to the appropriately registered callback.
 * 
 */
public interface BlackQueueDemultiplexor {

    /**
     * Set the callback for handling new connections.
     * 
     * @param newConnectionCallback
     */
    public void registerNewConnectionCallback(BlackQueueReadyCallback newConnectionCallback);

    /**
     * Register a callback for the given requestType and connection handle.
     * 
     * Note: Once a callback is invoked, it is unregistered from the given requestType and
     * connection handle.  It must re-register if it wishes to handle further requests of the
     * same type and handle. (This rule does not apply to newConnectionCallback.  The 
     * newConnectionCallback is never removed from the registration list).
     * 
     * @param callback
     * @param requestType
     * @param clientConnHandle
     * 
     * @throws CallbackAlreadyRegisteredException, if another callback is already registered
     *         for the given requestType and lhdlPointer.
     * @throws ConnectionClosedException, if the registration failed because the connection 
     *         is closed or is closing.
     */
    public void registerCallback(BlackQueueReadyCallback callback, NativeWorkRequestType requestType, LocalCommClientConnHandle clientConnHandle) throws IOException;

    /**
     * Remove the given callback for the given requestType and connection handle.
     * 
     * Note: The callback is removed if and only if the given callback instance is exactly
     * the callback currently registered for the given requestType and connection handle.
     * 
     * @param callback
     * @param requestType
     * @param clientConnHandle
     * 
     * @returns true if the callback was removed; false otherwise (callback not in the map).
     */
    public boolean removeCallback(BlackQueueReadyCallback callback, NativeWorkRequestType requestType, LocalCommClientConnHandle clientConnHandle);
    
    /**
     * Cancel all callbacks associated with the given connection handle (lhdlPointer).
     * This method is called when a connection is closed.
     * 
     * Each callback's cancel() method is called.
     * 
     * @param clientConnHandle
     * @param e - The exception caused the cancellation.  If null, then the cancel is due
     *            to a normal close of the connection.
     */
    public void cancelCallbacks(LocalCommClientConnHandle clientConnHandle, Exception e);

    /**
     * Invoke all callbacks registered for requestType 'Disconnect'.  This effectively closes
     * all open connections.  This method is called during channel shutdown.
     */
    public void disconnectAll();

}