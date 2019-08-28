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
package com.ibm.ws.sip.container.events;

import com.ibm.ws.sip.container.util.Queueable;

/**
 * A basic events invoker. Using a single thread invocation
 * 
 * @author Nitzan
 */
public class BasicTasksInvoker implements TasksInvoker{
	/**
     * Invoking the task in a straightforward manner (no queue involved)
     * @see com.ibm.ws.sip.container.was.MessageDispatchingHandler#invokeTimerTask(javax.servlet.sip.TimerListener, javax.servlet.sip.ServletTimer)
     */
	public boolean invokeTask(Queueable task) {
		task.run();
		return true;
	}
	
	/**
	 * Not supported
	 * @see com.ibm.ws.sip.container.events.TasksInvoker#invokeTask(com.ibm.ws.sip.container.util.Queueable, long)
	 */
	public boolean invokeTask(Queueable task, long blockTimeout){
		throw new RuntimeException("Operation not supported");
	}
}
