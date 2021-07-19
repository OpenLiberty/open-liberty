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

import java.nio.ByteBuffer;


/**
 * Implemented by upstream channels to receive control once an asynchronous
 * read against the local comm channel completes.
 * 
 * Note: the implementor of this interface should call 
 * LocalCommServiceContext.releaseDisconnectLock after the data has been
 * processed to indicate to the local comm channel that it is safe to
 * close the connection.
 * 
 */
public interface LocalCommReadCompletedCallback {
    
	/**
	 * Called when the read has completed normally.
	 * 
	 * @param context The service context used to read the data.
	 * @param ByteBuffer The data.
	 */
	public void ready(LocalCommServiceContext context, ByteBuffer data);
	
	/**
	 * Called when a problem occurred on the read.
	 * 
	 * @param context The service context used to read the data.
	 * @param e The exception that was generated.  If null, then the 
	 *        connection is being (or has been) closed normally.
	 */
	public void error(LocalCommServiceContext context, Exception e);
}
