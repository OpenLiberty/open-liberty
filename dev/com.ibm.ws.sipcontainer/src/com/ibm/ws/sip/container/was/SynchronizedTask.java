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

import com.ibm.ws.sip.container.util.Queueable;

/**
 * A wrapper for Queueable task that will execute it within a service synchronizer lock 
 * 
 * @author Nitzan Nissim
 */
public class SynchronizedTask implements Runnable{
	/**
	 * Task to run
	 */
	private Queueable _task;
	/**
	 * A service (i.e. application code) synchronizing object
	 */
	private Object _synchronizer;
	
	/**
	 * Ctor
	 * @param task
	 * @param synchronizer
	 */
	public SynchronizedTask(Queueable task, Object synchronizer){
		_task = task;
		_synchronizer = synchronizer;
	}
	
	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		if (_synchronizer != null){
			synchronized (_synchronizer) {
				_task.run();
			}
		}
		else{
			_task.run();
		}
	}

}
