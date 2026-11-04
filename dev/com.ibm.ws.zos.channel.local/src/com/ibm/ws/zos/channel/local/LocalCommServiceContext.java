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
package com.ibm.ws.zos.channel.local;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Interface between the local comm channel and the application channel.
 */
public interface LocalCommServiceContext {
	
	/**
	 * Perform a read.
	 * 
	 * If data is immediately available, then the given callback object is
	 * invoked immediately on this thread.  If no data is available, then
	 * the read goes async, and the callback object will be invoked on another 
	 * thread once the async read operation completes (i.e. once data becomes
	 * available).
	 * 
	 * @param callback The object to callback when the read completes.
	 * 
	 */
	public void read(LocalCommReadCompletedCallback callback) ;
	
	/**
	 * Force an asynchronous read.  The callback will be invoked on a separate
	 * thread regardless of whether or not data is immediately available.
	 * 
	 * @param callback The object to callback when the read completes.
	 */
	public void asyncRead(LocalCommReadCompletedCallback callback) ;
	
	/**
	 * Perform a synchronous read.  This call will block until data is available.
     *
	 * @return The read data.
	 */
	public ByteBuffer syncRead() throws IOException;
	
	/**
	 * Perform a synchronous write.
	 * 
	 * @param buffer The buffer to write.
	 */
	public void syncWrite(ByteBuffer buffer) throws IOException;

	/**
	 * Gets the client connection handle for the connection represented by this
	 * service context.  The client connection handle identifies this specific
	 * connection to the client, and is returned to the client on the connect call.
	 * 
	 * @return The client connection handle.
	 */
	public LocalCommClientConnHandle getClientConnectionHandle();

	/**
	 * Indicate to the local comm connection that it's safe to disconnect.  
	 * This method is called by upstream channels under their read-completed 
	 * callback after they've read off the data.
	 */
    public void releaseDisconnectLock();
	
    /**
     * Indicates the caller can dispatch work on this thread (the thread that this
     * method is called from).  Or put another way, you can start a long-running piece
     * of work on this thread.  Otherwise you'll need to queue to another thread pool.
     */
    public boolean isDispatchThread();
}
