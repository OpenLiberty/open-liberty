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
package com.ibm.ws.sip.container.events;

import com.ibm.ws.sip.container.util.Queueable;

/**
 * This interface can define different task invocation methods
 * basically single-threaded or multi-threaded.
 * @author Nitzan
 */
public interface TasksInvoker {
	/**
	 * Invoking a queueable operation
	 * @param task
	 * @param blockTimeout is set, the calling thread will be blocked 
	 * until task is can be executed or this number of milliseconds have passed.
	 * If set to 0, there is no timeout for blocking. 
	 * @return
	 */
	public boolean invokeTask(Queueable task, long blockTimeout);
	
	/**
	 * Invoking a queueable operation, non-blocking
	 * @param task
	 * @return
	 */
	public boolean invokeTask(Queueable task);
}
