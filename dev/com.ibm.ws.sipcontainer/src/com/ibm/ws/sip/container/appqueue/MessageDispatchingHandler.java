/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.appqueue;

import com.ibm.ws.sip.container.util.Queueable;

/**
 * A MessageDispatchingHandler is a service that receives sip messages 
 * and dispatches them to the WebContainer. Different implementations 
 * might use different ways to distribute the messages between different
 * threads.   
 * 
 * @author Nitzan
 */
public interface MessageDispatchingHandler extends QueueLoadListener{
	
	/**
	 * Starts handler operation, allocating thread pool resources
	 */
	public abstract void start();
	
	/**
	 * Stops handler operation, releasing thread pool resources
	 */
	public abstract void stop();
	
	/**
	 * Dispatching a sip message to WebContainer
	 * returns false if queue reached capacity and message could not be added.
	 * @param msg
	 * @param blockTimeout time in MS for the calling thread to wait until dispatching is available 
	 * @return
	 */
	public abstract boolean dispatch(Queueable msg, long blockTimeout);
	
	/**
	 * Dispatching a sip message to WebContainer
	 * returns false if queue reached capacity and message could not be added.
	 * non-blocking 
	 */
	public abstract boolean dispatch(Queueable msg);
	
	/**
	 * This method will be called when container will want to print all related information
	 * of this MessageDispatchingHandler.
	 *
	 */
	public abstract void printState();
	
}

