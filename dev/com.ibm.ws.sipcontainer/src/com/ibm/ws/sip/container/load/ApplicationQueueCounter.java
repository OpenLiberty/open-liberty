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
package com.ibm.ws.sip.container.load;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.pmi.PerfUtil;

/**
 * @author anat
 * 
 * Represents the higher queue value in the container queue. 
 * Container contains array of queues one for each application thread. 
 * Each time the number of messages in one of the queues changes - 
 * ApplicationQueueCounter will be updated with the parameter representing 
 * the queue that holds the most messages. 
 * ApplicationQueueCounter affects the weight of the container immediately for 
 * each change. When size of queue changed, setCounter() called, new weight 
 * calculated and LoadMgr is notified.
 */
public class ApplicationQueueCounter extends LoadCounterAbs {
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log
			.get(ApplicationQueueCounter.class);
	
	/**
	 * Holds the last value of max messages in msgQueue. 
	 * (one that decided that has maximum messages in the queue).
	 */
	private long _counter = 0;

	/**
	 * Reference to ConcurentLoadListener;
	 */
	private ConcurentLoadListener _lstr = null;
	
	/**
	 * Ctor
	 * @param maximumAllowed
	 * @param initialWeight
	 */
	public ApplicationQueueCounter(	int maximumAllowed, 
									int waterMarkSize, 
									int initialWeight,
									ConcurentLoadListener lstr) {
		super(maximumAllowed,waterMarkSize,initialWeight,PerfUtil.MSG_QUEUE_SIZE_INT);
		_lstr = lstr;
	}

	
	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#setCounter(long)
	 *  
	 *  When size of queue changed, setCounter() called, 
	 * 	new weight calculated and LoadMgr is notified.
	 */
	public void setCounter(long queueSize) {
		_counter = queueSize;
		if(calculateNewWeight(queueSize)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setCounter", 
						"Try to set new weight = " + getWeight());
			}
			_lstr.setNewWeight(this,queueSize);	
		}
	}
	
	/**
	 * @see com.ibm.ws.sip.container.load.LoadCounterAbs#getCurrentLoad()
	 */
	public long getCurrentLoad() {
		return _counter;
	}


	/**
	 *  @see com.ibm.ws.sip.container.load.LoadCounterAbs#reset()
	 */
	void reset() {
	 // Do Nothig. Timer should not affect this class.	
	}


	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#decrement()
	 */public void decrement() {
		// Not implemented here
	}


	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#increment()
	 */
	public void increment() {
		// Not implemented here		
	}


	/*
	 * @see com.ibm.ws.sip.container.load.Weighable#getCurrentState()
	 */
	public String getCurrentState() {
		StringBuffer buff = new StringBuffer();
		buff.append(_myInfo);
		buff.append(" Counter = ");
		buff.append(_counter);
		return buff.toString();
	}

}
