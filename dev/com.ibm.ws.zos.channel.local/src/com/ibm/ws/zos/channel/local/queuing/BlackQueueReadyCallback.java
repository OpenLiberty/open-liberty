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


/**
 * This interface is implemented by classes which handle local comm native 
 * "black queue" work requests.
 * 
 * The work handlers implement this interface and register themselves with 
 * BlackQueueDemultiplexor.  BlackQueueDemultiplexor receives black queue 
 * requests and dispatches them to the appropriate handler by invoking the
 * handler's "ready" method, as specified by this interface.
 *
 */
public interface BlackQueueReadyCallback {
    
    /**
     * Called when a black queue work request is ready to be processed.
     * 
     * The provider of this interface must have previously registered with
     * the BlackQueueDemultiplexor to handle requests of the given type and 
     * connection handle, which are encapsulated in the NativeWorkRequest object.
     * 
     * @param nativeWorkRequest - The black queue work request to be processed.
     * 
     */
    public void blackQueueReady(NativeWorkRequest nativeWorkRequest);

    /**
     * Called when a callback is cancelled, usually because the connection has
     * been closed.
     * 
     * @param e - The exception that caused the cancellation.  If null, then the 
     *            cancellation is due to a normal close of the connection.
     */
    public void cancel(Exception e);
}