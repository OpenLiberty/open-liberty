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
package com.ibm.ws.sip.container.osgi;

import java.io.Serializable;

import javax.servlet.sip.SipApplicationSession;

import com.ibm.websphere.sip.AsynchronousWork;
import com.ibm.websphere.sip.AsynchronousWorkListener;

/**
 * The <code>AsynchronousWorkDispatcher</code> provides an interface for dispatching asynchronous messages.
 * <p> 
 * The SIP container implements this interface to dispatch a routed task.
 * 
 * @author Galina Rubinshtein, Dec 2008
 * 
 */
public interface AsynchronousWorkDispatcher extends Serializable {
	
	/**
	 * A method to dispatch asynchronous messages
	 * @param obj AsynchronousWork
	 * @param listener AsynchronousWorkListener
	 */
	public void dispatch(AsynchronousWork obj, AsynchronousWorkListener listener);
	
	/**
	 * A method to retrieve SipApplicationSession object by AsynchronousWork
	 * 
	 * @return SipApplicationSession
	 */
	public SipApplicationSession getSipApplicationSession();
	
	/**
	 * wait until the response is received, this method will cause the current thread to be locked
	 * until the async work task is finished
	 * 
	 * @param time - the maximum time to wait in milliseconds, 0 is for waiting forever 
	 * @return Object - the result of the async task or null if it was not finished yet
	 */
	public Object waitForResponse(long time);
}
