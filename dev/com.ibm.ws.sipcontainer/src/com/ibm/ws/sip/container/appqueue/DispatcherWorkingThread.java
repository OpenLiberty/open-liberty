/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.appqueue;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;

/**
 * Class that represents a Thread that will dispatch the incoming reqeust to the
 * application. This Thread contains the queue of the waiting requests.
 * 
 * @author Anat Fradin, Aug 1, 2006
 * 
 */
public class DispatcherWorkingThread extends AppQueueHandler implements Runnable  {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(DispatcherWorkingThread.class);
	    
	/**
	 * Local thread.
	 */
	private Thread _thread;
	
	/**
	 * local thread name.
	 */
	private static final String THREAD_NAME = "WASX_Msg_Thread_";
	
	/**
	 * Used to create an unique name for eath thread that represents by
	 * this object
	 */
	private static int threadIndex = 1;
	
	/**
	 * Ctor
	 * 
	 * @param waitingQueueSize
	 */
	public DispatcherWorkingThread(int waitingQueueSize, int id, QueueLoadListener lstr) {
		super(waitingQueueSize,id,lstr);
		
		String threadName = THREAD_NAME + String.valueOf(threadIndex++);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "DispatcherWorkingThread", 
					"initiating thread for pool = " + threadName);
		}
		init(threadName);
	}
	
	protected void init(String threadName)
	{
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "DispatcherWorkingThread#init", 
					"creating/starting java thread to run this class = " + threadName);
		}
		_thread = new Thread(this, threadName);
		_thread.start();
	}

	/**
	 * In this method when the thread is invoked it will take the first
	 * parameter from the _waitingRequestsQueue and execute it.
	 */
	public void run() {
		while (true) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "run",
						this+" looping to see if queue has filled with new elements.");
			}
			try{
				extractAmsgAndExecute();
			}catch( Throwable e){
				if (c_logger.isErrorEnabled()) {
					c_logger.error("error.exception", null, null, e);
				}
			}
		} // while (m_keepRunning)
	}

	protected void extractAmsgAndExecute() {
		try {
			//store the queue id on the current thread
			ThreadLocalStorage.setQueueId(getId());
			
			Runnable msg = getRunnableObj();

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "executeTask","msg = " + msg);
			}
			reportToFailoverServiceStart();
			msg.run();			
		}catch( Throwable e){
			if (c_logger.isErrorEnabled()) {
				c_logger.error("error.exception", null, null, e);
			}
		}
		finally{
			//remove the queue id from the current thread
			ThreadLocalStorage.setQueueId(null);
			invalidateWhenReadyTU();
			finishToExecuteRunnable();
		}
	}

	/**
	 * Method which creates current statistic info about this DispatcherWorkingThread.
	 * @return
	 */
	public String getInfo(){
		StringBuffer buff = new StringBuffer();
		buff.append("Name = <");
		buff.append(_thread.getName());
		buff.append(super.getInfo());
		return buff.toString();
	}
}
