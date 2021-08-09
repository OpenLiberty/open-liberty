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
package com.ibm.ws.sip.container.was;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.appqueue.MessageDispatcher;
import com.ibm.ws.sip.container.events.TasksInvoker;
import com.ibm.ws.sip.container.util.Queueable;

/**
 * This timer invoker will use the WAS ThreadPool to execute the timer 
 * tasks in a multi-threaded manner.
 * 
 * @author Nitzan
 */
public class WASXTasksInvoker implements TasksInvoker {
	 /**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(WASXTasksInvoker.class);
	
	/**
	 * Invoking the timer task using a thread pool for task distribution 
	 * @see com.ibm.ws.sip.container.events.TasksInvoker#invokeTask(com.ibm.ws.sip.container.util.Queueable, long)
	 */
	public boolean invokeTask(Queueable task, long blockTimeout) {
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("invokeTask: task = "+task);
		}
			
		return MessageDispatcher.getMessageDispatchingHandler().dispatch(task, blockTimeout);
		
	}
	
	/**
	 * Invoking the timer task using a thread pool for task distribution, non-blocking 
	 * @see com.ibm.ws.sip.container.events.TasksInvoker#invokeTask(com.ibm.ws.sip.container.util.Queueable)
	 */
	public boolean invokeTask(Queueable task) {
		return invokeTask(task, -1);
	}
}
