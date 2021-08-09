/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.interrupt;

/**
 * Monitor the request begin/end for this thread.
 * 
 * Note that this interface is somewhat arbitrary and is the product of removing
 * the requestInterrupt-1.0 feature (and it's associated request probe) and folding
 * it into requestTiming-1.0.
 */
public interface InterruptibleRequestLifecycle {
	/**
	 * Prepare the current thread for a new request.
	 * 
	 * @param requestId A string representing the request that is starting.
	 */
	public void newRequestEntry(String requestId);

	/**
	 * Clean up the current thread's ODI stack after the request is finished.
	 * 
 	 * @param requestId A string representing the request that is ending.
	 */
	public void completedRequestExit(String requestId);
	
	/**
	 * Notify implementer that a request is hung.
	 * 
	 * @parm requestId : Request id of the request that is hung.
	 * @parm threadId  : Thread id that the request is running on.
	 */
	void hungRequestDetected(String requestId, long threadId);

}
