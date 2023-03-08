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
package com.ibm.ws.zos.channel.local.queuing.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueDemultiplexor;
import com.ibm.ws.zos.channel.local.queuing.BlackQueueReadyCallback;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequest;
import com.ibm.ws.zos.channel.local.queuing.NativeWorkRequestType;

/**
 * Routes local comm native "black queue" (aka work queue) requests to the appropriate
 * callback object.
 * 
 * There is one thread that listens natively for black queue requests. When a request
 * arrives, the listener thread tosses the request up to java, wraps the work request
 * with a java object (NativeWorkRequest), and then queues the work to another task via
 * the ExecutorService.
 * 
 * This other task then calls the BlackQueueDemultiplexor to find the appropriate callback
 * object for the given work request type and connection handle.
 */
public class BlackQueueDemultiplexorImpl implements BlackQueueDemultiplexor {

    /**
     * There's only one callback for new connections. 
     * All other callbacks (except FFDC) are connection-handle-specific.
     */
    private BlackQueueReadyCallback newConnectionCallback;
    
    /**
     * There's only one FFDC callback.  The FFDC callback handles FFDC work
     * request types, for logging an FFDC for a failure in the native localcomm
     * code.
     */
    private BlackQueueReadyCallback ffdcCallback;

    /**
     * Map of callbacks, registered based on request type and native connection handle (lhdlPointer).
     */
    private ConcurrentMap<CallbackKey, BlackQueueReadyCallback> registeredCallbacks = new ConcurrentHashMap<CallbackKey, BlackQueueReadyCallback>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerNewConnectionCallback(BlackQueueReadyCallback newConnectionCallback) {
        this.newConnectionCallback = newConnectionCallback;
    }
    
    /**
     * Register a callback for handling FFDC work.
     */
    protected void registerFfdcCallback(BlackQueueReadyCallback ffdcCallback) {
        this.ffdcCallback = ffdcCallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerCallback(BlackQueueReadyCallback callback,
                                 NativeWorkRequestType requestType,
                                 LocalCommClientConnHandle clientConnHandle) throws IOException {

        CallbackKey key = new CallbackKey(requestType, clientConnHandle);

        BlackQueueReadyCallback currentCallback = registeredCallbacks.putIfAbsent(key, callback);

        if (currentCallback != null && currentCallback != callback) {
            // This means the key was already associated with another callback.  
            // Overwriting callbacks is not allowed. Throw an exception.
            throw new CallbackAlreadyRegisteredException(requestType, clientConnHandle, currentCallback, callback);

        } else if (!registeredCallbacks.containsKey(new CallbackKey(NativeWorkRequestType.REQUESTTYPE_DISCONNECT, clientConnHandle))) {
            // There's no DISCONNECT callback for this connection handle. The DISCONNECT callback
            // is registered as soon as the connection is established.  If it's not in the map, it must
            // have been removed and invoked - which means the connection is closing. In that case, 
            // remove the callback we just added and throw an exception.
            registeredCallbacks.remove(key, callback);

            throw new ConnectionClosedException(requestType, clientConnHandle, callback);
        }
    }

    /**
     * Lookup the callback registered for the given NativeWorkRequest type and connection handle
     * and invoke it.
     * 
     * Note: Once a callback is invoked, it is unregistered from the given requestType and
     * connection handle. It must re-register if it wishes to handle further requests of the
     * same type and handle. (This rule does not apply to newConnectionCallback. The
     * newConnectionCallback is never removed from the registration list).
     * 
     * @param nativeWorkRequest
     * 
     * @throws CallbackNotFoundException, if no callback is registered for this NativeWorkRequest type/handle.
     */
    public void dispatch(NativeWorkRequest nativeWorkRequest) throws CallbackNotFoundException {

        if (nativeWorkRequest.getRequestType() == NativeWorkRequestType.REQUESTTYPE_CONNECT && newConnectionCallback != null) {
            newConnectionCallback.blackQueueReady(nativeWorkRequest);
            
        } else if (nativeWorkRequest.getRequestType() == NativeWorkRequestType.REQUESTTYPE_FFDC && ffdcCallback != null) {
            ffdcCallback.blackQueueReady(nativeWorkRequest);
            
        } else {
            // Removes the callback from the map.
            CallbackKey key = new CallbackKey(nativeWorkRequest.getRequestType(), nativeWorkRequest.getClientConnectionHandle());
            BlackQueueReadyCallback callback = registeredCallbacks.remove(key);
            if (callback != null) {
                callback.blackQueueReady(nativeWorkRequest);
            } else {
                throw new CallbackNotFoundException(nativeWorkRequest);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeCallback(BlackQueueReadyCallback callback, NativeWorkRequestType requestType, LocalCommClientConnHandle clientConnHandle) {
        // Note: this call removes the callback if and only it matches the value in the map 
        return registeredCallbacks.remove(new CallbackKey(requestType, clientConnHandle), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelCallbacks(LocalCommClientConnHandle clientConnHandle, Exception e) {
        for (NativeWorkRequestType requestType : NativeWorkRequestType.class.getEnumConstants()) {
            BlackQueueReadyCallback callback = registeredCallbacks.remove(new CallbackKey(requestType, clientConnHandle));
            if (callback != null) {
                callback.cancel(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnectAll() {
        // Invoke all 'disconnect' callbacks.
        for (CallbackKey callbackKey : registeredCallbacks.keySet()) {
            if (callbackKey.getRequestType() == NativeWorkRequestType.REQUESTTYPE_DISCONNECT) {
                BlackQueueReadyCallback callback = registeredCallbacks.remove(callbackKey);
                if (callback != null) {
                    callback.blackQueueReady(null);
                }
            }
        }
    }

    /**
     * Write the key/value pairs of registered callbacks to the output stream.
     */
    public Set<LocalCommClientConnHandle> introspect(PrintWriter out) throws IOException {
        StringBuilder sbuilder = new StringBuilder();

        Set<LocalCommClientConnHandle> connHandlePointers = new HashSet<LocalCommClientConnHandle>();

        // Write registered callback info
        out.println();
        sbuilder.append("Registered Callbacks");
        out.println();
        for (CallbackKey key : registeredCallbacks.keySet()) {
            out.println();
            out.print(key.toString());
            out.print("\tCallback: ");
            out.print(registeredCallbacks.get(key));
            out.println();

            connHandlePointers.add(key.getClientConnectionHandle());
        }

        return connHandlePointers;

    }

}

/**
 * Composite HashMap key encapsulating:
 * 
 * 1) black queue work requestType, and
 * 2) native connection handle.
 * 
 */
@Trivial
class CallbackKey {
    private final NativeWorkRequestType requestType;
    private final LocalCommClientConnHandle clientConnHandle;

    public CallbackKey(NativeWorkRequestType requestType, LocalCommClientConnHandle clientConnHandle) {
        this.requestType = requestType;
        this.clientConnHandle = clientConnHandle;
    }

    public NativeWorkRequestType getRequestType() {
        return requestType;
    }

    @Override
    public int hashCode() {
        return requestType.getNativeValue() + clientConnHandle.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CallbackKey) {
            CallbackKey that = (CallbackKey) o;
            return (this.requestType == that.requestType && this.clientConnHandle.equals(that.clientConnHandle));
        }
        return false;
    }

    public LocalCommClientConnHandle getClientConnectionHandle() {
        return clientConnHandle;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\tRequest Type: " + this.requestType.toString() + "\n");
        sb.append("\tConnection Handle: " + clientConnHandle + "\n");

        return sb.toString();
    }
}
